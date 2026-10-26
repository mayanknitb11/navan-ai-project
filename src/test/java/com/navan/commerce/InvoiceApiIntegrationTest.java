package com.navan.commerce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.navan.commerce.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invoice API against the real HTTP server + real Postgres.
 * Spec tests #3 (trip invoice totals), #4 (snapshot freeze), #5 (mixed currency),
 * #7 (PATCH invoiced → 409), #8 (DELETE invoiced → 409), #9 (DELETE invoice keeps transactions).
 */
class InvoiceApiIntegrationTest extends AbstractIntegrationTest {

    /** Creates the full trip (flight + hotel + rail + both Navan fees) and invoices it. */
    private record Trip(String flightId, String invoiceId) {
    }

    private Trip createTripInvoice() {
        List<String> ids = new ArrayList<>();
        String flightId = createSeed("flight");
        ids.add(flightId);
        ids.add(createSeed("hotel"));
        ids.add(createSeed("rail"));
        ids.add(createSeed("trip_fee"));
        ids.add(createSeed("agent_call_fee"));

        ResponseEntity<JsonNode> response = createInvoice(ids);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return new Trip(flightId, response.getBody().get("id").asText());
    }

    // ---- Spec test #3: trip invoice matches expected-invoice.json (grand total 685.70 EUR)

    @Test
    void tripInvoiceMatchesExpectedFixture() {
        Trip trip = createTripInvoice();

        JsonNode invoice = getBody("/invoices/" + trip.invoiceId());
        assertThat(invoice.get("id").asText()).startsWith("inv_");
        assertThat(invoice.get("currency").asText()).isEqualTo("EUR");
        assertAmount(invoice, "total", "685.70");
        assertThat(invoice.get("line_count").asInt()).isEqualTo(5);
        assertThat(invoice.get("lines")).hasSize(5);

        // every line is a snapshot: type + frozen tax/fee split + metadata
        assertThat(invoice.get("lines")).allSatisfy(line -> {
            assertThat(line.get("transaction_id").asText()).startsWith("txn_");
            assertThat(line.has("metadata")).isTrue();
        });

        // tax rollup (by name+rate): Airport tax 15.00, VAT@0.07 21.70 — matches fixture
        assertThat(invoice.get("tax_rollup")).hasSize(2);
        assertThat(findByName(invoice.get("tax_rollup"), "Airport tax").decimalValue()).isEqualByComparingTo("15.00");
        JsonNode vat = invoice.get("tax_rollup").get(0).get("name").asText().equals("VAT")
                ? invoice.get("tax_rollup").get(0) : invoice.get("tax_rollup").get(1);
        assertThat(vat.get("name").asText()).isEqualTo("VAT");
        assertThat(vat.get("rate").decimalValue()).isEqualByComparingTo("0.07");
        assertThat(vat.get("amount").decimalValue()).isEqualByComparingTo("21.70");

        // fee rollup (by name): YQ 25.00, Booking fee 4.00
        assertThat(invoice.get("fee_rollup")).hasSize(2);
        assertThat(findByName(invoice.get("fee_rollup"), "YQ").decimalValue()).isEqualByComparingTo("25.00");
        assertThat(findByName(invoice.get("fee_rollup"), "Booking fee").decimalValue()).isEqualByComparingTo("4.00");

        // flight line keeps its coupons in the snapshot
        JsonNode flightLine = findLineByType(invoice, "flight");
        assertThat(flightLine.at("/metadata/coupons/0/flight_number").asText()).isEqualTo("BA982");
        assertAmount(flightLine, "total", "220.00");
    }

    // ---- Spec tests #4 + #7: PATCH of an invoiced transaction → 409; invoice snapshot frozen

    @Test
    void patchInvoicedTransactionReturns409AndInvoiceStaysFrozen() {
        Trip trip = createTripInvoice();

        // Spec test #4's "change the stored flight total" — the API must refuse and persist nothing
        ResponseEntity<JsonNode> patch = patch("/transactions/" + trip.flightId(),
                MAPPER.createObjectNode().put("total", 999.00));
        assertError(patch, 409, "TRANSACTION_INVOICED");

        // stored transaction unchanged
        assertAmount(getBody("/transactions/" + trip.flightId()), "total", "220.00");

        // invoice snapshot unchanged: flight contribution still 220.00, grand total still 685.70
        JsonNode invoice = getBody("/invoices/" + trip.invoiceId());
        assertAmount(invoice, "total", "685.70");
        assertAmount(findLineByType(invoice, "flight"), "total", "220.00");
    }

    // ---- Spec test #5: mixed currencies → 409, nothing persisted

    @Test
    void mixedCurrencyInvoiceReturns409() {
        String eurId = createSeed("flight");
        ObjectNode usdBody = SEEDS.get("hotel").deepCopy();
        usdBody.put("currency", "USD");
        usdBody.put("external_id", "hotel-USD-1");
        ResponseEntity<JsonNode> usdCreated = postTransaction(usdBody);
        assertThat(usdCreated.getStatusCode().value()).isEqualTo(201);
        String usdId = usdCreated.getBody().get("id").asText();

        ResponseEntity<JsonNode> response = createInvoice(List.of(eurId, usdId));

        assertError(response, 409, "MIXED_CURRENCY");
        assertThat(getBody("/invoices?limit=100").get("total").asInt()).isZero(); // nothing persisted
    }

    // ---- Spec test #8: DELETE of an invoiced transaction → 409, row survives

