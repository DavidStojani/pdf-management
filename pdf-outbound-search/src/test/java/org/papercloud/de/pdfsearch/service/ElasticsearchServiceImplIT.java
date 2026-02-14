package org.papercloud.de.pdfsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.papercloud.de.core.dto.search.IndexableDocumentDTO;
import org.papercloud.de.core.dto.search.SearchRequestDTO;
import org.papercloud.de.core.dto.search.SearchResultDTO;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DisplayName("ElasticsearchServiceImpl Integration Tests")
class ElasticsearchServiceImplIT {

    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
            "docker.elastic.co/elasticsearch/elasticsearch:8.13.0"
    ).withEnv("xpack.security.enabled", "false")
     .withEnv("discovery.type", "single-node");

    private ElasticsearchClient esClient;
    private ElasticsearchServiceImpl searchService;

    @BeforeEach
    void setUp() throws IOException {
        RestClient restClient = RestClient.builder(
                HttpHost.create(elasticsearch.getHttpHostAddress())
        ).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        esClient = new ElasticsearchClient(transport);

        searchService = new ElasticsearchServiceImpl(esClient);
        searchService.createIndexIfNotExists();
    }

    @AfterEach
    void tearDown() throws IOException {
        boolean exists = esClient.indices().exists(e -> e.index("documents")).value();
        if (exists) {
            esClient.indices().delete(d -> d.index("documents"));
        }
    }

    @Test
    @DisplayName("should index and search a document by full text")
    void should_indexAndSearch_byFullText() throws IOException {
        IndexableDocumentDTO doc = IndexableDocumentDTO.builder()
                .id(1L)
                .fileName("Tax Report")
                .contentType("application/pdf")
                .tags(List.of("tax"))
                .year(2024)
                .fullText("This is a tax report with important financial information")
                .username("alice")
                .build();

        searchService.indexDocument(doc);
        esClient.indices().refresh(r -> r.index("documents"));

        SearchRequestDTO request = SearchRequestDTO.builder()
                .query("tax report")
                .username("alice")
                .page(0)
                .size(10)
                .build();

        SearchResultDTO result = searchService.search(request);

        assertThat(result.getTotalHits()).isEqualTo(1);
        assertThat(result.getHits()).hasSize(1);
        assertThat(result.getHits().get(0).getDocumentId()).isEqualTo("1");
    }

    @Test
    @DisplayName("should filter search results by username")
    void should_filterByUsername() throws IOException {
        IndexableDocumentDTO aliceDoc = IndexableDocumentDTO.builder()
                .id(1L).fileName("Alice Doc").contentType("application/pdf")
                .tags(List.of()).year(2024)
                .fullText("Invoice for consulting services rendered in January")
                .username("alice").build();

        IndexableDocumentDTO bobDoc = IndexableDocumentDTO.builder()
                .id(2L).fileName("Bob Doc").contentType("application/pdf")
                .tags(List.of()).year(2024)
                .fullText("Invoice for consulting services rendered in February")
                .username("bob").build();

        searchService.indexDocument(aliceDoc);
        searchService.indexDocument(bobDoc);
        esClient.indices().refresh(r -> r.index("documents"));

        SearchRequestDTO request = SearchRequestDTO.builder()
                .query("invoice consulting")
                .username("alice")
                .page(0).size(10).build();

        SearchResultDTO result = searchService.search(request);

        assertThat(result.getTotalHits()).isEqualTo(1);
        assertThat(result.getHits().get(0).getDocumentId()).isEqualTo("1");
    }

    @Test
    @DisplayName("should rank documents by BM25 relevance")
    void should_rankByBM25Relevance() throws IOException {
        IndexableDocumentDTO lessRelevant = IndexableDocumentDTO.builder()
                .id(1L).fileName("Brief mention").contentType("application/pdf")
                .tags(List.of()).year(2024)
                .fullText("The contract mentions insurance once.")
                .username("alice").build();

        IndexableDocumentDTO moreRelevant = IndexableDocumentDTO.builder()
                .id(2L).fileName("Insurance Policy").contentType("application/pdf")
                .tags(List.of()).year(2024)
                .fullText("Insurance policy details. This insurance covers health insurance and life insurance premiums for insurance holders.")
                .username("alice").build();

        searchService.indexDocument(lessRelevant);
        searchService.indexDocument(moreRelevant);
        esClient.indices().refresh(r -> r.index("documents"));

        SearchRequestDTO request = SearchRequestDTO.builder()
                .query("insurance")
                .username("alice")
                .page(0).size(10).build();

        SearchResultDTO result = searchService.search(request);

        assertThat(result.getTotalHits()).isEqualTo(2);
        // More relevant doc (more occurrences of "insurance") should come first
        assertThat(result.getHits().get(0).getDocumentId()).isEqualTo("2");
        assertThat(result.getHits().get(1).getDocumentId()).isEqualTo("1");
    }

    @Test
    @DisplayName("should return empty results when no documents match")
    void should_returnEmpty_when_noMatch() throws IOException {
        IndexableDocumentDTO doc = IndexableDocumentDTO.builder()
                .id(1L).fileName("Report").contentType("application/pdf")
                .tags(List.of()).year(2024)
                .fullText("Quarterly financial report")
                .username("alice").build();

        searchService.indexDocument(doc);
        esClient.indices().refresh(r -> r.index("documents"));

        SearchRequestDTO request = SearchRequestDTO.builder()
                .query("nonexistent term xyz")
                .username("alice")
                .page(0).size(10).build();

        SearchResultDTO result = searchService.search(request);

        assertThat(result.getTotalHits()).isEqualTo(0);
        assertThat(result.getHits()).isEmpty();
    }

    @Test
    @DisplayName("should delete document from index")
    void should_deleteDocument() throws IOException {
        IndexableDocumentDTO doc = IndexableDocumentDTO.builder()
                .id(1L).fileName("To Delete").contentType("application/pdf")
                .tags(List.of()).year(2024)
                .fullText("Document to be deleted")
                .username("alice").build();

        searchService.indexDocument(doc);
        esClient.indices().refresh(r -> r.index("documents"));

        searchService.deleteDocument(1L);
        esClient.indices().refresh(r -> r.index("documents"));

        SearchRequestDTO request = SearchRequestDTO.builder()
                .query("deleted")
                .username("alice")
                .page(0).size(10).build();

        SearchResultDTO result = searchService.search(request);

        assertThat(result.getTotalHits()).isEqualTo(0);
    }

    @Test
    @DisplayName("should overwrite document on re-index (idempotent)")
    void should_overwriteOnReindex() throws IOException {
        IndexableDocumentDTO original = IndexableDocumentDTO.builder()
                .id(1L).fileName("Original Title").contentType("application/pdf")
                .tags(List.of()).year(2024)
                .fullText("Original content")
                .username("alice").build();

        searchService.indexDocument(original);

        IndexableDocumentDTO updated = IndexableDocumentDTO.builder()
                .id(1L).fileName("Updated Title").contentType("application/pdf")
                .tags(List.of("updated")).year(2025)
                .fullText("Updated content with new information")
                .username("alice").build();

        searchService.indexDocument(updated);
        esClient.indices().refresh(r -> r.index("documents"));

        SearchRequestDTO request = SearchRequestDTO.builder()
                .query("updated")
                .username("alice")
                .page(0).size(10).build();

        SearchResultDTO result = searchService.search(request);

        assertThat(result.getTotalHits()).isEqualTo(1);
        assertThat(result.getHits().get(0).getDocumentName()).isEqualTo("Updated Title");
    }
}
