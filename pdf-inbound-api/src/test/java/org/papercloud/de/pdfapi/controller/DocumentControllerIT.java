package org.papercloud.de.pdfapi.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.papercloud.de.pdfapi.PdfApiApplication;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.FavouriteRepository;
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

import static org.papercloud.de.core.domain.Document.Status.UPLOADED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = PdfApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
public class DocumentControllerIT {

    private static final String DEFAULT_PASSWORD = "test";
    private static final byte[] SAMPLE_PDF_CONTENT = "Fake PDF Content".getBytes();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FavouriteRepository favouriteRepository;

    @BeforeEach
    void setUp() {
        favouriteRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void ping_returnsOk() throws Exception {
        mockMvc.perform(get("/api/documents/ping"))
                .andExpect(status().isOk());
    }

    @Nested
    class Download {

        @Test
        @WithMockUser(username = "testuser")
        void returnsDocumentWithHeaders() throws Exception {
            UserEntity user = createUser("testuser");
            DocumentPdfEntity saved = createDocument(user, "test-file.pdf", SAMPLE_PDF_CONTENT);

            mockMvc.perform(get("/api/documents/" + saved.getId() + "/download"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test-file.pdf\""))
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF_VALUE))
                    .andExpect(content().bytes(SAMPLE_PDF_CONTENT));
        }

        @Test
        @WithMockUser(username = "testuser")
        void nonExistentDocument_returns404() throws Exception {
            mockMvc.perform(get("/api/documents/99999/download"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "testuser")
        void documentOwnedByDifferentUser_returns403() throws Exception {
            UserEntity owner = createUser("owner");
            createUser("testuser");
            DocumentPdfEntity saved = createDocument(owner, "private-file.pdf", SAMPLE_PDF_CONTENT);

            mockMvc.perform(get("/api/documents/" + saved.getId() + "/download"))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        void withoutAuthentication_returns401() throws Exception {
            UserEntity user = createUser("someuser");
            DocumentPdfEntity saved = createDocument(user, "test-file.pdf", SAMPLE_PDF_CONTENT);

            mockMvc.perform(get("/api/documents/" + saved.getId() + "/download"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class Upload {

        @Test
        @WithMockUser(username = "uploaduser")
        void validPdfFile_returns200WithDocumentId() throws Exception {
            createUser("uploaduser");

            mockMvc.perform(multipart("/api/documents/upload")
                            .file(createPdfMultipartFile("upload-test.pdf", SAMPLE_PDF_CONTENT)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.documentId").exists());
        }

        @Test
        void withoutAuthentication_returns401() throws Exception {
            mockMvc.perform(multipart("/api/documents/upload")
                            .file(createPdfMultipartFile("upload-test.pdf", SAMPLE_PDF_CONTENT)))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "uploaduser")
        void nonPdfFile_returns400() throws Exception {
            createUser("uploaduser");
            MockMultipartFile textFile = new MockMultipartFile(
                    "file", "test.txt", MediaType.TEXT_PLAIN_VALUE,
                    "This is not a PDF".getBytes());

            mockMvc.perform(multipart("/api/documents/upload")
                            .file(textFile))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "uploaduser")
        void emptyFile_returns400() throws Exception {
            createUser("uploaduser");
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file", "empty.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]);

            mockMvc.perform(multipart("/api/documents/upload")
                            .file(emptyFile))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Favourites {

        @Test
        @WithMockUser(username = "testuser")
        void addFavourite_returns204() throws Exception {
            UserEntity user = createUser("testuser");
            DocumentPdfEntity doc = createDocument(user, "fav.pdf", SAMPLE_PDF_CONTENT);

            mockMvc.perform(post("/api/documents/" + doc.getId() + "/favourite"))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "testuser")
        void addFavourite_nonExistentDocument_returns404() throws Exception {
            createUser("testuser");

            mockMvc.perform(post("/api/documents/99999/favourite"))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @Test
        void addFavourite_withoutAuth_returns401() throws Exception {
            mockMvc.perform(post("/api/documents/1/favourite"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "testuser")
        void removeFavourite_returns204() throws Exception {
            UserEntity user = createUser("testuser");
            DocumentPdfEntity doc = createDocument(user, "fav.pdf", SAMPLE_PDF_CONTENT);

            mockMvc.perform(delete("/api/documents/" + doc.getId() + "/favourite"))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @Test
        void removeFavourite_withoutAuth_returns401() throws Exception {
            mockMvc.perform(delete("/api/documents/1/favourite"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "testuser")
        void getFavourites_returnsEmptyList() throws Exception {
            createUser("testuser");

            mockMvc.perform(get("/api/documents/favourites"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @WithMockUser(username = "testuser")
        void getFavourites_returnsFavouritedDoc() throws Exception {
            UserEntity user = createUser("testuser");
            DocumentPdfEntity doc = createDocument(user, "fav.pdf", SAMPLE_PDF_CONTENT);

            // Add favourite first
            mockMvc.perform(post("/api/documents/" + doc.getId() + "/favourite"))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/documents/favourites"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(doc.getId()))
                    .andExpect(jsonPath("$[0].isFavourite").value(true));
        }

        @Test
        void getFavourites_withoutAuth_returns401() throws Exception {
            mockMvc.perform(get("/api/documents/favourites"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    // --- Test helpers ---

    private UserEntity createUser(String username) {
        UserEntity user = UserEntity.builder()
                .username(username)
                .password(DEFAULT_PASSWORD)
                .build();
        return userRepository.save(user);
    }

    private DocumentPdfEntity createDocument(UserEntity owner, String filename, byte[] content) {
        DocumentPdfEntity entity = DocumentPdfEntity.builder()
                .filename(filename)
                .contentType(MediaType.APPLICATION_PDF_VALUE)
                .pdfContent(content)
                .owner(owner)
                .status(UPLOADED)
                .build();
        return documentRepository.save(entity);
    }

    private MockMultipartFile createPdfMultipartFile(String filename, byte[] content) {
        return new MockMultipartFile("file", filename, MediaType.APPLICATION_PDF_VALUE, content);
    }
}
