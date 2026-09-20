/*
 * Copyright (c) 2018-present, easy-4-java (https://github.com/easy-4-java).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.easy4j.kimi.acp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.github.easy4j.kimi.KimiException;

/**
 * RED tests for the ACP lifecycle hardening OpenSpec change.
 *
 * <p>These tests intentionally target failure/race/resource cases that the
 * current implementation does not yet satisfy. They must be committed before
 * the production fix so CI provides evidence for the TDD RED phase.</p>
 */
class KimiAcpLifecycleHardeningTest {

    private static final String FAKE_AGENT = Paths
            .get("src", "test", "resources", "fake-acp-agent.py")
            .toAbsolutePath().toString();

    private static KimiAcpConfig config(String mode) {
        KimiAcpConfig config = new KimiAcpConfig();
        config.setLocalExecutable("python3");
        config.setAcpSubcommand(null);
        config.setAcpArgs(new String[] {FAKE_AGENT, mode});
        config.setConnectTimeoutMillis(2_000);
        config.setReadTimeoutMillis(5_000);
        return config;
    }

    @Test
    void shouldRemovePendingRpcWhenRequestCannotWrite() throws Exception {
        try (KimiAcpClient client = new KimiAcpClient(config("normal"))) {
            assertThrows(KimiException.class, () -> client.newSession("/tmp"));
            assertEquals(0, privateMapSize(client, "pendingRpcs"),
                    "failed write before connect must not leave an orphaned RPC");
        }
    }

    @Test
    void shouldRejectSecondPromptForSameSessionBeforeSending() throws Exception {
        try (KimiAcpClient client = new KimiAcpClient(config("delay-prompt"))) {
            client.connect();
            String sessionId = client.newSession("/tmp");

            CompletableFuture<KimiAcpTurnResult> first =
                    client.promptAsync(sessionId, "first", null);

            assertThrows(KimiException.class,
                    () -> client.promptAsync(sessionId, "second", null),
                    "same session must not overwrite an active prompt stream");

            client.cancel(sessionId);
            first.cancel(true);
        }
    }

    @Test
    void shouldIsolateThrowingDeltaCallbackAndKeepTransportUsable() throws Exception {
        try (KimiAcpClient client = new KimiAcpClient(config("normal"))) {
            client.connect();
            String sessionId = client.newSession("/tmp");

            CompletableFuture<KimiAcpTurnResult> first =
                    client.promptAsync(sessionId, "callback-fails", delta -> {
                        throw new IllegalStateException("listener boom");
                    });

            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> first.get(2, TimeUnit.SECONDS));
            assertTrue(failure.getCause() instanceof KimiException);
            assertTrue(failure.getCause().getMessage().contains("callback"));

            List<String> deltas = new ArrayList<String>();
            KimiAcpTurnResult second = client.prompt("sess_after_callback", "still-alive", deltas::add);
            assertEquals("你好世界", second.getContent());
            assertEquals(2, deltas.size());
        }
    }

    @Test
    void shouldFailPendingPromptOnMalformedJsonFrameInsteadOfTimingOut() throws Exception {
        try (KimiAcpClient client = new KimiAcpClient(config("malformed-prompt"))) {
            client.connect();
            String sessionId = client.newSession("/tmp");

            CompletableFuture<KimiAcpTurnResult> future =
                    client.promptAsync(sessionId, "bad-frame", null);

            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> future.get(2, TimeUnit.SECONDS));
            assertTrue(failure.getCause() instanceof KimiException);
            assertTrue(failure.getCause().getMessage().toLowerCase().contains("json")
                            || failure.getCause().getMessage().toLowerCase().contains("protocol"),
                    "malformed ACP frame must become a protocol failure");
        }
    }

    @Test
    void shouldFailPendingPromptWhenAgentProcessExits() throws Exception {
        try (KimiAcpClient client = new KimiAcpClient(config("exit-on-prompt"))) {
            client.connect();
            String sessionId = client.newSession("/tmp");

            CompletableFuture<KimiAcpTurnResult> future =
                    client.promptAsync(sessionId, "exit", null);

            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> future.get(2, TimeUnit.SECONDS));
            assertTrue(failure.getCause() instanceof KimiException);
        }
    }


    @Test
    void shouldCompletePromptAsCancelledAndReleaseRegistries() throws Exception {
        try (KimiAcpClient client = new KimiAcpClient(config("delay-prompt"))) {
            client.connect();
            String sessionId = client.newSession("/tmp");

            CompletableFuture<KimiAcpTurnResult> future =
                    client.promptAsync(sessionId, "cancel-me", null);
            client.cancel(sessionId);

            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> future.get(1, TimeUnit.SECONDS));
            assertTrue(failure.getCause() instanceof KimiException);
            assertTrue(failure.getCause().getMessage().toLowerCase().contains("cancel"));
            assertEquals(0, privateMapSize(client, "pendingRpcs"));
            assertEquals(0, privateMapSize(client, "promptStreams"));
        }
    }

    @Test
    void shouldRemoveTimedOutRpcFromPendingRegistry() throws Exception {
        KimiAcpConfig config = config("hang-list");
        config.setConnectTimeoutMillis(250);
        try (KimiAcpClient client = new KimiAcpClient(config)) {
            client.connect();

            assertThrows(KimiException.class, client::listSessions);
            assertEquals(0, privateMapSize(client, "pendingRpcs"),
                    "timed out RPC must be removed from the pending registry");
        }
    }

    @Test
    void shouldFailAndCleanPendingPromptWhenClientCloses() throws Exception {
        KimiAcpClient client = new KimiAcpClient(config("delay-prompt"));
        client.connect();
        String sessionId = client.newSession("/tmp");
        CompletableFuture<KimiAcpTurnResult> future =
                client.promptAsync(sessionId, "close-me", null);

        client.close();

        assertThrows(ExecutionException.class, () -> future.get(1, TimeUnit.SECONDS));
        assertEquals(0, privateMapSize(client, "pendingRpcs"));
        assertEquals(0, privateMapSize(client, "promptStreams"));
        client.close();
    }

    @SuppressWarnings("unchecked")
    private static int privateMapSize(KimiAcpClient client, String fieldName) throws Exception {
        Field field = KimiAcpClient.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return ((Map<Object, Object>) field.get(client)).size();
    }
}
