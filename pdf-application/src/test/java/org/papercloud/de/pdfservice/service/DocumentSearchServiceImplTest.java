package org.papercloud.de.pdfservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.core.dto.document.DocumentListItemDTO;
import org.papercloud.de.core.dto.search.SearchHitDTO;
import org.papercloud.de.core.dto.search.SearchRequestDTO;
import org.papercloud.de.core.dto.search.SearchResultDTO;
import org.papercloud.de.core.ports.outbound.SearchService;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.FavouriteRepository;
import org.papercloud.de.pdfservice.mapper.DocumentServiceMapper;
import org.papercloud.de.pdfservice.mapper.DocumentServiceMapperImpl;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentSearchServiceImpl Tests")
class DocumentSearchServiceImplTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private FavouriteRepository favouriteRepository;
    @Mock private SearchService searchService;
    @Spy private DocumentServiceMapper documentMapper = new DocumentServiceMapperImpl();

    @InjectMocks
    private DocumentSearchServiceImpl searchService2;

    private UserEntity testUser;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder().id(1L).username("testuser").build();
    }

    @Nested
    @DisplayName("SearchDocuments Tests")
    class SearchDocumentsTests {

        @Test
        @DisplayName("should use UPLOAD_# title when document has no enriched title")
        void should_useUploadTitle_when_noEnrichedTitle() {
            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(42L).filename("report.pdf").title(null).owner(testUser).build();
            when(documentRepository.findByOwnerUsername("testuser")).thenReturn(List.of(doc));
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            List<DocumentListItemDTO> result = searchService2.searchDocuments("testuser", null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("UPLOAD_#42");
        }

        @Test
        @DisplayName("should use enriched title when available")
        void should_useEnrichedTitle_when_available() {
            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L).filename("report.pdf").title("Tax Report 2024").owner(testUser).build();
            when(documentRepository.findByOwnerUsername("testuser")).thenReturn(List.of(doc));
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            List<DocumentListItemDTO> result = searchService2.searchDocuments("testuser", null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Tax Report 2024");
        }

        @Test
        @DisplayName("should populate page count from pages list")
        void should_populatePageCount() {
            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L).filename("report.pdf").owner(testUser)
                    .pages(List.of(
                            PagesPdfEntity.builder().id(1L).pageNumber(1).build(),
                            PagesPdfEntity.builder().id(2L).pageNumber(2).build(),
                            PagesPdfEntity.builder().id(3L).pageNumber(3).build()
                    ))
                    .build();
            when(documentRepository.findByOwnerUsername("testuser")).thenReturn(List.of(doc));
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            List<DocumentListItemDTO> result = searchService2.searchDocuments("testuser", null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPageCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("should return zero page count when pages is null")
        void should_returnZeroPageCount_when_pagesNull() {
            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L).filename("report.pdf").owner(testUser).pages(null).build();
            when(documentRepository.findByOwnerUsername("testuser")).thenReturn(List.of(doc));
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            List<DocumentListItemDTO> result = searchService2.searchDocuments("testuser", null);

            assertThat(result.get(0).getPageCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("should filter by display title in fallback mode")
        void should_filterByDisplayTitle() {
            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenThrow(new RuntimeException("ES unavailable"));

            DocumentPdfEntity doc1 = DocumentPdfEntity.builder()
                    .id(42L).filename("a.pdf").title(null).owner(testUser).build();
            DocumentPdfEntity doc2 = DocumentPdfEntity.builder()
                    .id(99L).filename("b.pdf").title(null).owner(testUser).build();
            when(documentRepository.findByOwnerUsername("testuser")).thenReturn(List.of(doc1, doc2));
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            List<DocumentListItemDTO> result = searchService2.searchDocuments("testuser", "upload_#42");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("should filter by filename in fallback mode")
        void should_filterByFilename() {
            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenThrow(new RuntimeException("ES unavailable"));

            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L).filename("invoice-2024.pdf").title(null).owner(testUser).build();
            when(documentRepository.findByOwnerUsername("testuser")).thenReturn(List.of(doc));
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            List<DocumentListItemDTO> result = searchService2.searchDocuments("testuser", "invoice");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should delegate to Elasticsearch when query is present")
        void should_delegateToElasticsearch_when_queryPresent() {
            SearchResultDTO esResult = SearchResultDTO.builder()
                    .hits(List.of(
                            SearchHitDTO.builder().documentId("2").documentName("Doc B").build(),
                            SearchHitDTO.builder().documentId("1").documentName("Doc A").build()
                    ))
                    .totalHits(2).totalPages(1).currentPage(0).build();

            when(searchService.search(any(SearchRequestDTO.class))).thenReturn(esResult);
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of(2L));

            DocumentPdfEntity doc1 = DocumentPdfEntity.builder()
                    .id(1L).filename("a.pdf").title("Doc A").owner(testUser).build();
            DocumentPdfEntity doc2 = DocumentPdfEntity.builder()
                    .id(2L).filename("b.pdf").title("Doc B").owner(testUser).build();
            when(documentRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(doc1, doc2));

            List<DocumentListItemDTO> result = searchService2.searchDocuments("testuser", "some query");

            assertThat(result).hasSize(2);
            // Preserves ES relevance order: doc2 first, doc1 second
            assertThat(result.get(0).getId()).isEqualTo(2L);
            assertThat(result.get(0).getIsFavourite()).isTrue();
            assertThat(result.get(1).getId()).isEqualTo(1L);
            assertThat(result.get(1).getIsFavourite()).isFalse();

            verify(searchService).search(any(SearchRequestDTO.class));
        }

        @Test
        @DisplayName("should fall back to in-memory search when Elasticsearch fails")
        void should_fallBackToInMemory_when_elasticsearchFails() {
            when(searchService.search(any(SearchRequestDTO.class)))
                    .thenThrow(new RuntimeException("ES connection failed"));
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            DocumentPdfEntity doc = DocumentPdfEntity.builder()
                    .id(1L).filename("invoice-2024.pdf").title("Invoice 2024").owner(testUser).build();
            when(documentRepository.findByOwnerUsername("testuser")).thenReturn(List.of(doc));

            List<DocumentListItemDTO> result = searchService2.searchDocuments("testuser", "invoice");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Invoice 2024");
        }

        @Test
        @DisplayName("should return empty list when Elasticsearch returns no hits")
        void should_returnEmptyList_when_noElasticsearchHits() {
            SearchResultDTO emptyResult = SearchResultDTO.builder()
                    .hits(List.of()).totalHits(0).totalPages(0).currentPage(0).build();
            when(searchService.search(any(SearchRequestDTO.class))).thenReturn(emptyResult);
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            List<DocumentListItemDTO> result = searchService2.searchDocuments("testuser", "nonexistent");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should skip documents not found in DB after ES search")
        void should_skipMissingDocuments_afterElasticsearchSearch() {
            SearchResultDTO esResult = SearchResultDTO.builder()
                    .hits(List.of(
                            SearchHitDTO.builder().documentId("1").documentName("Doc A").build(),
                            SearchHitDTO.builder().documentId("999").documentName("Deleted").build()
                    ))
                    .totalHits(2).totalPages(1).currentPage(0).build();

            when(searchService.search(any(SearchRequestDTO.class))).thenReturn(esResult);
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of());

            DocumentPdfEntity doc1 = DocumentPdfEntity.builder()
                    .id(1L).filename("a.pdf").title("Doc A").owner(testUser).build();
            when(documentRepository.findAllById(List.of(1L, 999L))).thenReturn(List.of(doc1));

            List<DocumentListItemDTO> result = searchService2.searchDocuments("testuser", "query");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should mark favourited documents in search results")
        void should_markFavouritedDocuments() {
            DocumentPdfEntity doc1 = DocumentPdfEntity.builder()
                    .id(1L).filename("a.pdf").owner(testUser).build();
            DocumentPdfEntity doc2 = DocumentPdfEntity.builder()
                    .id(2L).filename("b.pdf").owner(testUser).build();
            when(documentRepository.findByOwnerUsername("testuser")).thenReturn(List.of(doc1, doc2));
            when(favouriteRepository.findFavouriteDocumentIdsByUsername("testuser")).thenReturn(Set.of(1L));

            List<DocumentListItemDTO> result = searchService2.searchDocuments("testuser", null);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getIsFavourite()).isTrue();
            assertThat(result.get(1).getIsFavourite()).isFalse();
        }
    }
}
