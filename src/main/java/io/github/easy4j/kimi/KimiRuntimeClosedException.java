/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi;

/**
 * Raised when an operation is attempted after a runtime is closed.
 */
public class KimiRuntimeClosedException extends KimiException {

    public KimiRuntimeClosedException(String message) {
        super(message);
    }

    public KimiRuntimeClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
