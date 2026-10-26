package com.navan.commerce;

import com.fasterxml.jackson.databind.JsonNode;
import com.navan.commerce.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spec test #6: two OVERLAPPING POSTs with the same external_id must leave exactly one row.
 * The partial unique index is the only guard — this race test proves it over real HTTP.
 */
class ConcurrentCreateIntegrationTest extends AbstractIntegrationTest {

    @Test
    void overlappingPostsWithSameExternalIdCreateExactlyOneRow() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        Runnable post = () -> {
            try {
                ready.countDown();
                go.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        };

        Future<ResponseEntity<JsonNode>> first = pool.submit(() -> {
            post.run();
            return postTransaction(SEEDS.get("flight"));
        });
        Future<ResponseEntity<JsonNode>> second = pool.submit(() -> {
            post.run();
            return postTransaction(SEEDS.get("flight"));
        });

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        go.countDown(); // release both at once

        ResponseEntity<JsonNode> r1 = first.get(30, TimeUnit.SECONDS);
        ResponseEntity<JsonNode> r2 = second.get(30, TimeUnit.SECONDS);
        pool.shutdownNow();

        List<Integer> statuses = List.of(r1.getStatusCode().value(), r2.getStatusCode().value());
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);

        JsonNode created = r1.getStatusCode().value() == 201 ? r1.getBody() : r2.getBody();
        JsonNode rejected = r1.getStatusCode().value() == 409 ? r1.getBody() : r2.getBody();
        assertThat(rejected.get("code").asText()).isEqualTo("DUPLICATE_TRANSACTION");
        assertThat(rejected.get("existing_id").asText()).isEqualTo(created.get("id").asText());

        // exactly one stored row
        assertThat(countByExternalId("flight-TXL-LHR-2026-03-11")).isEqualTo(1);
    }
}
