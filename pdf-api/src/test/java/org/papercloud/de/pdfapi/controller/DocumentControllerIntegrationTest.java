package org.papercloud.de.pdfapi.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.EmbeddedDatabaseAutoConfiguration"
)
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WithUserDetails("testuser")
public class DocumentControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:14.1")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    static  {
        postgresContainer.start();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // 🟢 Make sure Hibernate creates the schema
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.show-sql", () -> "true"); // optional: debug SQL
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeAll
    void insertUser() {
        if (userRepository.findByUsername("testuser").isEmpty()) {
            UserEntity user = new UserEntity();
            user.setUsername("testuser");
            user.setPassword(passwordEncoder.encode("testpass"));
            userRepository.save(user);
        }
    }

    @Test
    void upload_validFile_shouldReturnCreated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.pdf",
                "application/pdf",
                Files.readAllBytes(Paths.get("src/test/resources/valide_bank_info.pdf"))
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Document uploaded successfully")));
    }


    @Test
    void pingEndpointShouldReturnPong() throws Exception {
        mockMvc.perform(get("/api/documents/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"message\":\"Pong! Server is running\"}"));
    }
    @Test
    void upload_invalidFile_shouldReturnBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.txt",
                "text/plain",
                "Some text content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .with(csrf()))                     // ← add CSRF
                .andExpect(status().isBadRequest());  // ← now your controller’s @Valid kicks in
    }

    @Test
    void upload_emptyFile_shouldReturnBadRequest() throws Exception {
        // Create a sample PDF file for testing
        byte[] pdfContent = Files.readAllBytes(Paths.get("src/test/resources/empty_sample.pdf"));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty_sample.pdf",
                "application/pdf",
                pdfContent
        );

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file))
                //.andExpect(status().isOk())
                .andExpect(status().isBadRequest());
        //.andExpect(content().string(org.hamcrest.Matchers.containsString("Document uploaded successfully")));
    }

    @Test
    void download_nonExistingDocument_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/documents/999/download"))
                .andExpect(status().isNotFound());
    }
}