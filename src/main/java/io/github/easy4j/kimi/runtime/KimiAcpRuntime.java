/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.runtime;

import java.util.Objects;

import io.github.easy4j.kimi.KimiRuntimeClosedException;
import io.github.easy4j.kimi.acp.KimiAcpClient;
import io.github.easy4j.kimi.acp.KimiAcpState;
import io.github.easy4j.kimi.model.KimiPromptRequest;
import io.github.easy4j.kimi.model.KimiPromptResult;

/**
 * ACP adapter for the common runtime contract.
 */
public final class KimiAcpRuntime implements KimiRuntime {

    private static final KimiCapabilities CAPABILITIES = KimiCapabilities.of(
            KimiCapability.PROMPT,
            KimiCapability.STREAMING,
            KimiCapability.SESSION,
            KimiCapability.SESSION_RESUME,
            KimiCapability.SESSION_FORK,
            KimiCapability.CANCEL,
            KimiCapability.MODEL_SWITCH,
            KimiCapability.MODE_SWITCH,
            KimiCapability.ACP);

    private final KimiAcpClient client;

    public KimiAcpRuntime(KimiAcpClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    public KimiCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public KimiHealth health() {
        KimiAcpState state = client.getState();
        if (state == KimiAcpState.READY) {
            return KimiHealth.healthy("ACP ready");
        }
        return KimiHealth.unhealthy("ACP state=" + state);
    }

    @Override
    public KimiRuntimeState state() {
        KimiAcpState state = client.getState();
        switch (state) {
            case NEW:
                return KimiRuntimeState.NEW;
            case CONNECTING:
            case INITIALIZING:
                return KimiRuntimeState.STARTING;
            case READY:
                return KimiRuntimeState.READY;
            case CLOSING:
                return KimiRuntimeState.CLOSING;
            case CLOSED:
                return KimiRuntimeState.CLOSED;
            case FAILED:
            default:
                return KimiRuntimeState.FAILED;
        }
    }

    @Override
    public KimiPromptResult prompt(KimiPromptRequest request) {
        requireOpen();
        CAPABILITIES.require(KimiCapability.PROMPT);
        return client.prompt(Objects.requireNonNull(request, "request"), null);
    }

    @Override
    public void cancel(String executionId) {
        requireOpen();
        CAPABILITIES.require(KimiCapability.CANCEL);
        client.cancel(executionId);
    }

    @Override
    public void close() {
        client.close();
    }

    private void requireOpen() {
        if (client.getState() == KimiAcpState.CLOSED) {
            throw new KimiRuntimeClosedException("kimi ACP runtime is closed");
        }
    }
}