    @Test
    void deleteInvoicedTransactionReturns409AndRowSurvives() {
        Trip trip = createTripInvoice();

        assertError(delete("/transactions/" + trip.flightId()), 409, "TRANSACTION_INVOICED");
        assertAmount(getBody("/transactions/" + trip.flightId()), "total", "220.00");
    }

    // ---- Spec test #9: DELETE invoice removes it; the transactions still GET

    @Test
    void deleteInvoiceKeepsTransactionsAndUnblocksThem() {
        Trip trip = createTripInvoice();

        assertThat(delete("/invoices/" + trip.invoiceId()).getStatusCode().value()).isEqualTo(204);
        assertError(rest.getForEntity("/invoices/" + trip.invoiceId(), JsonNode.class), 404, "INVOICE_NOT_FOUND");

        // underlying transaction untouched — and now deletable again
        assertAmount(getBody("/transactions/" + trip.flightId()), "total", "220.00");
        assertThat(delete("/transactions/" + trip.flightId()).getStatusCode().value()).isEqualTo(204);
    }

    // ---- Invoice body rules: 400 / 404 / 409

    @Test
    void invoiceWithAbsentOrEmptyIdsReturns400() {
        assertError(rest.postForEntity("/invoices", MAPPER.createObjectNode(), JsonNode.class),
                400, "VALIDATION_FAILED");
        assertError(createInvoice(List.of()), 400, "VALIDATION_FAILED");
    }

    @Test
    void invoiceWithMissingTransactionIdReturns404() {
        String id = createSeed("flight");

        assertError(createInvoice(List.of(id, "txn_missing")), 404, "TRANSACTION_NOT_FOUND");
        assertThat(getBody("/invoices?limit=100").get("total").asInt()).isZero(); // nothing persisted
    }

    @Test
    void invoiceWithRepeatedIdReturns409() {
        String id = createSeed("rail");

        assertError(createInvoice(List.of(id, id)), 409, "DUPLICATE_TRANSACTION_IDS");
    }

    @Test
    void getMissingInvoiceReturns404() {
        assertError(rest.getForEntity("/invoices/inv_missing", JsonNode.class), 404, "INVOICE_NOT_FOUND");
    }

    // ---- PATCH invoice: atomic re-snapshot from CURRENT transactions

    @Test
    void patchInvoiceReplacesSnapshotAtomically() {
        String flightId = createSeed("flight");
        String invoiceId = createInvoice(List.of(flightId)).getBody().get("id").asText();
        assertAmount(getBody("/invoices/" + invoiceId), "total", "220.00");

        // flight is NOT yet invoiced-changed... change total first? No: flight IS invoiced.
        // Re-snapshot to a different set: hotel only.
        String hotelId = createSeed("hotel");
        ResponseEntity<JsonNode> patched = patch("/invoices/" + invoiceId,
                MAPPER.createObjectNode().putPOJO("transaction_ids", List.of(hotelId)));
        assertThat(patched.getStatusCode().value()).isEqualTo(200);
        assertAmount(patched.getBody(), "total", "331.70");
        assertThat(patched.getBody().get("line_count").asInt()).isEqualTo(1);

        JsonNode invoice = getBody("/invoices/" + invoiceId);
        assertThat(findLineByType(invoice, "hotel").at("/metadata/name").asText())
                .isEqualTo("Hotel Mitte London");
        assertThat(invoice.get("tax_rollup")).hasSize(1); // VAT only
    }

    @Test
    void patchInvoiceWithMixedCurrencyReturns409AndLeavesInvoiceUntouched() {
        String eurId = createSeed("flight");
        String invoiceId = createInvoice(List.of(eurId)).getBody().get("id").asText();

        ObjectNode usdBody = SEEDS.get("hotel").deepCopy();
        usdBody.put("currency", "USD");
        usdBody.put("external_id", "hotel-USD-1");
        String usdId = postTransaction(usdBody).getBody().get("id").asText();

        ResponseEntity<JsonNode> response = patch("/invoices/" + invoiceId,
                MAPPER.createObjectNode().putPOJO("transaction_ids", List.of(eurId, usdId)));

        assertError(response, 409, "MIXED_CURRENCY");
        JsonNode invoice = getBody("/invoices/" + invoiceId); // unchanged snapshot
        assertAmount(invoice, "total", "220.00");
        assertThat(invoice.get("line_count").asInt()).isEqualTo(1);
    }

    @Test
    void invoiceListIsBounded() {
        createTripInvoice();

        JsonNode page = getBody("/invoices");
        assertThat(page.get("limit").asInt()).isEqualTo(50);
        assertThat(page.get("total").asInt()).isEqualTo(1);

        assertError(rest.getForEntity("/invoices?limit=1000", JsonNode.class), 400, "VALIDATION_FAILED");
    }

    // ---- helpers ---------------------------------------------------------------

    private static JsonNode findByName(JsonNode rollup, String name) {
        for (JsonNode entry : rollup) {
            if (name.equals(entry.get("name").asText())) {
                return entry.get("amount");
            }
        }
        throw new AssertionError("rollup entry not found: " + name);
    }

    private static JsonNode findLineByType(JsonNode invoice, String type) {
        for (JsonNode line : invoice.get("lines")) {
            if (type.equals(line.get("type").asText())) {
                return line;
            }
        }
        throw new AssertionError("invoice line of type not found: " + type);
    }
}
