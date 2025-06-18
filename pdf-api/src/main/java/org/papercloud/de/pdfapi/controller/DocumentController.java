package org.papercloud.de.pdfapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.common.dto.document.DocumentDTO;
import org.papercloud.de.common.dto.document.DocumentDownloadDTO;
import org.papercloud.de.common.dto.document.DocumentUploadDTO;
import org.papercloud.de.common.dto.document.FolderPathDTO;
import org.papercloud.de.pdfservice.search.DocumentService;
import org.papercloud.de.pdfservice.textutils.FolderScannerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Document Management", description = "APIs for uploading, downloading, and searching PDFs")
@RequiredArgsConstructor
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    private final DocumentService documentService;
    private final FolderScannerService folderScannerService;

    @Operation(summary = "Upload a PDF document")
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadPdf(@RequestParam("file") MultipartFile file) {
        if (!isValidPdf(file)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid file format. Only PDF files are allowed."));
        }

        try {
            DocumentUploadDTO documentUploadDTO = DocumentUploadDTO.builder()
                    .fileName(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .inputPdfBytes(file.getBytes())
                    .build();

            DocumentDTO savedDocument = documentService.processDocument(documentUploadDTO, getCurrentUsername());

            return ResponseEntity.ok(Map.of(
                    "message", "Document uploaded successfully",
                    "documentId", savedDocument.getId().toString()
            ));
        } catch (Exception e) {
            logger.error("Unexpected error during file upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error uploading file: " + e.getMessage()));
        }
    }

    @Operation(summary = "Download a PDF document")
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) throws AccessDeniedException {
        String username = getCurrentUsername();
        DocumentDownloadDTO document = documentService.downloadDocument(username, id);

        if (document == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .contentLength(document.getSize())
                .body(document.getContent());
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        logger.info("Ping endpoint called");
        return ResponseEntity.ok(Map.of("message", "Pong! Server is running"));
    }

    @PostMapping("/folder")
    public ResponseEntity<Map<String, String>> setUserFolder(@RequestBody FolderPathDTO request) {

        String username = getCurrentUsername();
        folderScannerService.scanUserFolder(username, request.getFolderPath());

        return ResponseEntity.ok(Map.of("message", request.getFolderPath()));
    }


    // ========== Private Helpers ==========

    private boolean isValidPdf(MultipartFile file) {
        return !file.isEmpty() && "application/pdf".equalsIgnoreCase(file.getContentType());
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
