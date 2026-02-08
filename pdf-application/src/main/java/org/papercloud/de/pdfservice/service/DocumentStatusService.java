package org.papercloud.de.pdfservice.service;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DocumentStatusService {

    private final DocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public Document.Status getStatus(Long documentId) {
        return getDocument(documentId).getStatus();
    }

    @Transactional
    public void updateStatus(Long documentId, Document.Status newStatus) {
        DocumentPdfEntity document = getDocument(documentId);
        document.setStatus(newStatus);
        documentRepository.save(document);
    }

    @Transactional
    public boolean updateStatusIfCurrent(Long documentId, Document.Status expected, Document.Status next) {
        DocumentPdfEntity document = getDocument(documentId);
        if (document.getStatus() != expected) {
            return false;
        }
        document.setStatus(next);
        documentRepository.save(document);
        return true;
    }

    private DocumentPdfEntity getDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId));
    }
}
