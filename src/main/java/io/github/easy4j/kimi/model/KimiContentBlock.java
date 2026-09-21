/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Base type for typed Kimi message content blocks.
 */
public interface KimiContentBlock {

    String getType();

    /**
     * Returns a defensive raw representation of this block.
     */
    JsonNode getRaw();
}
