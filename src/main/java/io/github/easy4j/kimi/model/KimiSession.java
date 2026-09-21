/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import tools.jackson.databind.JsonNode;

public final class KimiSession {
    private final String id;
    private final String model;
    private final String mode;
    private final Map<String, JsonNode> extensions;

    KimiSession(String id, String model, String mode, Map<String, JsonNode> extensions) {
        this.id = id;
        this.model = model;
        this.mode = mode;
        this.extensions = immutableExtensions(extensions);
    }

    public String getId() { return id; }
    public String getModel() { return model; }
    public String getMode() { return mode; }
    public Map<String, JsonNode> getExtensions() { return extensions; }

    static Map<String, JsonNode> immutableExtensions(Map<String, JsonNode> source) {
        Map<String, JsonNode> copy = new LinkedHashMap<String, JsonNode>();
        for (Map.Entry<String, JsonNode> entry : source.entrySet()) {
            copy.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().deepCopy());
        }
        return Collections.unmodifiableMap(copy);
    }
}
