package com.droidablebee.micronaut.rest;

import io.micronaut.context.env.Environment;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class ApplicationITest {

    @Inject
    EmbeddedApplication<?> application;

    @Inject
    Environment environment;

    @DisplayName("application is configured correctly and starts up")
    @Test
    void testAppStartup() {

        assertTrue(application.isRunning());
        assertNotNull(environment);
    }

}
