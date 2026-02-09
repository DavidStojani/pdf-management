package org.papercloud.de.pdfapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.papercloud.de.core.dto.search.SearchHitDTO;
import org.papercloud.de.core.dto.search.SearchRequestDTO;
import org.papercloud.de.core.dto.search.SearchResultDTO;
import org.papercloud.de.core.ports.outbound.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for SearchController using MockMvc.
 * Tests search endpoints for document search functionality.
 */
@WebMvcTest(controllers = SearchController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
                org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SearchController Integration Tests")
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SearchService searchService;

    private SearchResultDTO sampleSearchResult;
    private SearchHitDTO sampleHit1;
    private SearchHitDTO sampleHit2;

    @BeforeEach
    void setUp() {
        sampleHit1 = SearchHitDTO.builder()
                .documentId("1")
                .documentName("annual-report-2025.pdf")
                .pageNumber(1)
                .textSnippet("This is a snippet from the annual report...")
                .build();

        sampleHit2 = SearchHitDTO.builder()
                .documentId("2")
                .documentName("financial-q1.pdf")
                .pageNumber(1)
                .textSnippet("Financial details for the first quarter...")
                .build();

        sampleSearchResult = SearchResultDTO.builder()
                .hits(Arrays.asList(sampleHit1, sampleHit2))
                .totalHits(2L)
                .totalPages(1)
                .currentPage(0)
                .build();
    }

    @Nested
    @DisplayName("Search Endpoint Tests")
    class SearchEndpointTests {

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should successfully search and return results")
        void search_validQuery_returnsSearchResults() throws Exception {
            // Arrange
            SearchRequestDTO request = SearchRequestDTO.builder()
                    .query("annual report")
                    .page(0)
                    .size(10)
                    .build();

            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenReturn(sampleSearchResult);

            // Act & Assert
            mockMvc.perform(post("/api/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalHits").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1))
                    .andExpect(jsonPath("$.currentPage").value(0))
                    .andExpect(jsonPath("$.hits.length()").value(2))
                    .andExpect(jsonPath("$.hits[0].documentId").value("1"))
                    .andExpect(jsonPath("$.hits[0].documentName").value("annual-report-2025.pdf"))
                    .andExpect(jsonPath("$.hits[0].pageNumber").value(1))
                    .andExpect(jsonPath("$.hits[1].documentId").value("2"));

            verify(searchService).search(argThat(req ->
                    "testuser".equals(req.getUsername()) &&
                    "annual report".equals(req.getQuery())
            ));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should return empty results when no documents match")
        void search_noMatches_returnsEmptyResults() throws Exception {
            // Arrange
            SearchRequestDTO request = SearchRequestDTO.builder()
                    .query("nonexistent term")
                    .build();

            SearchResultDTO emptyResult = SearchResultDTO.builder()
                    .hits(Collections.emptyList())
                    .totalHits(0L)
                    .totalPages(0)
                    .currentPage(0)
                    .build();

            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenReturn(emptyResult);

            // Act & Assert
            mockMvc.perform(post("/api/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalHits").value(0))
                    .andExpect(jsonPath("$.hits.length()").value(0));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should automatically inject authenticated username")
        void search_authenticatedUser_injectsUsername() throws Exception {
            // Arrange
            SearchRequestDTO request = SearchRequestDTO.builder()
                    .query("test query")
                    .build();

            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenReturn(sampleSearchResult);

            // Act & Assert
            mockMvc.perform(post("/api/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk());

            verify(searchService).search(argThat(req -> "testuser".equals(req.getUsername())));
        }

        @Test
        @WithMockUser(username = "anotheruser")
        @DisplayName("should use correct username for different authenticated user")
        void search_differentUser_usesCorrectUsername() throws Exception {
            // Arrange
            SearchRequestDTO request = SearchRequestDTO.builder()
                    .query("financial report")
                    .build();

            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenReturn(sampleSearchResult);

            // Act & Assert
            mockMvc.perform(post("/api/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk());

            verify(searchService).search(argThat(req -> "anotheruser".equals(req.getUsername())));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should handle search with pagination parameters")
        void search_withPagination_returnsPagedResults() throws Exception {
            // Arrange
            SearchRequestDTO request = SearchRequestDTO.builder()
                    .query("report")
                    .page(1)
                    .size(5)
                    .build();

            SearchResultDTO pagedResult = SearchResultDTO.builder()
                    .hits(List.of(sampleHit1))
                    .totalHits(10L)
                    .totalPages(2)
                    .currentPage(1)
                    .build();

            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenReturn(pagedResult);

            // Act & Assert
            mockMvc.perform(post("/api/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalHits").value(10))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.currentPage").value(1))
                    .andExpect(jsonPath("$.hits.length()").value(1));

            verify(searchService).search(argThat(req ->
                    req.getPage() == 1 && req.getSize() == 5
            ));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should handle search with year filter")
        void search_withYearFilter_appliesFilter() throws Exception {
            // Arrange
            SearchRequestDTO request = SearchRequestDTO.builder()
                    .query("contract")
                    .year(2025)
                    .build();

            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenReturn(sampleSearchResult);

            // Act & Assert
            mockMvc.perform(post("/api/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk());

            verify(searchService).search(argThat(req ->
                    req.getYear() != null && req.getYear() == 2025
            ));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should handle search with tags filter")
        void search_withTagsFilter_appliesFilter() throws Exception {
            // Arrange
            SearchRequestDTO request = SearchRequestDTO.builder()
                    .query("invoice")
                    .tags(Arrays.asList("tax", "finance"))
                    .build();

            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenReturn(sampleSearchResult);

            // Act & Assert
            mockMvc.perform(post("/api/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk());

            verify(searchService).search(argThat(req ->
                    req.getTags() != null &&
                    req.getTags().contains("tax") &&
                    req.getTags().contains("finance")
            ));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should handle search with empty query")
        void search_emptyQuery_processesRequest() throws Exception {
            // Arrange
            SearchRequestDTO request = SearchRequestDTO.builder()
                    .query("")
                    .build();

            SearchResultDTO emptyResult = SearchResultDTO.builder()
                    .hits(Collections.emptyList())
                    .totalHits(0L)
                    .totalPages(0)
                    .currentPage(0)
                    .build();

            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenReturn(emptyResult);

            // Act & Assert
            mockMvc.perform(post("/api/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should handle search with special characters in query")
        void search_specialCharacters_processesQuery() throws Exception {
            // Arrange
            SearchRequestDTO request = SearchRequestDTO.builder()
                    .query("contract #2024 (final)")
                    .build();

            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenReturn(sampleSearchResult);

            // Act & Assert
            mockMvc.perform(post("/api/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk());

            verify(searchService).search(argThat(req ->
                    "contract #2024 (final)".equals(req.getQuery())
            ));
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should handle search with unicode characters")
        void search_unicodeCharacters_processesQuery() throws Exception {
            // Arrange
            SearchRequestDTO request = SearchRequestDTO.builder()
                    .query("Über résumé 日本語")
                    .build();

            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenReturn(sampleSearchResult);

            // Act & Assert
            mockMvc.perform(post("/api/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should handle search service exception")
        void search_serviceException_returnsErrorStatus() throws Exception {
            // Arrange
            SearchRequestDTO request = SearchRequestDTO.builder()
                    .query("test")
                    .build();

            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenThrow(new RuntimeException("Search service unavailable"));

            // Act & Assert
            mockMvc.perform(post("/api/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @WithMockUser(username = "testuser")
        @DisplayName("should handle search with all filters combined")
        void search_allFiltersCombined_appliesAllFilters() throws Exception {
            // Arrange
            SearchRequestDTO request = SearchRequestDTO.builder()
                    .query("important document")
                    .year(2025)
                    .tags(Arrays.asList("urgent", "confidential"))
                    .page(0)
                    .size(20)
                    .build();

            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenReturn(sampleSearchResult);

            // Act & Assert
            mockMvc.perform(post("/api/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk());

            verify(searchService).search(argThat(req ->
                    "important document".equals(req.getQuery()) &&
                    req.getYear() == 2025 &&
                    req.getTags() != null &&
                    req.getTags().size() == 2 &&
                    req.getPage() == 0 &&
                    req.getSize() == 20 &&
                    "testuser".equals(req.getUsername())
            ));
        }
    }
}
