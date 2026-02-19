package org.papercloud.de.pdfservice.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.domain.AuditActionType;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.core.domain.UploadSource;
import org.papercloud.de.core.dto.document.DocumentDTO;
import org.papercloud.de.core.dto.document.DocumentDownloadDTO;
import org.papercloud.de.core.dto.document.DocumentUploadDTO;
import org.papercloud.de.core.dto.document.DocumentListItemDTO;
import org.papercloud.de.core.dto.search.SearchRequestDTO;
import org.papercloud.de.core.dto.search.SearchResultDTO;
import org.papercloud.de.core.events.OcrEvent;
import org.papercloud.de.core.ports.outbound.SearchService;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.entity.UserDocumentFavouriteEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.FavouriteRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.papercloud.de.pdfservice.errors.DocumentUploadException;
import org.papercloud.de.pdfservice.errors.InvalidDocumentException;
import org.papercloud.de.pdfservice.errors.UserAuthenticationException;
import org.papercloud.de.pdfservice.mapper.DocumentServiceMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final FavouriteRepository favouriteRepository;
    private final DocumentServiceMapper documentMapper;
    private final ApplicationEventPublisher publisher;
    private final SearchService searchService;
    private final AuditService auditService;
    private final HttpServletRequest httpServletRequest;

    @Override
    public DocumentDTO processUpload(MultipartFile file, Authentication authentication, UploadSource uploadSource) {
        Authentication resolvedAuth = resolveAuthentication(authentication);
        validateAuthentication(resolvedAuth);
        validateFile(file);
        validatePdfContentType(file);

        DocumentUploadDTO uploadDTO = buildUploadDto(file, uploadSource);
        return processDocumentSafely(uploadDTO, resolvedAuth.getName());
    }

    @Override
    public DocumentDTO processDocument(DocumentUploadDTO uploadDTO, String username) throws IOException {
        DocumentDTO documentDTO = saveDocToDB(username, uploadDTO);
        publisher.publishEvent(new OcrEvent(documentDTO.getId()));
        return documentDTO;
    }


    @Transactional
    protected DocumentDTO saveDocToDB(String username, DocumentUploadDTO uploadDTO) {
        UserEntity user = findUserOrThrow(username);

        DocumentPdfEntity documentPdfEntity = DocumentPdfEntity.builder()
                .filename(uploadDTO.getFileName())
                .contentType(uploadDTO.getContentType())
                .pdfContent(uploadDTO.getInputPdfBytes())
                .size(uploadDTO.getSize())
                .owner(user)
                .uploadedAt(LocalDateTime.now())
                .status(Document.Status.UPLOADED)
                .build();

        documentRepository.save(documentPdfEntity);

        String source = uploadDTO.getUploadSource() != null
                ? uploadDTO.getUploadSource().name()
                : UploadSource.FILE_UPLOAD.name();
        recordAuditSafely(documentPdfEntity.getId(), username, AuditActionType.UPLOADED,
                getClientIp(), getClientUserAgent(), "UPLOAD_SOURCE:" + source);

        return documentMapper.toDocumentDTO(documentPdfEntity);
    }

    @Override
    public DocumentDownloadDTO downloadDocument(String username, Long id) throws AccessDeniedException {
        DocumentPdfEntity document = getDocumentOrThrow(id);

        if (!document.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("You are not allowed to access this document.");
        }

        recordAuditSafely(id, username, AuditActionType.DOWNLOADED,
                getClientIp(), getClientUserAgent(), null);

        return documentMapper.toDownloadDTO(document);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentListItemDTO> searchDocuments(String username, String query) {
        Set<Long> favouriteIds = favouriteRepository.findFavouriteDocumentIdsByUsername(username);

        if (query == null || query.isBlank()) {
            List<DocumentPdfEntity> documents = documentRepository.findByOwnerUsername(username);
            return mapToListItems(documents, favouriteIds);
        }

        try {
            return searchViaElasticsearch(username, query, favouriteIds);
        } catch (Exception e) {
            log.warn("Elasticsearch search failed, falling back to in-memory filtering", e);
            return searchInMemory(username, query, favouriteIds);
        }
    }

    private List<DocumentListItemDTO> searchViaElasticsearch(String username, String query, Set<Long> favouriteIds) {
        SearchRequestDTO request = SearchRequestDTO.builder()
                .query(query)
                .username(username)
                .page(0)
                .size(50)
                .build();

        SearchResultDTO result = searchService.search(request);

        List<Long> documentIds = result.getHits().stream()
                .map(hit -> Long.parseLong(hit.getDocumentId()))
                .toList();

        if (documentIds.isEmpty()) {
            return List.of();
        }

        Map<Long, DocumentPdfEntity> documentMap = documentRepository.findAllById(documentIds).stream()
                .collect(Collectors.toMap(DocumentPdfEntity::getId, doc -> doc));

        return documentIds.stream()
                .map(documentMap::get)
                .filter(doc -> doc != null)
                .map(doc -> DocumentListItemDTO.builder()
                        .id(doc.getId())
                        .title(getDisplayTitle(doc))
                        .pageCount(getPageCount(doc))
                        .isFavourite(favouriteIds.contains(doc.getId()))
                        .build())
                .toList();
    }

    private List<DocumentListItemDTO> searchInMemory(String username, String query, Set<Long> favouriteIds) {
        List<DocumentPdfEntity> documents = documentRepository.findByOwnerUsername(username);
        String q = query.toLowerCase(Locale.ROOT);
        List<DocumentPdfEntity> filtered = documents.stream()
                .filter(doc -> {
                    String filename = doc.getFilename();
                    String displayTitle = getDisplayTitle(doc);
                    return displayTitle.toLowerCase(Locale.ROOT).contains(q)
                            || (filename != null && filename.toLowerCase(Locale.ROOT).contains(q));
                })
                .toList();
        return mapToListItems(filtered, favouriteIds);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentListItemDTO> getFavourites(String username) {
        List<UserDocumentFavouriteEntity> favourites = favouriteRepository.findByUserUsernameWithDocument(username);
        return favourites.stream()
                .map(fav -> {
                    DocumentPdfEntity doc = fav.getDocument();
                    return DocumentListItemDTO.builder()
                            .id(doc.getId())
                            .title(getDisplayTitle(doc))
                            .pageCount(getPageCount(doc))
                            .isFavourite(true)
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public void addFavourite(Long documentId, String username) {
        UserEntity user = findUserOrThrow(username);
        DocumentPdfEntity document = getDocumentOrThrow(documentId);
        if (!favouriteRepository.existsByUserIdAndDocumentId(user.getId(), documentId)) {
            UserDocumentFavouriteEntity favourite = UserDocumentFavouriteEntity.builder()
                    .user(user)
                    .document(document)
                    .build();
            favouriteRepository.save(favourite);
        }
        recordAuditSafely(documentId, username, AuditActionType.FAVOURITE_ADDED, null, null, null);
    }

    @Override
    @Transactional
    public void removeFavourite(Long documentId, String username) {
        UserEntity user = findUserOrThrow(username);
        favouriteRepository.deleteByUserIdAndDocumentId(user.getId(), documentId);
        recordAuditSafely(documentId, username, AuditActionType.FAVOURITE_REMOVED, null, null, null);
    }


    // 🔽 --- Private Helper Methods --- 🔽

    private DocumentUploadDTO buildUploadDto(MultipartFile file, UploadSource uploadSource) {
        try {
            return DocumentUploadDTO.builder()
                    .fileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .inputPdfBytes(file.getBytes())
                    .uploadSource(uploadSource)
                    .build();
        } catch (IOException e) {
            throw new DocumentUploadException("Failed to read uploaded file.", e);
        }
    }

    private DocumentDTO processDocumentSafely(DocumentUploadDTO uploadDTO, String username) {
        try {
            return processDocument(uploadDTO, username);
        } catch (IOException e) {
            throw new DocumentUploadException("Failed to process uploaded document.", e);
        }
    }

    private UserEntity findUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    private DocumentPdfEntity getDocumentOrThrow(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + id));
    }

    private List<DocumentListItemDTO> mapToListItems(List<DocumentPdfEntity> documents, Set<Long> favouriteIds) {
        return documents.stream()
                .map(doc -> DocumentListItemDTO.builder()
                        .id(doc.getId())
                        .title(getDisplayTitle(doc))
                        .pageCount(getPageCount(doc))
                        .isFavourite(favouriteIds.contains(doc.getId()))
                        .build())
                .toList();
    }

    private Authentication resolveAuthentication(Authentication authentication) {
        if (authentication != null) {
            return authentication;
        }
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private void validateAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserAuthenticationException("User must be authenticated to upload documents.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentException("Uploaded file must not be empty.");
        }
    }

    private void validatePdfContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (!MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(contentType)) {
            throw new InvalidDocumentException("Invalid file format. Only PDF files are allowed.");
        }
    }

    private String getDisplayTitle(DocumentPdfEntity doc) {
        return doc.getTitle() != null ? doc.getTitle() : "UPLOAD_#" + doc.getId();
    }

    private int getPageCount(DocumentPdfEntity doc) {
        return doc.getPages() != null ? doc.getPages().size() : 0;
    }

    private String getClientIp() {
        try {
            String forwarded = httpServletRequest.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return httpServletRequest.getRemoteAddr();
        } catch (Exception e) {
            log.debug("Could not extract client IP", e);
            return null;
        }
    }

    private String getClientUserAgent() {
        try {
            return httpServletRequest.getHeader("User-Agent");
        } catch (Exception e) {
            log.debug("Could not extract User-Agent", e);
            return null;
        }
    }

    private void recordAuditSafely(Long documentId, String username, AuditActionType action,
                                   String ipAddress, String userAgent, String additionalInfo) {
        try {
            auditService.recordAction(documentId, username, action, ipAddress, userAgent, additionalInfo);
        } catch (Exception e) {
            log.warn("Failed to record audit event {} for document {}: {}", action, documentId, e.getMessage());
        }
    }
}
