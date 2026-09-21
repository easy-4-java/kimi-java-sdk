/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi;

public class KimiTimeoutException extends KimiException {

    public KimiTimeoutException(String message) {
        super(KimiErrorCategory.TIMEOUT, message);
    }

    public KimiTimeoutException(String message, Throwable cause) {
        super(KimiErrorCategory.TIMEOUT, message, cause);
    }
}
