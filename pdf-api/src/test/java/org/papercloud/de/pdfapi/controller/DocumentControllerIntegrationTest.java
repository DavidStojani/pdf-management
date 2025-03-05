package org.papercloud.de.pdfapi.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class DocumentControllerIntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:14.1")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test");

  @Autowired
  private MockMvc mockMvc;

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
    registry.add("spring.datasource.username", postgresContainer::getUsername);
    registry.add("spring.datasource.password", postgresContainer::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    registry.add("spring.datasource.hikari.maximumPoolSize", () -> "5");
  }

  @BeforeAll
  public static void setup() {
    postgresContainer.start();
  }

  @Transactional
  @Test
  void upload_validFile_shouldReturnCreated() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "sample.pdf",
        "application/pdf",
        Files.readAllBytes(Paths.get("src/test/resources/valide_bank_info.pdf"))
    );

    mockMvc.perform(multipart("/api/documents/upload")
            .file(file))
        .andExpect(status().isOk())
        .andExpect(content().string(
            org.hamcrest.Matchers.containsString("Document uploaded successfully")));
  }

  @Test
  void pingEndpointShouldReturnPong() throws Exception {
    mockMvc.perform(get("/api/documents/ping"))
        .andExpect(status().isOk())
        .andExpect(content().string("Pong! Server is running"));
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
            .file(file))
        .andExpect(status().isBadRequest());
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