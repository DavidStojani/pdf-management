package org.papercloud.de.pdfservice.search;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.common.dto.document.DocumentDTO;
import org.papercloud.de.common.dto.document.DocumentDownloadDTO;
import org.papercloud.de.common.dto.document.DocumentMapper;
import org.papercloud.de.common.dto.document.DocumentUploadDTO;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.PageRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.papercloud.de.pdfservice.utils.PdfTextExtractorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final PageRepository pageRepository;
    private final PdfTextExtractorService pdfTextExtractorService;
    private final DocumentMapper documentMapper;

    @Override
    @Transactional
    public DocumentDTO processDocument(DocumentUploadDTO uploadDTO, String username) throws IOException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        DocumentPdfEntity documentEntity = saveDocumentEntity(user, uploadDTO, uploadDTO.getInputPdfBytes());
        extractAndSavePages(documentEntity, uploadDTO.getInputPdfBytes());

        return documentMapper.toDocumentDTO(documentEntity);
    }

    @Override
    public DocumentDTO getDocument(Long id) {
        // TODO: implement
        return null;
    }

    @Override
    public byte[] getDocumentContent(Long id) {
        // TODO: implement
        return new byte[0];
    }

    @Override
    public DocumentDownloadDTO downloadDocument(String username, Long id) throws AccessDeniedException {
        DocumentPdfEntity document = documentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Document not found with id: " + id));

        if (!document.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("You are not allowed to access this document.");
        }

        return documentMapper.toDownloadDTO(document);
    }

    private byte[] extractBytes(InputStream inputStream) throws IOException {
        return StreamUtils.copyToByteArray(inputStream);
    }

    private DocumentPdfEntity saveDocumentEntity(UserEntity user, DocumentUploadDTO uploadDTO, byte[] pdfBytes) {
        DocumentPdfEntity document = DocumentPdfEntity.builder()
                .title(extractTitle(uploadDTO))
                .filename(uploadDTO.getFileName())
                .contentType(uploadDTO.getContentType())
                .pdfContent(pdfBytes)
                .size(uploadDTO.getSize())
                .owner(user)
                .uploadedAt(LocalDateTime.now())
                .build();

        return documentRepository.save(document);
    }

    private void extractAndSavePages(DocumentPdfEntity document, byte[] pdfBytes) throws IOException {
        List<String> pageTexts = pdfTextExtractorService.extractTextFromPdf(pdfBytes);

        List<PagesPdfEntity> pages = IntStream.range(0, pageTexts.size())
                .mapToObj(i -> PagesPdfEntity.builder()
                        .document(document)
                        .pageNumber(i + 1)
                        .pageText(pageTexts.get(i))
                        .build())
                .collect(Collectors.toList());

        pageRepository.saveAll(pages);
    }

    private String extractTitle(DocumentUploadDTO uploadDTO) {
        // TODO: Optionally extract title from metadata
        return uploadDTO.getFileName();
    }
}


