package org.papercloud.de.pdfservice.processor;

public interface DocumentEnrichmentProcessor {
    void enrichDocument(Long documentId) throws Exception;
}
