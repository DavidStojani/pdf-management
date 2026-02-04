package org.papercloud.de.pdfservice.textutils;

import lombok.RequiredArgsConstructor;
import org.papercloud.de.core.dto.document.DocumentUploadDTO;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.papercloud.de.pdfservice.search.DocumentService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class FolderScannerService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final DocumentService documentService;


    public void scanUserFolder(String username, String folderPath) {

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));


        user.setFolderPath(folderPath);
        userRepository.save(user);

        Path folder = Paths.get(folderPath);
        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            throw new RuntimeException("Invalid folder path: " + folderPath);
        }

        try (Stream<Path> files = Files.walk(folder)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".pdf"))
                    .forEach(path -> processWithService(path, user.getUsername()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan folder: " + folderPath, e);
        }
    }

    private void processWithService(Path path, String username) {
        try {
            byte[] pdfBytes = Files.readAllBytes(path);  // Fully load file into memory
            String filename = path.getFileName().toString();

            if (documentRepository.existsByFilenameAndOwnerUsername(filename, username)) return;

            DocumentUploadDTO uploadDTO = DocumentUploadDTO.builder()
                    .fileName(filename)
                    .contentType("application/pdf")
                    .size(pdfBytes.length)
                    .inputPdfBytes(pdfBytes) // safe stream from fully read bytes
                    .build();

            documentService.processDocument(uploadDTO, username);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process file: " + path, e);
        }
    }

}
