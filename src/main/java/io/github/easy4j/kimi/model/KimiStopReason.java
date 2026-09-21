/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.model;

public final class KimiStopReason {
    private final String value;

    private KimiStopReason(String value) {
        this.value = value;
    }

    public static KimiStopReason of(String value) {
        return new KimiStopReason(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
