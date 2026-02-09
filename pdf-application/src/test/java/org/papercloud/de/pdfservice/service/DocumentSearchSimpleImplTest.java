package org.papercloud.de.pdfservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.core.dto.search.SearchHitDTO;
import org.papercloud.de.core.dto.search.SearchResultDTO;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.PageRepository;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DocumentSearchSimpleImpl.
 * Tests search functionality, snippet extraction, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentSearchSimpleImpl Tests")
class DocumentSearchSimpleImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private PageRepository pageRepository;

    @InjectMocks
    private DocumentSearchSimpleImpl documentSearch;

    private DocumentPdfEntity testDocument;
    private PagesPdfEntity testPage;

    @BeforeEach
    void setUp() {
        testDocument = DocumentPdfEntity.builder()
                .id(1L)
                .title("Test Document")
                .build();
    }

    @Nested
    @DisplayName("Search Results Tests")
    class SearchResultsTests {

        @Test
        @DisplayName("should return search results when term is found")
        void should_returnSearchResults_when_termIsFound() {
            // Arrange
            String searchTerm = "important";
            String pageText = "This is an important document with important information about the topic.";
            testPage = createPage(1, pageText);

            when(pageRepository.findByExtractedTextContaining(searchTerm))
                    .thenReturn(List.of(testPage));

            // Act
            SearchResultDTO result = documentSearch.searchDocumentByText(searchTerm);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalHits()).isEqualTo(1);
            assertThat(result.getHits()).hasSize(1);
            assertThat(result.getCurrentPage()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);

            SearchHitDTO hit = result.getHits().get(0);
            assertThat(hit.getDocumentId()).isEqualTo("1");
            assertThat(hit.getDocumentName()).isEqualTo("Test Document");
            assertThat(hit.getPageNumber()).isEqualTo(1);
            assertThat(hit.getTextSnippet()).contains("important");
        }

        @Test
        @DisplayName("should return empty results when no documents match")
        void should_returnEmptyResults_when_noDocumentsMatch() {
            // Arrange
            String searchTerm = "nonexistent";
            when(pageRepository.findByExtractedTextContaining(searchTerm))
                    .thenReturn(Collections.emptyList());

            // Act
            SearchResultDTO result = documentSearch.searchDocumentByText(searchTerm);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getTotalHits()).isZero();
            assertThat(result.getHits()).isEmpty();
        }

        @Test
        @DisplayName("should return multiple hits from multiple pages")
        void should_returnMultipleHits_when_multiplePages() {
            // Arrange
            String searchTerm = "keyword";
            PagesPdfEntity page1 = createPage(1, "First page contains keyword");
            PagesPdfEntity page2 = createPage(2, "Second page also has keyword");

            when(pageRepository.findByExtractedTextContaining(searchTerm))
                    .thenReturn(List.of(page1, page2));

            // Act
            SearchResultDTO result = documentSearch.searchDocumentByText(searchTerm);

            // Assert
            assertThat(result.getTotalHits()).isEqualTo(2);
            assertThat(result.getHits()).hasSize(2);
            assertThat(result.getHits().get(0).getPageNumber()).isEqualTo(1);
            assertThat(result.getHits().get(1).getPageNumber()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Snippet Extraction Tests")
    class SnippetExtractionTests {

        @Test
        @DisplayName("should extract snippet with term in the middle of text")
        void should_extractSnippet_when_termInMiddle() {
            // Arrange
            String searchTerm = "target";
            String prefix = "a".repeat(150); // 150 chars before
            String suffix = "z".repeat(150); // 150 chars after
            String pageText = prefix + "target" + suffix;
            testPage = createPage(1, pageText);

            when(pageRepository.findByExtractedTextContaining(searchTerm))
                    .thenReturn(List.of(testPage));

            // Act
            SearchResultDTO result = documentSearch.searchDocumentByText(searchTerm);

            // Assert
            SearchHitDTO hit = result.getHits().get(0);
            assertThat(hit.getTextSnippet()).startsWith("...");
            assertThat(hit.getTextSnippet()).endsWith("...");
            assertThat(hit.getTextSnippet()).contains("target");
        }

        @Test
        @DisplayName("should extract snippet without leading ellipsis when term is at start")
        void should_extractSnippetWithoutLeadingEllipsis_when_termAtStart() {
            // Arrange
            String searchTerm = "beginning";
            // Text must be longer than searchTerm.length() + CONTEXT_CHARS (100) to trigger trailing ellipsis
            String pageText = "beginning" + "x".repeat(200);
            testPage = createPage(1, pageText);

            when(pageRepository.findByExtractedTextContaining(searchTerm))
                    .thenReturn(List.of(testPage));

            // Act
            SearchResultDTO result = documentSearch.searchDocumentByText(searchTerm);

            // Assert
            SearchHitDTO hit = result.getHits().get(0);
            assertThat(hit.getTextSnippet()).doesNotStartWith("...");
            assertThat(hit.getTextSnippet()).startsWith("beginning");
            assertThat(hit.getTextSnippet()).endsWith("...");
        }

        @Test
        @DisplayName("should extract snippet without trailing ellipsis when term is at end")
        void should_extractSnippetWithoutTrailingEllipsis_when_termAtEnd() {
            // Arrange
            String searchTerm = "end";
            String prefix = "a".repeat(150);
            String pageText = prefix + "end";
            testPage = createPage(1, pageText);

            when(pageRepository.findByExtractedTextContaining(searchTerm))
                    .thenReturn(List.of(testPage));

            // Act
            SearchResultDTO result = documentSearch.searchDocumentByText(searchTerm);

            // Assert
            SearchHitDTO hit = result.getHits().get(0);
            assertThat(hit.getTextSnippet()).startsWith("...");
            assertThat(hit.getTextSnippet()).doesNotEndWith("...");
            assertThat(hit.getTextSnippet()).endsWith("end");
        }

        @Test
        @DisplayName("should handle case-insensitive search")
        void should_handleCaseInsensitiveSearch() {
            // Arrange
            String searchTerm = "CaSe";
            String pageText = "This text contains the word case in different forms";
            testPage = createPage(1, pageText);

            when(pageRepository.findByExtractedTextContaining(searchTerm))
                    .thenReturn(List.of(testPage));

            // Act
            SearchResultDTO result = documentSearch.searchDocumentByText(searchTerm);

            // Assert
            SearchHitDTO hit = result.getHits().get(0);
            assertThat(hit.getTextSnippet()).contains("case");
        }

        @Test
        @DisplayName("should extract snippet without ellipsis for short text")
        void should_extractSnippetWithoutEllipsis_when_textIsShort() {
            // Arrange
            String searchTerm = "short";
            String pageText = "This is a short text";
            testPage = createPage(1, pageText);

            when(pageRepository.findByExtractedTextContaining(searchTerm))
                    .thenReturn(List.of(testPage));

            // Act
            SearchResultDTO result = documentSearch.searchDocumentByText(searchTerm);

            // Assert
            SearchHitDTO hit = result.getHits().get(0);
            assertThat(hit.getTextSnippet()).doesNotStartWith("...");
            assertThat(hit.getTextSnippet()).doesNotEndWith("...");
            assertThat(hit.getTextSnippet()).isEqualTo(pageText);
        }

        @Test
        @DisplayName("should return empty snippet when term not found in page text")
        void should_returnEmptySnippet_when_termNotInPageText() {
            // Arrange
            String searchTerm = "missing";
            String pageText = "This text does not contain the search term";
            testPage = createPage(1, pageText);

            when(pageRepository.findByExtractedTextContaining(searchTerm))
                    .thenReturn(List.of(testPage));

            // Act
            SearchResultDTO result = documentSearch.searchDocumentByText(searchTerm);

            // Assert
            SearchHitDTO hit = result.getHits().get(0);
            assertThat(hit.getTextSnippet()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("should handle empty search term")
        void should_handleEmptySearchTerm() {
            // Arrange
            when(pageRepository.findByExtractedTextContaining(""))
                    .thenReturn(Collections.emptyList());

            // Act
            SearchResultDTO result = documentSearch.searchDocumentByText("");

            // Assert
            assertThat(result.getTotalHits()).isZero();
        }

        @Test
        @DisplayName("should handle page with null title")
        void should_handlePageWithNullTitle() {
            // Arrange
            String searchTerm = "test";
            testDocument.setTitle(null);
            testPage = createPage(1, "test content");

            when(pageRepository.findByExtractedTextContaining(searchTerm))
                    .thenReturn(List.of(testPage));

            // Act
            SearchResultDTO result = documentSearch.searchDocumentByText(searchTerm);

            // Assert
            SearchHitDTO hit = result.getHits().get(0);
            assertThat(hit.getDocumentName()).isNull();
        }

        @Test
        @DisplayName("should handle special characters in search term")
        void should_handleSpecialCharacters_in_searchTerm() {
            // Arrange
            String searchTerm = "test@example.com";
            String pageText = "Contact us at test@example.com for more information";
            testPage = createPage(1, pageText);

            when(pageRepository.findByExtractedTextContaining(searchTerm))
                    .thenReturn(List.of(testPage));

            // Act
            SearchResultDTO result = documentSearch.searchDocumentByText(searchTerm);

            // Assert
            assertThat(result.getTotalHits()).isEqualTo(1);
            SearchHitDTO hit = result.getHits().get(0);
            assertThat(hit.getTextSnippet()).contains("test@example.com");
        }
    }

    private PagesPdfEntity createPage(int pageNumber, String pageText) {
        return PagesPdfEntity.builder()
                .id((long) pageNumber)
                .document(testDocument)
                .pageNumber(pageNumber)
                .pageText(pageText)
                .build();
    }
}
