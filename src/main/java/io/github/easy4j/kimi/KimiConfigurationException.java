/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi;

public class KimiConfigurationException extends KimiException {
    public KimiConfigurationException(String message) {
        super(KimiErrorCategory.CONFIGURATION, message);
    }
    public KimiConfigurationException(String message, Throwable cause) {
        super(KimiErrorCategory.CONFIGURATION, message, cause);
    }
}
