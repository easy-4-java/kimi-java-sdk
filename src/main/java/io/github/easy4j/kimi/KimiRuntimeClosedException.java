/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi;

public class KimiRuntimeClosedException extends KimiException {

    public KimiRuntimeClosedException(String message) {
        super(KimiErrorCategory.RUNTIME_CLOSED, message);
    }

    public KimiRuntimeClosedException(String message, Throwable cause) {
        super(KimiErrorCategory.RUNTIME_CLOSED, message, cause);
    }
}
