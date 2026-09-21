/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi;

public class KimiServerException extends KimiException {
    public KimiServerException(String message) {
        super(KimiErrorCategory.SERVER, message);
    }
    public KimiServerException(String message, Throwable cause) {
        super(KimiErrorCategory.SERVER, message, cause);
    }
}
