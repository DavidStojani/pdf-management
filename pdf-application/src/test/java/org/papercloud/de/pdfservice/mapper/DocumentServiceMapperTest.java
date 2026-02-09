package org.papercloud.de.pdfservice.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.papercloud.de.core.dto.document.DocumentDTO;
import org.papercloud.de.core.dto.document.DocumentDownloadDTO;
import org.papercloud.de.core.dto.document.PageDTO;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.PagesPdfEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DocumentServiceMapper using real MapStruct-generated implementation.
 * Tests mappings between JPA entities and DTOs used by the service layer.
 */
@DisplayName("DocumentServiceMapper Integration Tests")
class DocumentServiceMapperTest {

    private DocumentServiceMapper mapper;

    @BeforeEach
    void setUp() {
        // Instantiate the generated mapper implementation directly
        mapper = new DocumentServiceMapperImpl();
    }

    @Nested
    @DisplayName("toDocumentDTO Tests")
    class ToDocumentDTOTests {

        @Test
        @DisplayName("should map entity to DTO with all fields")
        void toDocumentDTO_fullEntity_mapsAllFields() {
            // Arrange
            PagesPdfEntity page1 = PagesPdfEntity.builder()
                    .id(1L)
                    .pageNumber(1)
                    .pageText("Page 1 content")
                    .build();

            PagesPdfEntity page2 = PagesPdfEntity.builder()
                    .id(2L)
                    .pageNumber(2)
                    .pageText("Page 2 content")
                    .build();

            DocumentPdfEntity entity = DocumentPdfEntity.builder()
                    .id(100L)
                    .filename("test-document.pdf")
                    .size(2048L)
                    .uploadedAt(LocalDateTime.of(2026, 2, 9, 10, 30))
                    .pdfContent("PDF content bytes".getBytes())
                    .pages(Arrays.asList(page1, page2))
                    .build();

            // Act
            DocumentDTO dto = mapper.toDocumentDTO(entity);

            // Assert
            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo(100L);
            assertThat(dto.getFileName()).isEqualTo("test-document.pdf");
            assertThat(dto.getSize()).isEqualTo(2048L);
            assertThat(dto.getUploadedAt()).isEqualTo(LocalDateTime.of(2026, 2, 9, 10, 30));
            assertThat(dto.getPdfContent()).isEqualTo("PDF content bytes".getBytes());
            assertThat(dto.getPages()).hasSize(2);
            assertThat(dto.getPages().get(0).getPageNumber()).isEqualTo(1);
            assertThat(dto.getPages().get(0).getExtractedText()).isEqualTo("Page 1 content");
            assertThat(dto.getPages().get(1).getPageNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("should handle null entity")
        void toDocumentDTO_nullEntity_returnsNull() {
            // Act
            DocumentDTO dto = mapper.toDocumentDTO(null);

            // Assert
            assertThat(dto).isNull();
        }

        @Test
        @DisplayName("should handle entity with null fields")
        void toDocumentDTO_nullFields_handlesGracefully() {
            // Arrange
            DocumentPdfEntity entity = DocumentPdfEntity.builder()
                    .id(1L)
                    .filename(null)
                    .size(null)
                    .uploadedAt(null)
                    .pdfContent(null)
                    .pages(null)
                    .build();

            // Act
            DocumentDTO dto = mapper.toDocumentDTO(entity);

            // Assert
            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getFileName()).isNull();
            assertThat(dto.getSize()).isZero();
            assertThat(dto.getUploadedAt()).isNull();
            assertThat(dto.getPdfContent()).isNull();
            assertThat(dto.getPages()).isNull();
        }

        @Test
        @DisplayName("should handle entity with empty pages list")
        void toDocumentDTO_emptyPages_mapsToEmptyList() {
            // Arrange
            DocumentPdfEntity entity = DocumentPdfEntity.builder()
                    .id(2L)
                    .filename("empty-doc.pdf")
                    .pages(Collections.emptyList())
                    .build();

            // Act
            DocumentDTO dto = mapper.toDocumentDTO(entity);

            // Assert
            assertThat(dto).isNotNull();
            assertThat(dto.getPages()).isEmpty();
        }

        @Test
        @DisplayName("should correctly map filename to fileName")
        void toDocumentDTO_filename_mapsToFileName() {
            // Arrange
            DocumentPdfEntity entity = DocumentPdfEntity.builder()
                    .id(3L)
                    .filename("important-file.pdf")
                    .build();

            // Act
            DocumentDTO dto = mapper.toDocumentDTO(entity);

            // Assert
            assertThat(dto.getFileName()).isEqualTo("important-file.pdf");
        }

        @Test
        @DisplayName("should handle entity with single page")
        void toDocumentDTO_singlePage_mapsSinglePage() {
            // Arrange
            PagesPdfEntity page = PagesPdfEntity.builder()
                    .id(10L)
                    .pageNumber(1)
                    .pageText("Only page content")
                    .build();

            DocumentPdfEntity entity = DocumentPdfEntity.builder()
                    .id(5L)
                    .filename("single-page.pdf")
                    .pages(List.of(page))
                    .build();

            // Act
            DocumentDTO dto = mapper.toDocumentDTO(entity);

            // Assert
            assertThat(dto.getPages()).hasSize(1);
            assertThat(dto.getPages().get(0).getPageNumber()).isEqualTo(1);
            assertThat(dto.getPages().get(0).getExtractedText()).isEqualTo("Only page content");
        }

        @Test
        @DisplayName("should handle entity with large PDF content")
        void toDocumentDTO_largePdfContent_copiesArrayCorrectly() {
            // Arrange
            byte[] largeContent = new byte[10000];
            Arrays.fill(largeContent, (byte) 0xFF);

            DocumentPdfEntity entity = DocumentPdfEntity.builder()
                    .id(6L)
                    .filename("large.pdf")
                    .pdfContent(largeContent)
                    .size((long) largeContent.length)
                    .build();

            // Act
            DocumentDTO dto = mapper.toDocumentDTO(entity);

            // Assert
            assertThat(dto.getPdfContent()).isNotNull();
            assertThat(dto.getPdfContent()).hasSize(10000);
            assertThat(dto.getPdfContent()).isEqualTo(largeContent);
            // Verify it's a copy, not the same reference
            assertThat(dto.getPdfContent()).isNotSameAs(largeContent);
        }
    }

