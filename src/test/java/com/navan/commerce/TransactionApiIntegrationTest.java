package com.navan.commerce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.navan.commerce.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Transaction API against the real HTTP server + real Postgres.
 * Spec tests #1 (create all types, GET round-trips metadata), #2 (duplicate external_id),
 * #10 (bounded lists) + the 400/404 contract and PATCH/DELETE happy paths.
 */
class TransactionApiIntegrationTest extends AbstractIntegrationTest {

    // ---- Spec test #1: create every type from the seed payloads; GET round-trips the structure

    @Test
    void createsAllFiveSeedsAndGetRoundTripsMetadata() {
        java.util.Map<String, String> ids = new java.util.HashMap<>();
        for (String key : new String[] {"flight", "hotel", "rail", "trip_fee", "agent_call_fee"}) {
            String id = createSeed(key);
            assertThat(id).startsWith("txn_");
            ids.put(key, id);
        }

        JsonNode flight = getBody("/transactions/" + ids.get("flight"));
        assertThat(flight.get("type").asText()).isEqualTo("flight");
        assertThat(flight.at("/metadata/coupons/0/flight_number").asText()).isEqualTo("BA982");
        assertThat(flight.at("/metadata/coupons/0/from").asText()).isEqualTo("TXL");
        assertThat(flight.at("/metadata/origin").asText()).isEqualTo("TXL");
        assertAmount(flight.at("/tax_lines").get(0), "amount", "15.00");
        assertThat(flight.at("/tax_lines").get(0).get("name").asText()).isEqualTo("Airport tax");
        assertThat(flight.at("/fee_lines").get(0).get("name").asText()).isEqualTo("YQ");

        JsonNode hotel = getBody("/transactions/" + ids.get("hotel"));
        assertThat(hotel.at("/metadata/name").asText()).isEqualTo("Hotel Mitte London");
        assertThat(hotel.at("/metadata/address/city").asText()).isEqualTo("London");
        assertThat(hotel.at("/metadata/address/country").asText()).isEqualTo("GB");
        assertAmount(hotel.at("/tax_lines").get(0), "rate", "0.07");

        JsonNode rail = getBody("/transactions/" + ids.get("rail"));
        assertThat(rail.at("/metadata/legs")).hasSize(2); // a change of train = two legs
        assertThat(rail.at("/metadata/legs/0/train_number").asText()).isEqualTo("EST9108");
        assertThat(rail.at("/metadata/legs/0/class").asText()).isEqualTo("standard"); // Java-keyword field

        JsonNode tripFee = getBody("/transactions/" + ids.get("trip_fee"));
        assertThat(tripFee.at("/metadata/fee_kind").asText()).isEqualTo("trip_fee");
        assertThat(tripFee.at("/metadata/related_external_id").asText()).isEqualTo("flight-TXL-LHR-2026-03-11");

        JsonNode agentFee = getBody("/transactions/" + ids.get("agent_call_fee"));
        assertThat(agentFee.at("/metadata/fee_kind").asText()).isEqualTo("agent_call_fee");
    }

    // ---- Spec test #2: duplicate external_id → 409 with existing id, exactly one row

    @Test
    void duplicateExternalIdReturns409WithExistingIdAndStoresOneRow() {
        String id = createSeed("flight");

        ResponseEntity<JsonNode> second = postTransaction(SEEDS.get("flight"));

        assertError(second, 409, "DUPLICATE_TRANSACTION");
        assertThat(second.getBody().get("existing_id").asText()).isEqualTo(id);
        assertThat(countByExternalId("flight-TXL-LHR-2026-03-11")).isEqualTo(1);
    }

    // ---- Spec test #10: lists are always bounded

    @Test
    void listDefaultsToLimit50AndRejectsUnbounded() {
        createSeed("flight");
        createSeed("hotel");
        createSeed("rail");

        JsonNode page = getBody("/transactions");
        assertThat(page.get("limit").asInt()).isEqualTo(50);
        assertThat(page.get("offset").asInt()).isEqualTo(0);
        assertThat(page.get("total").asInt()).isEqualTo(3);
        assertThat(page.get("items")).hasSize(3);

        assertError(rest.getForEntity("/transactions?limit=1000", JsonNode.class), 400, "VALIDATION_FAILED");
        assertError(rest.getForEntity("/transactions?limit=0", JsonNode.class), 400, "VALIDATION_FAILED");
        assertError(rest.getForEntity("/transactions?offset=-1", JsonNode.class), 400, "VALIDATION_FAILED");

        JsonNode tail = getBody("/transactions?limit=2&offset=2");
        assertThat(tail.get("items")).hasSize(1);
        assertThat(tail.get("limit").asInt()).isEqualTo(2);
    }

    // ---- 400 / 404 contract

    @Test
    void unknownTypeReturns400() {
        ObjectNode body = SEEDS.get("flight").deepCopy();
        body.put("type", "spaceflight");

        assertError(postTransaction(body), 400, "UNKNOWN_TYPE");

        // non-canonical casing is not a type either
        ObjectNode legacy = SEEDS.get("trip_fee").deepCopy();
        legacy.put("type", "Navan_fee");
        assertError(postTransaction(legacy), 400, "UNKNOWN_TYPE");
    }

    @Test
    void unknownFeeKindReturns400() {
        ObjectNode body = SEEDS.get("trip_fee").deepCopy();
        ((ObjectNode) body.get("metadata")).put("fee_kind", "pizza_fee");

        assertError(postTransaction(body), 400, "UNKNOWN_FEE_KIND");
    }

