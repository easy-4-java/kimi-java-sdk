/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.model;

import java.util.Objects;

import tools.jackson.databind.JsonNode;

/**
 * Forward-compatible content block for an unknown discriminator.
 */
public final class KimiUnknownContentBlock implements KimiContentBlock {

    private final String type;
    private final JsonNode raw;

    KimiUnknownContentBlock(String type, JsonNode raw) {
        this.type = type;
        this.raw = Objects.requireNonNull(raw, "raw").deepCopy();
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public JsonNode getRaw() {
        return raw.deepCopy();
    }
}
