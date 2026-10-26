package com.navan.commerce.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Both {@code transaction_ids} (canonical) and {@code transactionIds} (alias) deserialize. */
class CreateInvoiceRequestTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    void snakeCaseDeserializes() throws Exception {
        CreateInvoiceRequest request = mapper.readValue(
                "{\"transaction_ids\": [\"txn_1\", \"txn_2\"]}", CreateInvoiceRequest.class);

        assertThat(request.getTransactionIds()).containsExactly("txn_1", "txn_2");
    }

    @Test
    void camelCaseAliasDeserializes() throws Exception {
        CreateInvoiceRequest request = mapper.readValue(
                "{\"transactionIds\": [\"txn_1\"]}", CreateInvoiceRequest.class);

        assertThat(request.getTransactionIds()).isEqualTo(List.of("txn_1"));
    }

    @Test
    void serializesToSnakeCase() throws Exception {
        String json = mapper.writeValueAsString(
                CreateInvoiceRequest.builder().transactionIds(List.of("txn_1")).build());

        assertThat(json).contains("transaction_ids").doesNotContain("transactionIds");
    }
}
