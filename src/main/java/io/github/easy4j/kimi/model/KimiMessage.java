/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Immutable typed representation of a Kimi message.
 */
public final class KimiMessage {

    private final String role;
    private final List<KimiContentBlock> content;
    private final Map<String, JsonNode> extensions;

    KimiMessage(String role, List<KimiContentBlock> content, Map<String, JsonNode> extensions) {
        this.role = role;
        this.content = Collections.unmodifiableList(new ArrayList<KimiContentBlock>(content));
        Map<String, JsonNode> copied = new LinkedHashMap<String, JsonNode>();
        for (Map.Entry<String, JsonNode> entry : extensions.entrySet()) {
            copied.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().deepCopy());
        }
        this.extensions = Collections.unmodifiableMap(copied);
    }

    public String getRole() {
        return role;
    }

    public List<KimiContentBlock> getContent() {
        return content;
    }

    public Map<String, JsonNode> getExtensions() {
        return extensions;
    }
}
