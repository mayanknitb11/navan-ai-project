package com.navan.commerce.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Renders {@link Instant}s on the wire in Asia/Kolkata ({@code +05:30}) instead of UTC
 * ({@code Z}) — e.g. {@code "2026-10-26T09:37:27.910352+05:30"}. Values are still absolute
 * instants (TIMESTAMPTZ at rest); only the rendering zone changes. Deserialization is
 * untouched: clients may keep sending any ISO-8601 offset, including {@code Z}.
 */
@Configuration
public class JacksonConfig {

    public static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");

    private static final DateTimeFormatter IST_FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(APP_ZONE);

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer istInstantRendering() {
        // serializers() (not modules()) so the well-known JavaTimeModule registration — which
        // owns Instant DESERIALIZATION — stays intact.
        return builder -> builder.serializers(new JsonSerializer<Instant>() {
            @Override
            public void serialize(Instant value, JsonGenerator gen,
                                  SerializerProvider serializers) throws IOException {
                gen.writeString(IST_FORMATTER.format(value));
            }

            @Override
            public Class<Instant> handledType() {
                return Instant.class; // anonymous class: generic type is erased, declare it
            }
        });
    }
}
