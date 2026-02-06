package org.papercloud.de.pdfservice.errors;

public class DocumentUploadException extends RuntimeException {
    public DocumentUploadException(String message) {
        super(message);
    }

    public DocumentUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
