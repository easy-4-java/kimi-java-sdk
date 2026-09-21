/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.model;

import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;

public final class KimiError {
    private final JsonNode code;
    private final String message;
    private final JsonNode data;
    private final Map<String, JsonNode> extensions;

    KimiError(JsonNode code, String message, JsonNode data, Map<String, JsonNode> extensions) {
        this.code = code == null ? null : code.deepCopy();
        this.message = message;
        this.data = data == null ? null : data.deepCopy();
        this.extensions = KimiSession.immutableExtensions(extensions);
    }

    public JsonNode getCode() { return code == null ? null : code.deepCopy(); }
    public String getMessage() { return message; }
    public JsonNode getData() { return data == null ? null : data.deepCopy(); }
    public Map<String, JsonNode> getExtensions() { return extensions; }
}
