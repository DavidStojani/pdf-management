package org.papercloud.de.pdfservice.search;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    private final DocumentRepository documentRepository;
    private final PageRepository pageRepository;
    private final PdfTextExtractorService pdfTextExtractorService;
    private final DocumentMapper documentMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public DocumentDTO processDocument(DocumentUploadDTO documentUploadDTO, String username) throws IOException {
        byte[] pdfBytes = extractBytesFromInputStream(documentUploadDTO.getInputStream());

        UserEntity user = userRepository.findByUsername(username).get();

        DocumentPdfEntity documentEntity = saveDocument(user, documentUploadDTO, pdfBytes);

        extractAndSavePages(documentEntity, pdfBytes);

        return documentMapper.toDocumentDTO(documentEntity);
    }

    @Override
    public DocumentDTO getDocument(Long id) {
        return null;
    }

    @Override
    public byte[] getDocumentContent(Long id) {
        return new byte[0];
    }

    @Override
    public DocumentDownloadDTO downloadDocument(String username, Long id) {
        DocumentPdfEntity documentPdfEntity = documentRepository.findById(id).orElse(null);

        if (!documentPdfEntity.getOwner().getUsername().equals(username)) {
            try {
                throw new AccessDeniedException("You are not allowed to access this document.");
            } catch (AccessDeniedException e) {
                throw new RuntimeException(e);
            }
        }

        return documentMapper.toDownloadDTO(documentPdfEntity);
    }

    private byte[] extractBytesFromInputStream(InputStream inputStream) throws IOException {
        return StreamUtils.copyToByteArray(inputStream);
    }

    private DocumentPdfEntity saveDocument(UserEntity user, DocumentUploadDTO file, byte[] pdfBytes) {

        DocumentPdfEntity documentEntity = DocumentPdfEntity.builder()
                .title(extractTitle(file))
                .filename(file.getFileName())
                .contentType(file.getContentType())
                .pdfContent(pdfBytes)
                .size(file.getSize())
                .owner(user)
                .uploadedAt(LocalDateTime.now())
                .build();

        return documentRepository.save(documentEntity);
    }

    private List<PagesPdfEntity> extractAndSavePages(DocumentPdfEntity document, byte[] pdfBytes)
            throws IOException {
        List<String> textByPage = pdfTextExtractorService.extractTextFromPdf(
                new ByteArrayInputStream(pdfBytes));
        List<PagesPdfEntity> pagesPdfEntityList = new ArrayList<>();

        for (int i = 0; i < textByPage.size(); i++) {
            PagesPdfEntity pagesPdfEntity = PagesPdfEntity.builder()
                    .document(document)
                    .pageNumber(i + 1)
                    .pageText(textByPage.get(i)).build();

            pagesPdfEntityList.add(pagesPdfEntity);
        }
        return pageRepository.saveAll(pagesPdfEntityList);
    }

    private String extractTitle(DocumentUploadDTO file) {
        // TODO: Extract metadata title if available
        return file.getFileName();
    }
}

