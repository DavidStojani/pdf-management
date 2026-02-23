package org.papercloud.de.pdfservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.core.domain.Document;
import org.papercloud.de.core.dto.document.DocumentListItemDTO;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.entity.UserDocumentFavouriteEntity;
import org.papercloud.de.pdfdatabase.entity.UserEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.FavouriteRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.papercloud.de.pdfservice.errors.DocumentNotFoundException;
import org.papercloud.de.pdfservice.mapper.DocumentServiceMapper;
import org.papercloud.de.pdfservice.mapper.DocumentServiceMapperImpl;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentFavouriteServiceImpl Tests")
class DocumentFavouriteServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private FavouriteRepository favouriteRepository;
    @Mock private AuditService auditService;
    @Spy private DocumentServiceMapper documentMapper = new DocumentServiceMapperImpl();

    @InjectMocks
    private DocumentFavouriteServiceImpl favouriteService;

    private UserEntity testUser;
    private DocumentPdfEntity testDocument;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder().id(1L).username("testuser").build();

        testDocument = DocumentPdfEntity.builder()
                .id(1L)
                .filename("test.pdf")
                .contentType(MediaType.APPLICATION_PDF_VALUE)
                .pdfContent("test content".getBytes())
                .size(100L)
                .owner(testUser)
                .status(Document.Status.UPLOADED)
                .build();
    }

    @Nested
    @DisplayName("Favourites Tests")
    class FavouritesTests {

        @Test
        @DisplayName("should add favourite idempotently")
        void should_addFavourite_idempotently() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
            when(favouriteRepository.existsByUserIdAndDocumentId(1L, 1L)).thenReturn(true);

            favouriteService.addFavourite(1L, "testuser");

            verify(favouriteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should save new favourite when not already favourited")
        void should_saveNewFavourite() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
            when(favouriteRepository.existsByUserIdAndDocumentId(1L, 1L)).thenReturn(false);

            favouriteService.addFavourite(1L, "testuser");

            ArgumentCaptor<UserDocumentFavouriteEntity> captor =
                    ArgumentCaptor.forClass(UserDocumentFavouriteEntity.class);
            verify(favouriteRepository).save(captor.capture());
            assertThat(captor.getValue().getUser()).isEqualTo(testUser);
            assertThat(captor.getValue().getDocument()).isEqualTo(testDocument);
        }

        @Test
        @DisplayName("should remove favourite")
        void should_removeFavourite() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

            favouriteService.removeFavourite(1L, "testuser");

            verify(favouriteRepository).deleteByUserIdAndDocumentId(1L, 1L);
        }

        @Test
        @DisplayName("should return favourite documents with isFavourite=true")
        void should_returnFavouriteDocs() {
            UserDocumentFavouriteEntity fav = UserDocumentFavouriteEntity.builder()
                    .id(1L).user(testUser).document(testDocument).build();
            when(favouriteRepository.findByUserUsernameWithDocument("testuser")).thenReturn(List.of(fav));

            List<DocumentListItemDTO> result = favouriteService.getFavourites("testuser");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIsFavourite()).isTrue();
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should throw exception when addFavourite with unknown user")
        void should_throwException_when_addFavourite_userNotFound() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> favouriteService.addFavourite(1L, "unknown"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found: unknown");

            verify(favouriteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when addFavourite with unknown document")
        void should_throwException_when_addFavourite_documentNotFound() {
            when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
            when(documentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> favouriteService.addFavourite(999L, "testuser"))
                    .isInstanceOf(DocumentNotFoundException.class)
                    .hasMessageContaining("Document not found with id: 999");

            verify(favouriteRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw exception when removeFavourite with unknown user")
        void should_throwException_when_removeFavourite_userNotFound() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> favouriteService.removeFavourite(1L, "unknown"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found: unknown");

            verify(favouriteRepository, never()).deleteByUserIdAndDocumentId(any(), any());
        }

        @Test
        @DisplayName("should return empty list when no favourites exist")
        void should_returnEmptyList_when_noFavourites() {
            when(favouriteRepository.findByUserUsernameWithDocument("testuser")).thenReturn(List.of());

            List<DocumentListItemDTO> result = favouriteService.getFavourites("testuser");

            assertThat(result).isEmpty();
        }
    }
}