    @Test
    void missingRequiredFieldReturns400() {
        ObjectNode body = SEEDS.get("flight").deepCopy();
        body.remove("external_id");

        assertError(postTransaction(body), 400, "VALIDATION_FAILED");
    }

    @Test
    void unknownFieldReturns400() {
        ObjectNode body = SEEDS.get("flight").deepCopy();
        body.put("surprise", true);

        assertError(postTransaction(body), 400, "VALIDATION_FAILED");
    }

    @Test
    void amountBeyondNumeric19_2Returns400Not409() {
        // Bigger than the NUMERIC(19,2) column: must fail validation (400), never reach the
        // DB — an unvalidated overflow escapes as DataIntegrityViolation → misleading 409.
        ObjectNode body = SEEDS.get("flight").deepCopy();
        body.put("total", new java.math.BigDecimal("1e30"));
        assertError(postTransaction(body), 400, "VALIDATION_FAILED");

        ObjectNode lineBody = SEEDS.get("flight").deepCopy();
        ((ObjectNode) lineBody.get("tax_lines").get(0)).put("amount", new java.math.BigDecimal("1e30"));
        assertError(postTransaction(lineBody), 400, "VALIDATION_FAILED");

        // the exact boundary is storable
        ObjectNode edge = SEEDS.get("flight").deepCopy();
        edge.put("external_id", "flight-max-amount");
        edge.put("total", new java.math.BigDecimal("99999999999999999.99"));
        assertThat(postTransaction(edge).getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void getPatchDeleteOfMissingTransactionReturn404() {
        assertError(rest.getForEntity("/transactions/txn_missing", JsonNode.class), 404, "TRANSACTION_NOT_FOUND");
        assertError(patch("/transactions/txn_missing", MAPPER.createObjectNode().put("total", 1)), 404,
                "TRANSACTION_NOT_FOUND");
        assertError(delete("/transactions/txn_missing"), 404, "TRANSACTION_NOT_FOUND");
    }

    // ---- PATCH / DELETE happy paths (not invoiced)

    @Test
    void patchUpdatesFieldsAndReplaceAllsLineLists() {
        String id = createSeed("flight");

        ResponseEntity<JsonNode> patchResponse = patch("/transactions/" + id, MAPPER.createObjectNode()
                .put("total", 250.00)
                .putPOJO("tax_lines", java.util.List.of())
                .putPOJO("fee_lines", java.util.List.of(
                        MAPPER.createObjectNode().put("name", "YQ").put("amount", 30.00))));
        assertThat(patchResponse.getStatusCode().value()).isEqualTo(200);
        JsonNode patched = patchResponse.getBody();

        assertAmount(patched, "total", "250.00");
        assertThat(patched.get("tax_lines")).isEmpty();
        assertThat(patched.get("fee_lines")).hasSize(1);
        assertAmount(patched.at("/fee_lines").get(0), "amount", "30.00");
        // untouched fields survive
        assertThat(patched.at("/metadata/coupons/0/flight_number").asText()).isEqualTo("BA982");
    }

    @Test
    void patchRejectsMetadataOfWrongShape() {
        String id = createSeed("flight");

        // hotel-shaped metadata on a flight → 400 (a flight can never become a hotel blob)
        ResponseEntity<JsonNode> response = patch("/transactions/" + id,
                MAPPER.createObjectNode().set("metadata", SEEDS.get("hotel").get("metadata")));

        assertError(response, 400, "VALIDATION_FAILED");
        assertAmount(getBody("/transactions/" + id), "total", "220.00"); // nothing persisted
    }

    @Test
    void deleteRemovesTransactionFromTheApi() {
        String id = createSeed("rail");

        assertThat(delete("/transactions/" + id).getStatusCode().value()).isEqualTo(204);
        assertError(rest.getForEntity("/transactions/" + id, JsonNode.class), 404, "TRANSACTION_NOT_FOUND");
        assertThat(countByExternalId("rail-STP-PAR-2026-03-13")).isZero();
    }

    @Test
    void forceDeleteBypassesTheInvoicedGuardAndLeavesTheInvoiceUntouched() {
        String id = createSeed("flight");
        String invoiceId = createInvoice(java.util.List.of(id)).getBody().get("id").asText();

        // default (force=false): the invoiced guard still applies
        assertError(delete("/transactions/" + id), 409, "TRANSACTION_INVOICED");

        // force=true: soft delete allowed even though the transaction is billed
        assertThat(delete("/transactions/" + id + "?force=true").getStatusCode().value()).isEqualTo(204);

        // the transaction is gone from the API…
        assertError(rest.getForEntity("/transactions/" + id, JsonNode.class), 404, "TRANSACTION_NOT_FOUND");

        // …and the invoice is untouched: frozen snapshot, total and line intact
        JsonNode invoice = getBody("/invoices/" + invoiceId);
        assertAmount(invoice, "total", "220.00");
        assertThat(invoice.get("lines")).hasSize(1);
        assertThat(invoice.get("lines").get(0).get("transaction_id").asText()).isEqualTo(id);
        assertAmount(invoice.get("lines").get(0), "total", "220.00");
    }

    @Test
    void deletedExternalIdCanBeRecreatedWithNewId() {
        String id = createSeed("hotel");
        delete("/transactions/" + id);

        ResponseEntity<JsonNode> recreated = postTransaction(SEEDS.get("hotel"));

        assertThat(recreated.getStatusCode().value()).isEqualTo(201);
        assertThat(recreated.getBody().get("id").asText()).isNotEqualTo(id);
    }
}
