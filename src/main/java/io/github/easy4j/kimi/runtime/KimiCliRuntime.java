/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.runtime;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.easy4j.kimi.KimiClient;
import io.github.easy4j.kimi.KimiException;
import io.github.easy4j.kimi.KimiRuntimeClosedException;
import io.github.easy4j.kimi.cli.KimiCliResult;
import io.github.easy4j.kimi.model.KimiPromptRequest;
import io.github.easy4j.kimi.model.KimiPromptResult;

/**
 * One-shot CLI adapter for the common runtime contract.
 */
public final class KimiCliRuntime implements KimiRuntime {

    private static final KimiCapabilities CAPABILITIES = KimiCapabilities.of(
            KimiCapability.PROMPT,
            KimiCapability.SESSION_RESUME);

    private final KimiClient client;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public KimiCliRuntime(KimiClient client) {
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
            return client.isAvailable()
                    ? KimiHealth.healthy("kimi CLI available")
                    : KimiHealth.unhealthy("kimi CLI unavailable");
        } catch (RuntimeException e) {
            return KimiHealth.unhealthy("kimi CLI probe failed");
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
        Objects.requireNonNull(request, "request");
        KimiCliResult result = client.promptWithSession(request.getText(), request.getSessionId());
        if (!result.isSuccess()) {
            throw new KimiException("kimi CLI prompt failed with exit code "
                    + result.getExitCode() + ": " + result.getStderr());
        }
        return KimiPromptResult.of(request.getSessionId(), null, result.getStdout());
    }

    @Override
    public void cancel(String executionId) {
        requireOpen();
        CAPABILITIES.require(KimiCapability.CANCEL);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            client.close();
        }
    }

    private void requireOpen() {
        if (closed.get()) {
            throw new KimiRuntimeClosedException("kimi CLI runtime is closed");
        }
    }
}
