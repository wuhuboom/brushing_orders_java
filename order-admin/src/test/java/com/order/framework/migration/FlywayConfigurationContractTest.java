package com.order.framework.migration;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSource;
import com.order.framework.config.DruidConfig;
import com.order.framework.config.properties.DruidProperties;
import com.order.quartz.service.impl.SysJobServiceImpl;
import com.order.system.service.impl.SysConfigServiceImpl;
import com.order.system.service.impl.SysDictTypeServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FlywayConfigurationContractTest
{
    @Test
    void applicationUsesOnlyTheGuardedMigrationLocation() throws Exception
    {
        String yaml = classpathText("application.yml").replace("\r\n", "\n");

        assertTrue(yaml.matches("(?s).*\\n  version: \\\"\\$\\{APP_VERSION:(?!@project\\.version@)[^}\\r\\n]+}\\\".*"));
        assertTrue(yaml.contains("  migration-version: 1.2.2"));
        Value migrationVersionValue = ControlledFlywayMigrationStrategy.class
                .getConstructor(String.class)
                .getParameters()[0]
                .getAnnotation(Value.class);
        assertNotNull(migrationVersionValue);
        assertEquals("${order.migration-version:${order.version}}", migrationVersionValue.value());
        assertTrue(yaml.contains("flyway:\n    enabled: true"));
        assertFalse(yaml.contains("DB_AUTO_MIGRATION_ENABLED"));
        assertTrue(yaml.contains("locations: classpath:db/migration/mysql"));
        assertTrue(yaml.contains("baseline-on-migrate: false"));
        assertTrue(yaml.contains("baseline-version: 2026.08.17.0"));
        assertTrue(yaml.contains("baseline-description: Controlled legacy adoption through 20260817"));
        assertTrue(yaml.contains("validate-on-migrate: true"));
        assertTrue(yaml.contains("validate-migration-naming: true"));
        assertTrue(yaml.contains("out-of-order: false"));
        assertTrue(yaml.contains("clean-disabled: true"));
        assertTrue(yaml.contains("placeholder-replacement: false"));
        assertTrue(yaml.contains("fail-on-missing-locations: true"));
        assertFalse(yaml.contains("classpath:sql"));
    }

    @Test
    void flywayUsesIndependentNonPooledDataSourceAndStartupReadersKeepInitializationOrdering() throws Exception
    {
        Method propertiesFactory = DruidConfig.class.getMethod("masterDataSourceProperties");
        ConfigurationProperties binding = propertiesFactory.getAnnotation(ConfigurationProperties.class);
        assertNotNull(binding);
        assertEquals("spring.datasource.druid.master", binding.value());

        Method masterFactory = DruidConfig.class.getMethod("masterDataSource",
                DataSourceProperties.class, DruidProperties.class);
        Method flywayFactory = DruidConfig.class.getMethod("flywayDataSource", DataSourceProperties.class);
        assertNull(masterFactory.getAnnotation(FlywayDataSource.class));
        assertNotNull(flywayFactory.getAnnotation(FlywayDataSource.class));

        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl("jdbc:mysql://example.invalid:3306/order?serverTimezone=UTC");
        properties.setUsername("order_app");
        properties.setPassword("test-password");

        DruidProperties druidProperties = mock(DruidProperties.class);
        when(druidProperties.dataSource(any(DruidDataSource.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        DruidConfig config = new DruidConfig();
        DataSource master = config.masterDataSource(properties, druidProperties);
        DataSource flyway = config.flywayDataSource(properties);

        assertInstanceOf(DruidDataSource.class, master);
        DriverManagerDataSource flywayDataSource = assertInstanceOf(DriverManagerDataSource.class, flyway);
        assertNotSame(master, flywayDataSource);
        assertEquals(properties.getUrl(), flywayDataSource.getUrl());
        assertEquals(properties.getUsername(), flywayDataSource.getUsername());
        assertEquals(properties.getPassword(), flywayDataSource.getPassword());

        assertNotNull(SysConfigServiceImpl.class.getAnnotation(DependsOnDatabaseInitialization.class));
        assertNotNull(SysDictTypeServiceImpl.class.getAnnotation(DependsOnDatabaseInitialization.class));
        assertNotNull(SysJobServiceImpl.class.getAnnotation(DependsOnDatabaseInitialization.class));
    }

    private static String classpathText(String name) throws IOException
    {
        try (var stream = FlywayConfigurationContractTest.class.getClassLoader().getResourceAsStream(name))
        {
            assertNotNull(stream, name);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
