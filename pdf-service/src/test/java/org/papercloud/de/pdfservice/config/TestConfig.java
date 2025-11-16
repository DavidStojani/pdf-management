package org.papercloud.de.pdfservice.config;

import jakarta.persistence.EntityManagerFactory;
import org.mockito.Mockito;
import org.papercloud.de.common.util.DocumentEnrichmentService;
import org.papercloud.de.common.util.OcrTextCleaningService;
import org.papercloud.de.pdfdatabase.repository.DocumentRepository;
import org.papercloud.de.pdfservice.processor.DocumentEnrichmentProcessor;
import org.papercloud.de.pdfservice.processor.DocumentEnrichmentProcessorImpl;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Configuration
@SpringBootApplication
@EntityScan("org/papercloud/de/pdfdatabase/entity") // Adjust to your entity package
@EnableJpaRepositories("org/papercloud/de/pdfdatabase/repository") // Adjust to your repo package
@ComponentScan(basePackages = {
        "org/papercloud/de/pdfservice/config",
        "org/papercloud/de/pdfservice/processor"// Adjust to your config package
})
public class TestConfig {

    @Bean
    @Primary
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    // Mock external dependencies that aren't part of your module
    @Bean
    @Primary
    public DocumentEnrichmentService documentEnrichmentService() {
        return Mockito.mock(DocumentEnrichmentService.class);
    }

    @Bean
    @Primary
    public OcrTextCleaningService ocrTextCleaningService() {
        return Mockito.mock(OcrTextCleaningService.class);
    }
    @Bean
    public DocumentEnrichmentProcessor documentEnrichmentProcessor(
            DocumentEnrichmentService enrichmentService,
            OcrTextCleaningService cleaningService,
            ApplicationEventPublisher eventPublisher,
            DocumentRepository documentRepository,
            TransactionTemplate transactionTemplate
            // Add other dependencies as needed
    ) {
        return new DocumentEnrichmentProcessorImpl(enrichmentService, cleaningService,documentRepository, eventPublisher, transactionTemplate);
    }
}