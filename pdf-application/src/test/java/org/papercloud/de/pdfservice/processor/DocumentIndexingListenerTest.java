package org.papercloud.de.pdfservice.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.core.dto.search.IndexableDocumentDTO;
import org.papercloud.de.core.events.DocumentEnrichedEvent;
import org.papercloud.de.core.ports.outbound.SearchService;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.PageRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DocumentIndexingListener.
 * Tests indexing workflow, text aggregation, and null handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentIndexingListener Tests")
class DocumentIndexingListenerTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private PageRepository pageRepository;

    @Mock
    private SearchService searchService;

    @InjectMocks
    private DocumentIndexingListener documentIndexingListener;

    private DocumentPdfEntity testDocument;
    private DocumentEnrichedEvent enrichedEvent;

    @BeforeEach
    void setUp() {
        UserEntity testOwner = UserEntity.builder()
                .id(1L)
                .username("testuser")
                .build();

        testDocument = DocumentPdfEntity.builder()
                .id(1L)
                .filename("test.pdf")
                .title("Test Document Title")
                .contentType("application/pdf")
                .tags(Arrays.asList("invoice", "business"))
                .dateOnDocument(LocalDate.of(2023, 5, 15))
                .owner(testOwner)
                .build();

        enrichedEvent = new DocumentEnrichedEvent(1L);
    }

    @Nested
    @DisplayName("Document Not Found Tests")
    class DocumentNotFoundTests {

        @Test
        @DisplayName("should throw exception when document not found")
        void should_throwException_when_documentNotFound() {
            // Arrange
            when(documentRepository.findById(1L))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> documentIndexingListener.handleDocumentEnriched(enrichedEvent))
                    .isInstanceOf(DocumentNotFoundException.class)
                    .hasMessageContaining("Document not found with ID: 1");

            verify(searchService, never()).indexDocument(any());
        }
    }

    @Nested
    @DisplayName("Text Aggregation Tests")
    class TextAggregationTests {

        @Test
        @DisplayName("should aggregate text from multiple pages")
        void should_aggregateText_from_multiplePages() {
            // Arrange
            PagesPdfEntity page1 = createPage(1, "First page content");
            PagesPdfEntity page2 = createPage(2, "Second page content");
            PagesPdfEntity page3 = createPage(3, "Third page content");

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(Arrays.asList(page1, page2, page3));

            // Act
            documentIndexingListener.handleDocumentEnriched(enrichedEvent);

            // Assert
            ArgumentCaptor<IndexableDocumentDTO> captor = ArgumentCaptor.forClass(IndexableDocumentDTO.class);
            verify(searchService).indexDocument(captor.capture());

            IndexableDocumentDTO indexedDoc = captor.getValue();
            assertThat(indexedDoc.getFullText())
                    .isEqualTo("First page content\nSecond page content\nThird page content");
        }

        @Test
        @DisplayName("should filter out null page text during aggregation")
        void should_filterOutNullPageText() {
            // Arrange
            PagesPdfEntity page1 = createPage(1, "Page 1");
            PagesPdfEntity page2 = createPage(2, null);
            PagesPdfEntity page3 = createPage(3, "Page 3");

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(Arrays.asList(page1, page2, page3));

            // Act
            documentIndexingListener.handleDocumentEnriched(enrichedEvent);

            // Assert
            ArgumentCaptor<IndexableDocumentDTO> captor = ArgumentCaptor.forClass(IndexableDocumentDTO.class);
            verify(searchService).indexDocument(captor.capture());

            IndexableDocumentDTO indexedDoc = captor.getValue();
            assertThat(indexedDoc.getFullText()).isEqualTo("Page 1\nPage 3");
        }

        @Test
        @DisplayName("should filter out blank page text during aggregation")
        void should_filterOutBlankPageText() {
            // Arrange
            PagesPdfEntity page1 = createPage(1, "Content");
            PagesPdfEntity page2 = createPage(2, "   ");
            PagesPdfEntity page3 = createPage(3, "");

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(Arrays.asList(page1, page2, page3));

            // Act
            documentIndexingListener.handleDocumentEnriched(enrichedEvent);

            // Assert
            ArgumentCaptor<IndexableDocumentDTO> captor = ArgumentCaptor.forClass(IndexableDocumentDTO.class);
            verify(searchService).indexDocument(captor.capture());

            IndexableDocumentDTO indexedDoc = captor.getValue();
            assertThat(indexedDoc.getFullText()).isEqualTo("Content");
        }

        @Test
        @DisplayName("should return empty string when no pages have text")
        void should_returnEmptyString_when_noPagesHaveText() {
            // Arrange
            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(Collections.emptyList());

            // Act
            documentIndexingListener.handleDocumentEnriched(enrichedEvent);

            // Assert
            ArgumentCaptor<IndexableDocumentDTO> captor = ArgumentCaptor.forClass(IndexableDocumentDTO.class);
            verify(searchService).indexDocument(captor.capture());

            IndexableDocumentDTO indexedDoc = captor.getValue();
            assertThat(indexedDoc.getFullText()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Title Fallback Tests")
    class TitleFallbackTests {

        @Test
        @DisplayName("should use title when available")
        void should_useTitle_when_available() {
            // Arrange
            testDocument.setTitle("Document Title");
            testDocument.setFilename("original-filename.pdf");

            PagesPdfEntity page = createPage(1, "Content");

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(page));

            // Act
            documentIndexingListener.handleDocumentEnriched(enrichedEvent);

            // Assert
            ArgumentCaptor<IndexableDocumentDTO> captor = ArgumentCaptor.forClass(IndexableDocumentDTO.class);
            verify(searchService).indexDocument(captor.capture());

            IndexableDocumentDTO indexedDoc = captor.getValue();
            assertThat(indexedDoc.getFileName()).isEqualTo("Document Title");
        }

        @Test
        @DisplayName("should fallback to filename when title is null")
        void should_fallbackToFilename_when_titleIsNull() {
            // Arrange
            testDocument.setTitle(null);
            testDocument.setFilename("fallback-filename.pdf");

            PagesPdfEntity page = createPage(1, "Content");

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(page));

            // Act
            documentIndexingListener.handleDocumentEnriched(enrichedEvent);

            // Assert
            ArgumentCaptor<IndexableDocumentDTO> captor = ArgumentCaptor.forClass(IndexableDocumentDTO.class);
            verify(searchService).indexDocument(captor.capture());

            IndexableDocumentDTO indexedDoc = captor.getValue();
            assertThat(indexedDoc.getFileName()).isEqualTo("fallback-filename.pdf");
        }
    }

    @Nested
    @DisplayName("Null Tags and Date Defaults Tests")
    class NullTagsAndDateDefaultsTests {

        @Test
        @DisplayName("should use empty list when tags are null")
        void should_useEmptyList_when_tagsAreNull() {
            // Arrange
            testDocument.setTags(null);

            PagesPdfEntity page = createPage(1, "Content");

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(page));

            // Act
            documentIndexingListener.handleDocumentEnriched(enrichedEvent);

            // Assert
            ArgumentCaptor<IndexableDocumentDTO> captor = ArgumentCaptor.forClass(IndexableDocumentDTO.class);
            verify(searchService).indexDocument(captor.capture());

            IndexableDocumentDTO indexedDoc = captor.getValue();
            assertThat(indexedDoc.getTags()).isEmpty();
        }

        @Test
        @DisplayName("should use current year when date is null")
        void should_useCurrentYear_when_dateIsNull() {
            // Arrange
            testDocument.setDateOnDocument(null);

            PagesPdfEntity page = createPage(1, "Content");

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(page));

            // Act
            documentIndexingListener.handleDocumentEnriched(enrichedEvent);

            // Assert
            ArgumentCaptor<IndexableDocumentDTO> captor = ArgumentCaptor.forClass(IndexableDocumentDTO.class);
            verify(searchService).indexDocument(captor.capture());

            IndexableDocumentDTO indexedDoc = captor.getValue();
            assertThat(indexedDoc.getYear()).isEqualTo(LocalDate.now().getYear());
        }

        @Test
        @DisplayName("should use document year when date is available")
        void should_useDocumentYear_when_dateIsAvailable() {
            // Arrange
            testDocument.setDateOnDocument(LocalDate.of(2022, 8, 20));

            PagesPdfEntity page = createPage(1, "Content");

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(page));

            // Act
            documentIndexingListener.handleDocumentEnriched(enrichedEvent);

            // Assert
            ArgumentCaptor<IndexableDocumentDTO> captor = ArgumentCaptor.forClass(IndexableDocumentDTO.class);
            verify(searchService).indexDocument(captor.capture());

            IndexableDocumentDTO indexedDoc = captor.getValue();
            assertThat(indexedDoc.getYear()).isEqualTo(2022);
        }
    }

    @Nested
    @DisplayName("Complete Indexing Tests")
    class CompleteIndexingTests {

        @Test
        @DisplayName("should create complete IndexableDocumentDTO with all fields")
        void should_createCompleteIndexableDTO_withAllFields() {
            // Arrange
            PagesPdfEntity page1 = createPage(1, "Page one text");
            PagesPdfEntity page2 = createPage(2, "Page two text");

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(Arrays.asList(page1, page2));

            // Act
            documentIndexingListener.handleDocumentEnriched(enrichedEvent);

            // Assert
            ArgumentCaptor<IndexableDocumentDTO> captor = ArgumentCaptor.forClass(IndexableDocumentDTO.class);
            verify(searchService).indexDocument(captor.capture());

            IndexableDocumentDTO indexedDoc = captor.getValue();
            assertThat(indexedDoc.getId()).isEqualTo(1L);
            assertThat(indexedDoc.getFileName()).isEqualTo("Test Document Title");
            assertThat(indexedDoc.getContentType()).isEqualTo("application/pdf");
            assertThat(indexedDoc.getTags()).containsExactly("invoice", "business");
            assertThat(indexedDoc.getYear()).isEqualTo(2023);
            assertThat(indexedDoc.getFullText()).isEqualTo("Page one text\nPage two text");
            assertThat(indexedDoc.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should successfully index document after enrichment")
        void should_successfullyIndexDocument_afterEnrichment() {
            // Arrange
            PagesPdfEntity page = createPage(1, "Indexed content");

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(page));

            // Act
            documentIndexingListener.handleDocumentEnriched(enrichedEvent);

            // Assert
            verify(searchService).indexDocument(any(IndexableDocumentDTO.class));
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("should handle document with single page")
        void should_handleDocumentWithSinglePage() {
            // Arrange
            PagesPdfEntity page = createPage(1, "Single page content");

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(page));

            // Act
            documentIndexingListener.handleDocumentEnriched(enrichedEvent);

            // Assert
            ArgumentCaptor<IndexableDocumentDTO> captor = ArgumentCaptor.forClass(IndexableDocumentDTO.class);
            verify(searchService).indexDocument(captor.capture());

            IndexableDocumentDTO indexedDoc = captor.getValue();
            assertThat(indexedDoc.getFullText()).isEqualTo("Single page content");
        }

        @Test
        @DisplayName("should handle very long aggregated text")
        void should_handleVeryLongAggregatedText() {
            // Arrange
            String longText = "a".repeat(10000);
            PagesPdfEntity page1 = createPage(1, longText);
            PagesPdfEntity page2 = createPage(2, longText);

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(Arrays.asList(page1, page2));

            // Act
            documentIndexingListener.handleDocumentEnriched(enrichedEvent);

            // Assert
            ArgumentCaptor<IndexableDocumentDTO> captor = ArgumentCaptor.forClass(IndexableDocumentDTO.class);
            verify(searchService).indexDocument(captor.capture());

            IndexableDocumentDTO indexedDoc = captor.getValue();
            assertThat(indexedDoc.getFullText()).hasSize(20001); // 10000 + "\n" + 10000
        }

        @Test
        @DisplayName("should handle document with many tags")
        void should_handleDocumentWithManyTags() {
            // Arrange
            testDocument.setTags(Arrays.asList("tag1", "tag2", "tag3", "tag4", "tag5"));

            PagesPdfEntity page = createPage(1, "Content");

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(List.of(page));

            // Act
            documentIndexingListener.handleDocumentEnriched(enrichedEvent);

            // Assert
            ArgumentCaptor<IndexableDocumentDTO> captor = ArgumentCaptor.forClass(IndexableDocumentDTO.class);
            verify(searchService).indexDocument(captor.capture());

            IndexableDocumentDTO indexedDoc = captor.getValue();
            assertThat(indexedDoc.getTags()).containsExactly("tag1", "tag2", "tag3", "tag4", "tag5");
        }

        @Test
        @DisplayName("should handle pages with special characters")
        void should_handlePagesWithSpecialCharacters() {
            // Arrange
            PagesPdfEntity page1 = createPage(1, "Text with €, £, ¥");
            PagesPdfEntity page2 = createPage(2, "Unicode: 你好世界");

            when(documentRepository.findById(1L))
                    .thenReturn(Optional.of(testDocument));
            when(pageRepository.findByDocumentIdOrderByPageNumber(1L))
                    .thenReturn(Arrays.asList(page1, page2));

            // Act
            documentIndexingListener.handleDocumentEnriched(enrichedEvent);

            // Assert
            ArgumentCaptor<IndexableDocumentDTO> captor = ArgumentCaptor.forClass(IndexableDocumentDTO.class);
            verify(searchService).indexDocument(captor.capture());

            IndexableDocumentDTO indexedDoc = captor.getValue();
            assertThat(indexedDoc.getFullText()).contains("€", "£", "¥", "你好世界");
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
