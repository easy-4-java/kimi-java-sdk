/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.runtime;

/**
 * Common runtime lifecycle states.
 */
public enum KimiRuntimeState {
    NEW,
    STARTING,
    READY,
    CLOSING,
    CLOSED,
    FAILED
}
