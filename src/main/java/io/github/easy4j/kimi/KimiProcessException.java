/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi;

public class KimiProcessException extends KimiException {
    public KimiProcessException(String message) {
        super(KimiErrorCategory.PROCESS, message);
    }
    public KimiProcessException(String message, Throwable cause) {
        super(KimiErrorCategory.PROCESS, message, cause);
    }
}
