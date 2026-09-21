/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.model;

import java.util.Map;
import tools.jackson.databind.JsonNode;

public final class KimiToolCall {
    private final String id;
    private final String name;
    private final JsonNode arguments;
    private final Map<String, JsonNode> extensions;

    KimiToolCall(String id, String name, JsonNode arguments, Map<String, JsonNode> extensions) {
        this.id = id;
        this.name = name;
        this.arguments = arguments == null ? null : arguments.deepCopy();
        this.extensions = KimiSession.immutableExtensions(extensions);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public JsonNode getArguments() { return arguments == null ? null : arguments.deepCopy(); }
    public Map<String, JsonNode> getExtensions() { return extensions; }
}
