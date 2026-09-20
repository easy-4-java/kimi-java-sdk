/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.model;

import java.util.Objects;

import tools.jackson.databind.JsonNode;

/**
 * Typed text content block.
 */
public final class KimiTextContentBlock implements KimiContentBlock {

    private final String text;
    private final JsonNode raw;

    KimiTextContentBlock(String text, JsonNode raw) {
        this.text = text;
        this.raw = Objects.requireNonNull(raw, "raw").deepCopy();
    }

    @Override
    public String getType() {
        return "text";
    }

    public String getText() {
        return text;
    }

    @Override
    public JsonNode getRaw() {
        return raw.deepCopy();
    }
}
