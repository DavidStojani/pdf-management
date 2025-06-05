package org.papercloud.de.pdfservice.search;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
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
import org.papercloud.de.pdfservice.processor.AsyncEnrichmentProcessor;
import org.papercloud.de.pdfservice.textutils.PdfTextExtractorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final PageRepository pageRepository;
    private final PdfTextExtractorService pdfTextExtractorService;
    private final DocumentMapper documentMapper;
    private final AsyncEnrichmentProcessor asyncEnrichmentProcessor;
    @Override
    public DocumentDTO processDocument(DocumentUploadDTO uploadDTO, String username) throws IOException {
        UserEntity user = findUserOrThrow(username);
        DocumentPdfEntity document = createDocument(user, uploadDTO);
        List<String> pageTexts = pdfTextExtractorService.extractTextFromPdf(uploadDTO.getInputPdfBytes());
        DocumentDTO documentDTO = persistDocument(document, pageTexts);

        asyncEnrichmentProcessor.enrichAndPersist(documentDTO.getId(), pageTexts);

        return documentDTO;
    }

    @Transactional
    public DocumentDTO persistDocument(DocumentPdfEntity doc, List<String> pages) {
        DocumentPdfEntity saved = documentRepository.save(doc);
        savePages(saved, pages);

        return documentMapper.toDocumentDTO(saved);
    }

    @Override
    public DocumentDownloadDTO downloadDocument(String username, Long id) throws AccessDeniedException {
        DocumentPdfEntity document = getDocumentOrThrow(id);

        if (!document.getOwner().getUsername().equals(username)) {
            throw new AccessDeniedException("You are not allowed to access this document.");
        }

        return documentMapper.toDownloadDTO(document);
    }

    // 🔽 --- Private Helper Methods --- 🔽

    private UserEntity findUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    private DocumentPdfEntity createDocument(UserEntity user, DocumentUploadDTO uploadDTO) {
        DocumentPdfEntity document = DocumentPdfEntity.builder()
                .filename(uploadDTO.getFileName())
                .contentType(uploadDTO.getContentType())
                .pdfContent(uploadDTO.getInputPdfBytes())
                .size(uploadDTO.getSize())
                .owner(user)
                .uploadedAt(LocalDateTime.now())
                .build();

        return document;
    }

    private void savePages(DocumentPdfEntity document, List<String> pageTexts) {
        List<PagesPdfEntity> pages = IntStream.range(0, pageTexts.size())
                .mapToObj(i -> PagesPdfEntity.builder()
                        .document(document)
                        .pageNumber(i + 1)
                        .pageText(pageTexts.get(i))
                        .build())
                .toList();

        pageRepository.saveAll(pages);
    }

    private DocumentPdfEntity getDocumentOrThrow(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Document not found with id: " + id));
    }

}