    @Nested
    @DisplayName("toDownloadDTO Tests")
    class ToDownloadDTOTests {

        @Test
        @DisplayName("should map entity to download DTO with all fields")
        void toDownloadDTO_fullEntity_mapsAllFields() {
            // Arrange
            DocumentPdfEntity entity = DocumentPdfEntity.builder()
                    .id(200L)
                    .filename("download-test.pdf")
                    .size(5120L)
                    .pdfContent("Download content".getBytes())
                    .contentType("application/pdf")
                    .build();

            // Act
            DocumentDownloadDTO dto = mapper.toDownloadDTO(entity);

            // Assert
            assertThat(dto).isNotNull();
            assertThat(dto.getId()).isEqualTo(200L);
            assertThat(dto.getFileName()).isEqualTo("download-test.pdf");
            assertThat(dto.getSize()).isEqualTo(5120L);
            assertThat(dto.getContent()).isEqualTo("Download content".getBytes());
            assertThat(dto.getContentType()).isEqualTo("application/pdf");
        }

        @Test
        @DisplayName("should handle null entity")
        void toDownloadDTO_nullEntity_returnsNull() {
            // Act
            DocumentDownloadDTO dto = mapper.toDownloadDTO(null);

            // Assert
            assertThat(dto).isNull();
        }

        @Test
        @DisplayName("should map pdfContent to content field")
        void toDownloadDTO_pdfContent_mapsToContent() {
            // Arrange
            byte[] contentBytes = "Test PDF bytes".getBytes();
            DocumentPdfEntity entity = DocumentPdfEntity.builder()
                    .id(1L)
                    .filename("test.pdf")
                    .pdfContent(contentBytes)
                    .build();

            // Act
            DocumentDownloadDTO dto = mapper.toDownloadDTO(entity);

            // Assert
            assertThat(dto.getContent()).isEqualTo(contentBytes);
            assertThat(dto.getContent()).isNotSameAs(contentBytes);
        }

        @Test
        @DisplayName("should handle null pdfContent")
        void toDownloadDTO_nullPdfContent_mapsToNullContent() {
            // Arrange
            DocumentPdfEntity entity = DocumentPdfEntity.builder()
                    .id(1L)
                    .filename("no-content.pdf")
                    .pdfContent(null)
                    .build();

            // Act
            DocumentDownloadDTO dto = mapper.toDownloadDTO(entity);

            // Assert
            assertThat(dto.getContent()).isNull();
        }

