package org.papercloud.de.pdfapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.papercloud.de.common.dto.DocumentDTO;
import org.papercloud.de.common.dto.DocumentUploadDTO;
import org.papercloud.de.pdfservice.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Document Management", description = "APIs for uploading, downloading, and searching PDFs")
@RequiredArgsConstructor
public class DocumentController {

  private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
  private final DocumentService documentService;


  @Operation(summary = "Upload a PDF document")
  @PostMapping("/upload")
  public ResponseEntity<String> uploadPdf(@RequestParam("file") MultipartFile file) {
    try {
      logger.info("File name: {}", file.getOriginalFilename());
      DocumentUploadDTO documentUploadDTO = DocumentUploadDTO.builder()
          .fileName(file.getOriginalFilename())
          .contentType(file.getContentType())
          .size(file.getSize())
          .inputStream(file.getInputStream())
          .build();

      logger.info("DOCUMENT UPLOAD DTO: {}", documentUploadDTO);
      DocumentDTO documentDTO = documentService.processDocument(documentUploadDTO);
      return ResponseEntity.ok("File uploaded successfully: " + file.getOriginalFilename());

    } catch (IOException e) {
      return ResponseEntity.status(500).body("Error uploading file: " + e.getMessage());
    }
  }

  // Simple endpoint to test basic connectivity
  @GetMapping("/ping")
  public ResponseEntity<String> ping() {
    logger.info("Ping endpoint called");
    return ResponseEntity.ok("Pong! Server is running");
  }

  private boolean isValidPdfFile(MultipartFile file) {
    return "application/pdf".equals(file.getContentType());
  }
}
