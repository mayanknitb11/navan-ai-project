package com.navan.commerce.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata — the UI itself is auto-scanned from the controllers by springdoc. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI commerceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Navan Commerce API")
                .description("Flight / hotel / rail / Navan-fee transactions + frozen-snapshot invoices (take-home B)")
                .version("v1"));
    }
}
