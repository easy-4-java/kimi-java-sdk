/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi;

/**
 * Stable high-level categories for Kimi SDK failures.
 */
public enum KimiErrorCategory {
    GENERAL,
    CONFIGURATION,
    PROCESS,
    TIMEOUT,
    PROTOCOL,
    RPC,
    AUTHENTICATION,
    SERVER,
    SERIALIZATION,
    RUNTIME_CLOSED,
    UNSUPPORTED_CAPABILITY
}
