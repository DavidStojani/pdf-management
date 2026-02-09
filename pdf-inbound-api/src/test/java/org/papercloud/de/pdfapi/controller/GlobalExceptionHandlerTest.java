package org.papercloud.de.pdfapi.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.papercloud.de.pdfservice.errors.DocumentUploadException;
import org.papercloud.de.pdfservice.errors.InvalidDocumentException;
import org.papercloud.de.pdfservice.errors.UserAuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.nio.file.AccessDeniedException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GlobalExceptionHandler.
 * Verifies that each exception type is correctly mapped to the appropriate HTTP status code
 * and error response format.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("InvalidDocumentException handling")
    class InvalidDocumentExceptionTests {

        @Test
        @DisplayName("should return 400 BAD_REQUEST with error message")
        void handleInvalidDocument_shouldReturnBadRequest() {
            // Arrange
            String errorMessage = "Invalid PDF format";
            InvalidDocumentException exception = new InvalidDocumentException(errorMessage);

            // Act
            ResponseEntity<Map<String, String>> response = exceptionHandler.handleInvalidDocument(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).containsEntry("error", errorMessage);
        }
    }

    @Nested
    @DisplayName("DocumentNotFoundException handling")
    class DocumentNotFoundExceptionTests {

        @Test
        @DisplayName("should return 404 NOT_FOUND with error message")
        void handleDocumentNotFound_shouldReturnNotFound() {
            // Arrange
            String errorMessage = "Document with ID 123 not found";
            DocumentNotFoundException exception = new DocumentNotFoundException(errorMessage);

            // Act
            ResponseEntity<Map<String, String>> response = exceptionHandler.handleDocumentNotFound(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).containsEntry("error", errorMessage);
        }
    }

    @Nested
    @DisplayName("UserAuthenticationException handling")
    class UserAuthenticationExceptionTests {

        @Test
        @DisplayName("should return 401 UNAUTHORIZED with error message")
        void handleUserAuthentication_shouldReturnUnauthorized() {
            // Arrange
            String errorMessage = "Invalid credentials";
            UserAuthenticationException exception = new UserAuthenticationException(errorMessage);

            // Act
            ResponseEntity<Map<String, String>> response = exceptionHandler.handleUserAuthentication(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).containsEntry("error", errorMessage);
        }
    }

    @Nested
    @DisplayName("AccessDeniedException handling")
    class AccessDeniedExceptionTests {

        @Test
        @DisplayName("should return 403 FORBIDDEN with error message")
        void handleAccessDenied_shouldReturnForbidden() {
            // Arrange
            String errorMessage = "Access denied to resource";
            AccessDeniedException exception = new AccessDeniedException(errorMessage);

            // Act
            ResponseEntity<Map<String, String>> response = exceptionHandler.handleAccessDenied(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).containsEntry("error", errorMessage);
        }
    }

    @Nested
    @DisplayName("DocumentUploadException handling")
    class DocumentUploadExceptionTests {

        @Test
        @DisplayName("should return 500 INTERNAL_SERVER_ERROR with error message")
        void handleUploadFailure_shouldReturnInternalServerError() {
            // Arrange
            String errorMessage = "Failed to upload document to storage";
            DocumentUploadException exception = new DocumentUploadException(errorMessage);

            // Act
            ResponseEntity<Map<String, String>> response = exceptionHandler.handleUploadFailure(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).containsEntry("error", errorMessage);
        }
    }

    @Nested
    @DisplayName("MaxUploadSizeExceededException handling")
    class MaxUploadSizeExceededExceptionTests {

        @Test
        @DisplayName("should return 413 PAYLOAD_TOO_LARGE with error message")
        void handleMaxUploadSize_shouldReturnPayloadTooLarge() {
            // Arrange
            MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(10485760L);

            // Act
            ResponseEntity<Map<String, String>> response = exceptionHandler.handleMaxUploadSize(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).contains("10MB");
        }
    }

    @Nested
    @DisplayName("Generic Exception handling")
    class GenericExceptionTests {

        @Test
        @DisplayName("should return 500 INTERNAL_SERVER_ERROR with generic error message")
        void handleFallback_shouldReturnInternalServerError() {
            // Arrange
            Exception exception = new RuntimeException("Something went wrong");

            // Act
            ResponseEntity<Map<String, String>> response = exceptionHandler.handleFallback(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).containsEntry("error", "Unexpected server error");
        }

        @Test
        @DisplayName("should not expose internal exception message to client")
        void handleFallback_shouldNotExposeInternalMessage() {
            // Arrange
            Exception exception = new RuntimeException("Internal database connection failed");

            // Act
            ResponseEntity<Map<String, String>> response = exceptionHandler.handleFallback(exception);

            // Assert
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("error")).isEqualTo("Unexpected server error");
            assertThat(response.getBody().get("error")).doesNotContain("database");
        }

        @Test
        @DisplayName("should handle NullPointerException as generic exception")
        void handleFallback_shouldHandleNullPointerException() {
            // Arrange
            Exception exception = new NullPointerException("Null pointer in service layer");

            // Act
            ResponseEntity<Map<String, String>> response = exceptionHandler.handleFallback(exception);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).containsEntry("error", "Unexpected server error");
        }
    }
}
