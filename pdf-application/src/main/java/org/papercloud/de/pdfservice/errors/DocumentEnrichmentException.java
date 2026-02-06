package org.papercloud.de.pdfservice.errors;

public class DocumentEnrichmentException extends RuntimeException {
    public DocumentEnrichmentException(String message) {
        super(message);
    }

    public DocumentEnrichmentException(String message, Throwable cause) {
        super(message, cause);
    }
}