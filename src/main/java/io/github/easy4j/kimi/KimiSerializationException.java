/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi;

public class KimiSerializationException extends KimiException {
    public KimiSerializationException(String message) {
        super(KimiErrorCategory.SERIALIZATION, message);
    }
    public KimiSerializationException(String message, Throwable cause) {
        super(KimiErrorCategory.SERIALIZATION, message, cause);
    }
}
