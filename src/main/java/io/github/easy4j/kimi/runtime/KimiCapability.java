/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.runtime;

/**
 * Stable runtime capabilities exposed by kimi-java-sdk.
 */
public enum KimiCapability {
    PROMPT,
    STREAMING,
    SESSION,
    SESSION_RESUME,
    SESSION_FORK,
    CANCEL,
    MODEL_SWITCH,
    MODE_SWITCH,
    TOOLS,
    SKILLS,
    MCP,
    PROVIDERS,
    ACP,
    SERVER,
    WEBSOCKET
}
