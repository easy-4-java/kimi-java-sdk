/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import io.github.easy4j.kimi.KimiClient;
import io.github.easy4j.kimi.KimiClientConfig;
import io.github.easy4j.kimi.KimiException;
import io.github.easy4j.kimi.KimiRuntimeClosedException;
import io.github.easy4j.kimi.KimiUnsupportedCapabilityException;
import io.github.easy4j.kimi.acp.KimiAcpClient;
import io.github.easy4j.kimi.acp.KimiAcpConfig;
import io.github.easy4j.kimi.model.KimiPromptRequest;
import io.github.easy4j.kimi.model.KimiPromptResult;
import io.github.easy4j.kimi.server.KimiServerClient;
import io.github.easy4j.kimi.server.KimiServerConfig;
import tools.jackson.databind.JsonNode;

class KimiRuntimeAdapterContractTest {

    @Test
    void shouldExposeRouteSpecificCapabilities() {
        KimiCliRuntime cli = new KimiCliRuntime(new KimiClient(new KimiClientConfig()));
        KimiAcpRuntime acp = new KimiAcpRuntime(new StubAcpClient());
        KimiServerRuntime server = new KimiServerRuntime(new StubServerClient());

        assertTrue(cli.capabilities().supports(KimiCapability.PROMPT));
        assertTrue(acp.capabilities().supports(KimiCapability.PROMPT));
        assertTrue(acp.capabilities().supports(KimiCapability.CANCEL));
        assertTrue(acp.capabilities().supports(KimiCapability.ACP));
        assertTrue(server.capabilities().supports(KimiCapability.SERVER));
        assertEquals(false, server.capabilities().supports(KimiCapability.PROMPT));

        cli.close();
        acp.close();
        server.close();
    }

    @Test
    void shouldPropagateAcpFailureWithoutImplicitFallback() {
        KimiException sentinel = new KimiException("acp sentinel");
        StubAcpClient client = new StubAcpClient();
        client.promptFailure = sentinel;
        KimiRuntime runtime = new KimiAcpRuntime(client);

        KimiPromptRequest request = KimiPromptRequest.builder()
                .sessionId("s1")
                .text("hello")
                .build();

        KimiException actual = assertThrows(KimiException.class, () -> runtime.prompt(request));

        assertSame(sentinel, actual,
                "ACP failure must be surfaced as-is instead of silently falling back to another route");
        assertEquals(1, client.promptCalls.get());
        runtime.close();
    }

    @Test
    void shouldRejectUnsupportedServerPromptBeforeHttpSideEffects() {
        StubServerClient client = new StubServerClient();
        KimiRuntime runtime = new KimiServerRuntime(client);
        KimiPromptRequest request = KimiPromptRequest.builder()
                .sessionId("s1")
                .text("hello")
                .build();

        KimiUnsupportedCapabilityException error = assertThrows(
                KimiUnsupportedCapabilityException.class,
                () -> runtime.prompt(request));

        assertEquals(KimiCapability.PROMPT, error.getCapability());
        assertEquals(0, client.promptCalls.get(),
                "unsupported operation must fail before touching the server transport");
        runtime.close();
    }

    @Test
    void shouldKeepClosedRuntimeTerminalAndRejectNewOperations() {
        KimiRuntime runtime = new KimiCliRuntime(new KimiClient(new KimiClientConfig()));
        runtime.close();

        assertEquals(KimiRuntimeState.CLOSED, runtime.state());
        assertEquals(false, runtime.health().isHealthy());
        assertThrows(KimiRuntimeClosedException.class,
                () -> runtime.prompt(KimiPromptRequest.builder()
                        .sessionId("s1")
                        .text("hello")
                        .build()));
    }

    private static final class StubAcpClient extends KimiAcpClient {

        private final AtomicInteger promptCalls = new AtomicInteger();
        private KimiException promptFailure;

        private StubAcpClient() {
            super(new KimiAcpConfig());
        }

        @Override
        public KimiPromptResult prompt(KimiPromptRequest request, Consumer<String> onDelta) {
            promptCalls.incrementAndGet();
            if (promptFailure != null) {
                throw promptFailure;
            }
            return KimiPromptResult.of(
                    request.getSessionId(), "end_turn", "ok");
        }
    }

    private static final class StubServerClient extends KimiServerClient {

        private final AtomicInteger promptCalls = new AtomicInteger();

        private StubServerClient() {
            super(new KimiServerConfig());
        }

        @Override
        public JsonNode postPrompt(String sessionId, String text) {
            promptCalls.incrementAndGet();
            throw new AssertionError("postPrompt must not be called");
        }
    }
}
