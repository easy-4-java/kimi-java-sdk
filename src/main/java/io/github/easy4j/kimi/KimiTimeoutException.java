/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi;

/**
 * Raised when a Kimi runtime operation exceeds its deadline.
 */
public class KimiTimeoutException extends KimiException {

    public KimiTimeoutException(String message) {
        super(message);
    }

    public KimiTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
