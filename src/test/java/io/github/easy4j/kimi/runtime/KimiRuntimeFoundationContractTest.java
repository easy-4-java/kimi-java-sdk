/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.github.easy4j.kimi.KimiException;
import io.github.easy4j.kimi.KimiRuntimeClosedException;
import io.github.easy4j.kimi.KimiTimeoutException;
import io.github.easy4j.kimi.KimiUnsupportedCapabilityException;

class KimiRuntimeFoundationContractTest {

    @Test
    void shouldExposeImmutableCapabilitiesAndRejectUnsupportedCapability() {
        KimiCapabilities capabilities = KimiCapabilities.of(
                KimiCapability.PROMPT,
                KimiCapability.SESSION);

        assertTrue(capabilities.supports(KimiCapability.PROMPT));
        assertTrue(capabilities.supports(KimiCapability.SESSION));
        assertFalse(capabilities.supports(KimiCapability.WEBSOCKET));

        KimiUnsupportedCapabilityException error = assertThrows(
                KimiUnsupportedCapabilityException.class,
                () -> capabilities.require(KimiCapability.WEBSOCKET));

        assertTrue(error instanceof KimiException);
        assertEquals(KimiCapability.WEBSOCKET, error.getCapability());
        assertThrows(UnsupportedOperationException.class,
                () -> capabilities.asSet().add(KimiCapability.WEBSOCKET));
    }

    @Test
    void shouldKeepLifecycleAndHealthAsSeparateObservableFacts() {
        KimiHealth healthy = KimiHealth.healthy("ready");
        KimiHealth unhealthy = KimiHealth.unhealthy("transport unavailable");

        assertTrue(healthy.isHealthy());
        assertEquals("ready", healthy.getSummary());
        assertFalse(unhealthy.isHealthy());
        assertEquals("transport unavailable", unhealthy.getSummary());

        assertEquals(Arrays.asList(
                        KimiRuntimeState.NEW,
                        KimiRuntimeState.STARTING,
                        KimiRuntimeState.READY,
                        KimiRuntimeState.CLOSING,
                        KimiRuntimeState.CLOSED,
                        KimiRuntimeState.FAILED),
                Arrays.asList(KimiRuntimeState.values()));
    }

    @Test
    void shouldKeepRuntimeExceptionsCompatibleWithKimiException() {
        KimiRuntimeClosedException closed = new KimiRuntimeClosedException("closed");
        KimiTimeoutException timeout = new KimiTimeoutException("timeout");

        assertTrue(closed instanceof KimiException);
        assertTrue(timeout instanceof KimiException);
    }
}
