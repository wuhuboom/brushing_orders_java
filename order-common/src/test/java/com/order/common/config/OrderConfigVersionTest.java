package com.order.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderConfigVersionTest
{
    @Test
    void explicitRuntimeVersionTakesPrecedence()
    {
        assertEquals("4.0.1", OrderConfig.resolveApplicationVersion("4.0.1", buildProperties("3.9.0")));
    }

    @Test
    void unresolvedIdeTokenFallsBackToBuildMetadata()
    {
        assertEquals("3.9.0", OrderConfig.resolveApplicationVersion("@project.version@", buildProperties("3.9.0")));
    }

    @Test
    void cleanIdeRunUsesHonestDevelopmentVersion()
    {
        assertEquals("development", OrderConfig.resolveApplicationVersion("@project.version@", null));
    }

    private BuildProperties buildProperties(String version)
    {
        Properties properties = new Properties();
        properties.setProperty("version", version);
        return new BuildProperties(properties);
    }
}
