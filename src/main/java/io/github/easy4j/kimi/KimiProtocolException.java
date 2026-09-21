/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi;

public class KimiProtocolException extends KimiException {
    public KimiProtocolException(String message) {
        super(KimiErrorCategory.PROTOCOL, message);
    }
    public KimiProtocolException(String message, Throwable cause) {
        super(KimiErrorCategory.PROTOCOL, message, cause);
    }
}
