package org.papercloud.de.pdfservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.papercloud.de.core.domain.AuditActionType;
import org.papercloud.de.core.dto.document.DocumentListItemDTO;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserDocumentFavouriteEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.FavouriteRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.papercloud.de.pdfservice.mapper.DocumentServiceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentFavouriteServiceImpl implements DocumentFavouriteService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final FavouriteRepository favouriteRepository;
    private final AuditService auditService;
    private final DocumentServiceMapper documentMapper;

    @Override
    @Transactional(readOnly = true)
    public List<DocumentListItemDTO> getFavourites(String username) {
        List<UserDocumentFavouriteEntity> favourites = favouriteRepository.findByUserUsernameWithDocument(username);
        return favourites.stream()
                .map(fav -> documentMapper.toListItemDTO(fav.getDocument(), true))
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

    private UserEntity findUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    private DocumentPdfEntity getDocumentOrThrow(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + id));
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
