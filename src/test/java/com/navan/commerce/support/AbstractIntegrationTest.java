package com.navan.commerce.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base for integration tests: full Spring context + REAL HTTP server (RANDOM_PORT) against the
 * real Docker Postgres (`test` database — auto-created by {@link TestDatabaseInitializer},
 * migrated by Flyway, TRUNCATEd before each test). Real SQL, real constraints, real HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestDatabaseInitializer.class)
public abstract class AbstractIntegrationTest {

    private static final String SEED_FILE = "task-b-travel-inventory/fixtures/task-b/seed-payloads.json";

    protected static final ObjectMapper MAPPER = new ObjectMapper();
    protected static JsonNode SEEDS;

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeAll
    static void loadSeeds() throws IOException {
        SEEDS = MAPPER.readTree(Path.of(SEED_FILE).toFile());
    }

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE invoice_lines, transaction_lines, invoices, transactions");
    }

    // ---- HTTP helpers ---------------------------------------------------------

    protected ResponseEntity<JsonNode> postTransaction(JsonNode body) {
        return rest.postForEntity("/transactions", body, JsonNode.class);
    }

    /** POSTs a seed by key, asserts 201, returns the server id. */
    protected String createSeed(String key) {
        ResponseEntity<JsonNode> response = postTransaction(SEEDS.get(key));
        assertThat(response.getStatusCode().value()).as("create %s", key).isEqualTo(201);
        return response.getBody().get("id").asText();
    }

    protected JsonNode getBody(String path) {
        ResponseEntity<JsonNode> response = rest.getForEntity(path, JsonNode.class);
        assertThat(response.getStatusCode().value()).as("GET %s", path).isEqualTo(200);
        return response.getBody();
    }

    protected ResponseEntity<JsonNode> patch(String path, JsonNode body) {
        return rest.exchange(path, HttpMethod.PATCH, new HttpEntity<>(body), JsonNode.class);
    }

    protected ResponseEntity<JsonNode> delete(String path) {
        return rest.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, JsonNode.class);
    }

    protected ResponseEntity<JsonNode> createInvoice(List<String> transactionIds) {
        return rest.postForEntity("/invoices",
                MAPPER.createObjectNode().putPOJO("transaction_ids", transactionIds), JsonNode.class);
    }

    // ---- assertion helpers ----------------------------------------------------

    protected static void assertError(ResponseEntity<JsonNode> response, int status, String code) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getBody().get("code").asText()).isEqualTo(code);
        assertThat(response.getBody().get("error_code").asInt()).isEqualTo(status);
    }

    protected static void assertAmount(JsonNode node, String field, String expected) {
        assertThat(node.get(field).decimalValue()).isEqualByComparingTo(new BigDecimal(expected));
    }

    /** Counts list items matching an external_id — proves "post twice, store once". */
    protected int countByExternalId(String externalId) {
        JsonNode items = getBody("/transactions?limit=100").get("items");
        int count = 0;
        for (JsonNode item : items) {
            if (externalId.equals(item.get("external_id").asText())) {
                count++;
            }
        }
        return count;
    }
}