        @Test
        @DisplayName("should handle different content types")
        void toDownloadDTO_differentContentTypes_mapsCorrectly() {
            // Arrange
            DocumentPdfEntity entity = DocumentPdfEntity.builder()
                    .id(3L)
                    .filename("document.pdf")
                    .contentType("application/octet-stream")
                    .build();

            // Act
            DocumentDownloadDTO dto = mapper.toDownloadDTO(entity);

            // Assert
            assertThat(dto.getContentType()).isEqualTo("application/octet-stream");
        }

        @Test
        @DisplayName("should handle entity with null size")
        void toDownloadDTO_nullSize_mapsToZero() {
            // Arrange
            DocumentPdfEntity entity = DocumentPdfEntity.builder()
                    .id(4L)
                    .filename("test.pdf")
                    .size(null)
                    .build();

            // Act
            DocumentDownloadDTO dto = mapper.toDownloadDTO(entity);

            // Assert
            assertThat(dto.getSize()).isZero();
        }

        @Test
        @DisplayName("should handle entity with zero size")
        void toDownloadDTO_zeroSize_mapsToZero() {
            // Arrange
            DocumentPdfEntity entity = DocumentPdfEntity.builder()
                    .id(5L)
                    .filename("empty.pdf")
                    .size(0L)
                    .build();

            // Act
            DocumentDownloadDTO dto = mapper.toDownloadDTO(entity);

            // Assert
            assertThat(dto.getSize()).isZero();
        }
    }

    @Nested
    @DisplayName("toPageDTO Tests")
    class ToPageDTOTests {

        @Test
        @DisplayName("should map page entity to page DTO")
        void toPageDTO_fullEntity_mapsAllFields() {
            // Arrange
            PagesPdfEntity entity = PagesPdfEntity.builder()
                    .id(50L)
                    .pageNumber(5)
                    .pageText("This is page 5 content")
                    .build();

            // Act
            PageDTO dto = mapper.toPageDTO(entity);

            // Assert
            assertThat(dto).isNotNull();
            assertThat(dto.getPageNumber()).isEqualTo(5);
            assertThat(dto.getExtractedText()).isEqualTo("This is page 5 content");
        }

        @Test
        @DisplayName("should handle null entity")
        void toPageDTO_nullEntity_returnsNull() {
            // Act
            PageDTO dto = mapper.toPageDTO(null);

            // Assert
            assertThat(dto).isNull();
        }

        @Test
        @DisplayName("should map pageText to extractedText")
        void toPageDTO_pageText_mapsToExtractedText() {
            // Arrange
            PagesPdfEntity entity = PagesPdfEntity.builder()
                    .id(1L)
                    .pageNumber(1)
                    .pageText("Extracted text from OCR")
                    .build();

            // Act
            PageDTO dto = mapper.toPageDTO(entity);

            // Assert
            assertThat(dto.getExtractedText()).isEqualTo("Extracted text from OCR");
        }

        @Test
        @DisplayName("should handle page with null text")
        void toPageDTO_nullPageText_mapsToNull() {
            // Arrange
            PagesPdfEntity entity = PagesPdfEntity.builder()
                    .id(2L)
                    .pageNumber(2)
                    .pageText(null)
                    .build();

            // Act
            PageDTO dto = mapper.toPageDTO(entity);

            // Assert
            assertThat(dto.getExtractedText()).isNull();
        }

        @Test
        @DisplayName("should handle page with empty text")
        void toPageDTO_emptyPageText_mapsToEmptyString() {
            // Arrange
            PagesPdfEntity entity = PagesPdfEntity.builder()
                    .id(3L)
                    .pageNumber(3)
                    .pageText("")
                    .build();

            // Act
            PageDTO dto = mapper.toPageDTO(entity);

            // Assert
            assertThat(dto.getExtractedText()).isEmpty();
        }

        @Test
        @DisplayName("should handle page number zero")
        void toPageDTO_pageNumberZero_mapsCorrectly() {
            // Arrange
            PagesPdfEntity entity = PagesPdfEntity.builder()
                    .id(4L)
                    .pageNumber(0)
                    .pageText("Cover page")
                    .build();

            // Act
            PageDTO dto = mapper.toPageDTO(entity);

            // Assert
            assertThat(dto.getPageNumber()).isZero();
        }

