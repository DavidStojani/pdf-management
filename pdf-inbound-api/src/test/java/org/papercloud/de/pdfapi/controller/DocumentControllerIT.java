package org.papercloud.de.pdfapi.controller;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.papercloud.de.pdfapi.PdfApiApplication;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.multipart.MultipartFile;

import static org.papercloud.de.core.domain.Document.Status.UPLOADED;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = PdfApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
        locations = "classpath:application-test.yml")
public class DocumentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    DocumentRepository documentRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void test_ping() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.get("/api/documents/ping"))
                .andExpect(status().isOk());

    }

    @Test
    @WithMockUser(username = "testuser")
    void test_download() throws Exception {
        UserEntity user = UserEntity.builder()
                .username("testuser")
                .password("test")
                .build();
        userRepository.save(user);
        // 1. Arrange: Create and save a document to the H2 database
        DocumentPdfEntity entity = DocumentPdfEntity.builder()
                .filename("test-file.pdf")
                .contentType(MediaType.APPLICATION_PDF_VALUE)
                .pdfContent("Fake PDF Content".getBytes()) // Ensure this field name matches your entity
                .owner(user)
                .status(UPLOADED)
                .build();

        DocumentPdfEntity saved = documentRepository.save(entity);
        Long documentId = saved.getId();

        // 2. Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.get("/api/documents/" + documentId + "/download"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test-file.pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(content().bytes("Fake PDF Content".getBytes()));
    }


    private MultipartFile createValidPdfFile() {
        return new MockMultipartFile(
                "file",
                "test.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF content".getBytes()
        );
    }
}
