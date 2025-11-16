package org.papercloud.de.pdfsearch.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.papercloud.de.common.dto.search.IndexableDocumentDTO;
import org.papercloud.de.common.dto.search.SearchRequestDTO;
import org.papercloud.de.common.dto.search.SearchResultDTO;
import org.papercloud.de.common.events.payload.IndexDocumentPayload;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ElasticsearchServiceIntegrationTest {

    @Container
    static final ElasticsearchContainer elastic = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false");

    private RestClient restClient;
    private ElasticsearchClient client;
    private ElasticsearchServiceImpl service;

    @BeforeEach
    void setUp() throws IOException {
        restClient = RestClient.builder(HttpHost.create(elastic.getHttpHostAddress())).build();
        client = new ElasticsearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
        service = new ElasticsearchServiceImpl(client);

        try {
            client.indices().delete(d -> d.index("documents"));
        } catch (ElasticsearchException ignored) {
            // Index might not exist yet
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        restClient.close();
    }

    @Test
    @DisplayName("search should honor username and query filters")
    void search_shouldFilterByUser() throws IOException {
        indexFixture(1L, "alice", "Quarterly report on revenue");
        indexFixture(2L, "bob", "Unrelated memo");

        SearchRequestDTO request = SearchRequestDTO.builder()
                .query("report")
                .username("alice")
                .page(0)
                .size(5)
                .build();

        SearchResultDTO results = service.search(request);

        assertThat(results.getHits()).hasSize(1);
        assertThat(results.getHits().getFirst().getDocumentId()).isEqualTo("1");
        assertThat(results.getTotalHits()).isEqualTo(1);
    }

    @Test
    @DisplayName("indexDocument should persist payload into Elasticsearch")
    void indexDocument_shouldStoreDocument() throws IOException {
        IndexDocumentPayload payload = new IndexDocumentPayload(
                10L,
                "doc-10.pdf",
                "application/pdf",
                List.of("tag-a", "tag-b"),
                2024,
                "content body"
        );

        service.indexDocument(payload);

        GetResponse<IndexableDocumentDTO> response = client.get(g -> g.index("documents").id("10"), IndexableDocumentDTO.class);

        assertThat(response.found()).isTrue();
        IndexableDocumentDTO source = response.source();
        assertThat(source.getFileName()).isEqualTo("doc-10.pdf");
        assertThat(source.getTags()).containsExactlyInAnyOrder("tag-a", "tag-b");
    }

    private void indexFixture(long id, String username, String text) throws IOException {
        String json = """
                {
                  \"id\": %d,
                  \"fileName\": \"doc-%d.pdf\",
                  \"contentType\": \"application/pdf\",
                  \"tags\": [\"finance\"],
                  \"year\": 2024,
                  \"fullText\": \"%s\",
                  \"username\": \"%s\"
                }
                """.formatted(id, id, text, username);

        client.index(IndexRequest.of(i -> i
                .index("documents")
                .id(String.valueOf(id))
                .withJson(new StringReader(json))));
    }
}
