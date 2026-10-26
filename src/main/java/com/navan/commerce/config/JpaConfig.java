package com.navan.commerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Enables created_at / updated_at auditing on entities (see AuditingEntityListener usage). */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