        @Test
        @DisplayName("should handle page with long text content")
        void toPageDTO_longText_mapsCorrectly() {
            // Arrange
            String longText = "A".repeat(10000);
            PagesPdfEntity entity = PagesPdfEntity.builder()
                    .id(5L)
                    .pageNumber(1)
                    .pageText(longText)
                    .build();

            // Act
            PageDTO dto = mapper.toPageDTO(entity);

            // Assert
            assertThat(dto.getExtractedText()).hasSize(10000);
            assertThat(dto.getExtractedText()).isEqualTo(longText);
        }
    }

    @Nested
    @DisplayName("toPageDTOList Tests")
    class ToPageDTOListTests {

        @Test
        @DisplayName("should map list of page entities to list of page DTOs")
        void toPageDTOList_multiplePages_mapsAllPages() {
            // Arrange
            List<PagesPdfEntity> entities = Arrays.asList(
                    PagesPdfEntity.builder().id(1L).pageNumber(1).pageText("Page 1").build(),
                    PagesPdfEntity.builder().id(2L).pageNumber(2).pageText("Page 2").build(),
                    PagesPdfEntity.builder().id(3L).pageNumber(3).pageText("Page 3").build()
            );

            // Act
            List<PageDTO> dtos = mapper.toPageDTOList(entities);

            // Assert
            assertThat(dtos).hasSize(3);
            assertThat(dtos.get(0).getPageNumber()).isEqualTo(1);
            assertThat(dtos.get(0).getExtractedText()).isEqualTo("Page 1");
            assertThat(dtos.get(1).getPageNumber()).isEqualTo(2);
            assertThat(dtos.get(2).getPageNumber()).isEqualTo(3);
        }

        @Test
        @DisplayName("should handle null list")
        void toPageDTOList_nullList_returnsNull() {
            // Act
            List<PageDTO> dtos = mapper.toPageDTOList(null);

            // Assert
            assertThat(dtos).isNull();
        }

        @Test
        @DisplayName("should handle empty list")
        void toPageDTOList_emptyList_returnsEmptyList() {
            // Arrange
            List<PagesPdfEntity> entities = Collections.emptyList();

            // Act
            List<PageDTO> dtos = mapper.toPageDTOList(entities);

            // Assert
            assertThat(dtos).isEmpty();
        }

        @Test
        @DisplayName("should handle list with single page")
        void toPageDTOList_singlePage_returnsSingleElementList() {
            // Arrange
            List<PagesPdfEntity> entities = List.of(
                    PagesPdfEntity.builder().id(1L).pageNumber(1).pageText("Only page").build()
            );

            // Act
            List<PageDTO> dtos = mapper.toPageDTOList(entities);

            // Assert
            assertThat(dtos).hasSize(1);
            assertThat(dtos.get(0).getPageNumber()).isEqualTo(1);
        }

        @Test
        @DisplayName("should handle list with null elements")
        void toPageDTOList_listWithNullElements_mapsNullElements() {
            // Arrange
            List<PagesPdfEntity> entities = new ArrayList<>();
            entities.add(PagesPdfEntity.builder().id(1L).pageNumber(1).pageText("Page 1").build());
            entities.add(null);
            entities.add(PagesPdfEntity.builder().id(3L).pageNumber(3).pageText("Page 3").build());

            // Act
            List<PageDTO> dtos = mapper.toPageDTOList(entities);

            // Assert
            assertThat(dtos).hasSize(3);
            assertThat(dtos.get(0)).isNotNull();
            assertThat(dtos.get(1)).isNull();
            assertThat(dtos.get(2)).isNotNull();
        }

        @Test
        @DisplayName("should maintain page order in list")
        void toPageDTOList_orderedPages_maintainsOrder() {
            // Arrange
            List<PagesPdfEntity> entities = Arrays.asList(
                    PagesPdfEntity.builder().id(10L).pageNumber(10).pageText("Page 10").build(),
                    PagesPdfEntity.builder().id(5L).pageNumber(5).pageText("Page 5").build(),
                    PagesPdfEntity.builder().id(1L).pageNumber(1).pageText("Page 1").build()
            );

            // Act
            List<PageDTO> dtos = mapper.toPageDTOList(entities);

            // Assert
            assertThat(dtos).hasSize(3);
            assertThat(dtos.get(0).getPageNumber()).isEqualTo(10);
            assertThat(dtos.get(1).getPageNumber()).isEqualTo(5);
            assertThat(dtos.get(2).getPageNumber()).isEqualTo(1);
        }
    }
}
