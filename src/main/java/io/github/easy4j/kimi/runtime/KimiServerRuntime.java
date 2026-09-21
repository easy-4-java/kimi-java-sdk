/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.runtime;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.easy4j.kimi.KimiRuntimeClosedException;
import io.github.easy4j.kimi.model.KimiPromptRequest;
import io.github.easy4j.kimi.model.KimiPromptResult;
import io.github.easy4j.kimi.server.KimiServerClient;

/**
 * REST server adapter.
 *
 * <p>The current REST route can submit prompts, but without the WebSocket
 * event stream it cannot synchronously promise a final assistant turn result.
 * Therefore this adapter intentionally does not advertise PROMPT yet.</p>
 */
public final class KimiServerRuntime implements KimiRuntime {

    private static final KimiCapabilities CAPABILITIES = KimiCapabilities.of(
            KimiCapability.SESSION,
            KimiCapability.CANCEL,
            KimiCapability.SERVER);

    private final KimiServerClient client;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public KimiServerRuntime(KimiServerClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    public KimiCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public KimiHealth health() {
        if (closed.get()) {
            return KimiHealth.unhealthy("closed");
        }
        try {
            client.healthz();
            return KimiHealth.healthy("server healthy");
        } catch (RuntimeException e) {
            return KimiHealth.unhealthy("server health check failed");
        }
    }

    @Override
    public KimiRuntimeState state() {
        return closed.get() ? KimiRuntimeState.CLOSED : KimiRuntimeState.READY;
    }

    @Override
    public KimiPromptResult prompt(KimiPromptRequest request) {
        requireOpen();
        CAPABILITIES.require(KimiCapability.PROMPT);
        throw new AssertionError("unreachable");
    }

    @Override
    public void cancel(String executionId) {
        requireOpen();
        CAPABILITIES.require(KimiCapability.CANCEL);
        client.abort(executionId);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            client.close();
        }
    }

    private void requireOpen() {
        if (closed.get()) {
            throw new KimiRuntimeClosedException("kimi server runtime is closed");
        }
    }
}
