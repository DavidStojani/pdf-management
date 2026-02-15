package org.papercloud.de.pdfservice.service;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DocumentStatusService {

    private final DocumentRepository documentRepository;

    @Value("${app.recovery.retry.backoff.base-minutes:15}")
    private long retryBackoffBaseMinutes;

    @Value("${app.recovery.retry.backoff.max-minutes:360}")
    private long retryBackoffMaxMinutes;

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

    @Transactional
    public void markOcrFailure(Long documentId, String reason) {
        DocumentPdfEntity document = getDocument(documentId);
        int nextRetryCount = document.getOcrRetryCount() + 1;

        document.setStatus(Document.Status.OCR_ERROR);
        document.setOcrRetryCount(nextRetryCount);
        document.setOcrNextRetryAt(calculateNextRetryAt(nextRetryCount));
        document.setOcrLastError(sanitizeError(reason));
        documentRepository.save(document);
    }

    @Transactional
    public void markEnrichmentFailure(Long documentId, String reason) {
        DocumentPdfEntity document = getDocument(documentId);
        int nextRetryCount = document.getEnrichmentRetryCount() + 1;

        document.setStatus(Document.Status.ENRICHMENT_ERROR);
        document.setFailedEnrichment(true);
        document.setEnrichmentRetryCount(nextRetryCount);
        document.setEnrichmentNextRetryAt(calculateNextRetryAt(nextRetryCount));
        document.setEnrichmentLastError(sanitizeError(reason));
        documentRepository.save(document);
    }

    @Transactional
    public void resetOcrRetry(Long documentId) {
        DocumentPdfEntity document = getDocument(documentId);
        document.setOcrRetryCount(0);
        document.setOcrNextRetryAt(null);
        document.setOcrLastError(null);
        documentRepository.save(document);
    }

    @Transactional
    public void resetEnrichmentRetry(Long documentId) {
        DocumentPdfEntity document = getDocument(documentId);
        document.setFailedEnrichment(false);
        document.setEnrichmentRetryCount(0);
        document.setEnrichmentNextRetryAt(null);
        document.setEnrichmentLastError(null);
        documentRepository.save(document);
    }

    @Transactional
    public void markIndexingFailure(Long documentId, String reason) {
        DocumentPdfEntity document = getDocument(documentId);
        int nextRetryCount = document.getIndexingRetryCount() + 1;

        document.setStatus(Document.Status.INDEXING_ERROR);
        document.setIndexingRetryCount(nextRetryCount);
        document.setIndexingNextRetryAt(calculateNextRetryAt(nextRetryCount));
        document.setIndexingLastError(sanitizeError(reason));
        documentRepository.save(document);
    }

    @Transactional
    public void resetIndexingRetry(Long documentId) {
        DocumentPdfEntity document = getDocument(documentId);
        document.setIndexingRetryCount(0);
        document.setIndexingNextRetryAt(null);
        document.setIndexingLastError(null);
        documentRepository.save(document);
    }

    private LocalDateTime calculateNextRetryAt(int retryCount) {
        long multiplier = 1L << Math.max(0, retryCount - 1);
        long delayMinutes = Math.min(retryBackoffBaseMinutes * multiplier, retryBackoffMaxMinutes);
        return LocalDateTime.now().plusMinutes(delayMinutes);
    }

    private String sanitizeError(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Unknown error";
        }
        return reason.length() > 1000 ? reason.substring(0, 1000) : reason;
    }

    private DocumentPdfEntity getDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with ID: " + documentId));
    }
}
