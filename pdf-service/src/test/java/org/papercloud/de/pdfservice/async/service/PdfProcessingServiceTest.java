package org.papercloud.de.pdfservice.async.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.common.dto.document.DocumentUploadDTO;
import org.papercloud.de.common.events.EnrichmentEvent;
import org.papercloud.de.pdfdatabase.entity.DocumentPdfEntity;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfservice.search.DocumentServiceImpl;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfProcessingServiceTest {

    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private DocumentRepository docRepo;
    @InjectMocks
    private DocumentServiceImpl documentService;

    @Test
    void shouldPublishDocumentUploadedEvent() throws Exception {
        DocumentUploadDTO dto = DocumentUploadDTO.builder().build();
        List<String> pages = List.of("sample text");

        when(docRepo.save(any())).thenReturn(new DocumentPdfEntity());

        documentService.processDocument(dto,"testuser"); // use mocks here

        verify(publisher).publishEvent(any(EnrichmentEvent.class));
    }
}