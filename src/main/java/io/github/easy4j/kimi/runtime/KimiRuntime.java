/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.runtime;

import io.github.easy4j.kimi.model.KimiPromptRequest;
import io.github.easy4j.kimi.model.KimiPromptResult;

/**
 * Common Java-8-compatible contract for one Kimi runtime route.
 */
public interface KimiRuntime extends AutoCloseable {

    KimiCapabilities capabilities();

    KimiHealth health();

    KimiRuntimeState state();

    KimiPromptResult prompt(KimiPromptRequest request);

    void cancel(String executionId);

    @Override
    void close();
}
