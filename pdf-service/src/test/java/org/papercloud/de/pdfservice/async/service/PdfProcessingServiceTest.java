package org.papercloud.de.pdfservice.async.service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfdatabase.repository.UserRepository;
import org.papercloud.de.pdfservice.search.DocumentServiceImpl;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PdfProcessingServiceTest {

    @InjectMocks
    private DocumentServiceImpl documentService;
    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private DocumentRepository docRepo;
    @Mock
    private UserRepository userRepository;


}