package org.papercloud.de.pdfapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.papercloud.de.core.dto.document.DocumentDTO;
import org.papercloud.de.core.dto.document.DocumentDownloadDTO;
import org.papercloud.de.pdfservice.service.DocumentService;
import org.papercloud.de.pdfservice.textutils.FolderScannerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.AccessDeniedException;
import java.util.Map;

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
    public ResponseEntity<Map<String, String>> uploadPdf(@RequestParam("file") MultipartFile file, Authentication authentication) {
        DocumentDTO savedDocument = documentService.processUpload(file, authentication);

        return ResponseEntity.ok(Map.of(
                "message", "Document uploaded successfully",
                "documentId", savedDocument.getId().toString()
        ));
    }

    @Operation(summary = "Download a PDF document")
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id, Authentication authentication) throws AccessDeniedException {
        DocumentDownloadDTO document = documentService.downloadDocument(authentication.getName(), id);
        String sanitizedFileName = document.getFileName().replaceAll("[^a-zA-Z0-9.\\-_ ()\\[\\]]", "_");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sanitizedFileName + "\"")
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .contentLength(document.getSize())
                .body(document.getContent());
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        logger.info("Ping endpoint called");
        return ResponseEntity.ok(Map.of("message", "Pong! Server is running"));
    }

}
