package org.papercloud.de.pdfservice.search;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.common.dto.document.DocumentDTO;
import org.papercloud.de.common.dto.document.DocumentDownloadDTO;
import org.papercloud.de.common.dto.document.DocumentMapper;
import org.papercloud.de.common.dto.document.DocumentUploadDTO;
import org.papercloud.de.common.events.OcrEvent;
import org.papercloud.de.common.util.PdfTextExtractorService;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.PageRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final PageRepository pageRepository;
    private final PdfTextExtractorService pdfTextExtractorService;
    private final DocumentMapper documentMapper;
    private final ApplicationEventPublisher publisher;

    @Override
    public DocumentDTO processDocument(DocumentUploadDTO uploadDTO, String username) throws IOException {
        DocumentDTO documentDTO = saveDocToDB(username, uploadDTO);
        publisher.publishEvent(new OcrEvent(documentDTO.getId(),documentDTO.getPdfContent()));
        return documentDTO;
    }


    @Transactional
    private DocumentDTO saveDocToDB(String username, DocumentUploadDTO uploadDTO) {
        UserEntity user = findUserOrThrow(username);

        DocumentPdfEntity documentPdfEntity = DocumentPdfEntity.builder()
                .filename(uploadDTO.getFileName())
                .contentType(uploadDTO.getContentType())
                .pdfContent(uploadDTO.getInputPdfBytes())
                .size(uploadDTO.getSize())
                .owner(user)
                .uploadedAt(LocalDateTime.now())
                .build();

        documentRepository.save(documentPdfEntity);
        return documentMapper.toDocumentDTO(documentPdfEntity);

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

    private DocumentPdfEntity getDocumentOrThrow(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Document not found with id: " + id
                        )
                );
    }

}
