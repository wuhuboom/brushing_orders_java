package com.order.testsupport;

import java.net.URI;
import java.sql.DriverManager;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Docker by default; explicitly selected loopback test schemas also work without Docker. */
public final class IsolatedMySqlDatabase implements BeforeAllCallback, AfterAllCallback {
    private final String property;
    private final String database;
    private MySQLContainer container;
    private String url;

    public IsolatedMySqlDatabase(String property, String database) {
        this.property = property;
        this.database = database;
        if (!database.startsWith("codex_") || !database.endsWith("_test")) {
            throw new IllegalArgumentException("A dedicated test schema is required");
        }
    }

    @Override public void beforeAll(ExtensionContext context) throws Exception {
        String override = System.getProperty(property);
        if (override == null) {
            assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                    "Docker unavailable; set -D" + property + " for isolated local MySQL");
            container = new MySQLContainer("mysql:8.0.42").withDatabaseName(database)
                    .withUsername("order").withPassword("order");
            container.start();
            url = container.getJdbcUrl();
        } else {
            assertTrue(override.startsWith("jdbc:mysql://"));
            URI endpoint = URI.create(override.substring("jdbc:".length()));
            assertEquals("127.0.0.1", endpoint.getHost());
            assertEquals("/" + database, endpoint.getPath());
            assertTrue(endpoint.getPort() > 0 && endpoint.getPort() <= 65535 && endpoint.getPort() != 3306,
                    "An explicit nonstandard test port is required");
            assertEquals("127.0.0.1:" + endpoint.getPort(), endpoint.getRawAuthority());
            assertNull(endpoint.getRawQuery());
            assertNull(endpoint.getRawFragment());
            url = override;
        }
        try (var connection = DriverManager.getConnection(url, getUsername(), getPassword());
             var statement = connection.createStatement();
             var result = statement.executeQuery("SELECT DATABASE()")) {
            assertTrue(result.next());
            assertEquals(database, result.getString(1));
        }
    }

    @Override public void afterAll(ExtensionContext context) {
        if (container != null) container.stop();
    }

    public String getJdbcUrl() { return url; }
    public String getUsername() { return container == null ? "root" : container.getUsername(); }
    public String getPassword() { return container == null ? "" : container.getPassword(); }
}
