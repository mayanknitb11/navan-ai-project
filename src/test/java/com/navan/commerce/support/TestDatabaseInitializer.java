package com.navan.commerce.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the integration-test database if it is missing, BEFORE the context starts (Flyway
 * migrates it right after). Connects to the maintenance database {@code postgres} on the same
 * server the test datasource points at. Keeps {@code ./mvnw test} a one-command run as long as
 * the Docker Postgres container is up.
 */
public class TestDatabaseInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = env.getProperty("spring.datasource.url");
        String user = env.getProperty("spring.datasource.username");
        String password = env.getProperty("spring.datasource.password");

        String maintenanceUrl = url.substring(0, url.lastIndexOf('/')) + "/postgres";
        // From our own application-test.yaml, not user input — safe to interpolate.
        String dbName = url.substring(url.lastIndexOf('/') + 1);

        try (Connection conn = DriverManager.getConnection(maintenanceUrl, user, password);
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + dbName.replace("'", "''") + "'");
            if (!rs.next()) {
                stmt.execute("CREATE DATABASE \"" + dbName.replace("\"", "") + "\"");
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Cannot reach Postgres for integration tests (is the navan-commerce-db container running?): "
                            + url, e);
        }
    }
}
