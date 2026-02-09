package org.papercloud.de.pdfapi.controller;

import java.nio.file.AccessDeniedException;
import java.util.Map;

import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.papercloud.de.pdfservice.errors.DocumentUploadException;
import org.papercloud.de.pdfservice.errors.InvalidDocumentException;
import org.papercloud.de.pdfservice.errors.UserAuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidDocumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidDocument(InvalidDocumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleDocumentNotFound(DocumentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(UserAuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleUserAuthentication(UserAuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(DocumentUploadException.class)
    public ResponseEntity<Map<String, String>> handleUploadFailure(DocumentUploadException exception) {
        logger.error("Document upload failed", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", "File size exceeds the maximum allowed limit of 10MB"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleFallback(Exception exception) {
        logger.error("Unexpected error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Unexpected server error"));
    }
}
