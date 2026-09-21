/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi;

public class KimiAuthenticationException extends KimiException {
    public KimiAuthenticationException(String message) {
        super(KimiErrorCategory.AUTHENTICATION, message);
    }
    public KimiAuthenticationException(String message, Throwable cause) {
        super(KimiErrorCategory.AUTHENTICATION, message, cause);
    }
}
