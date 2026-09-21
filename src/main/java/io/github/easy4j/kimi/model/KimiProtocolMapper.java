/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github.easy4j.kimi.KimiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Typed protocol mapper for stable Kimi domain models.
 *
 * <p>The mapper intentionally uses the JSON tree model at the transport
 * boundary so unknown fields and future content block kinds remain
 * observable instead of being discarded.</p>
 */
public final class KimiProtocolMapper {

    private final ObjectMapper mapper;

    public KimiProtocolMapper() {
        this.mapper = JsonMapper.builder().build();
    }

    public KimiMessage readMessage(String json) {
        return readMessage(readObject(json, "Kimi message"));
    }

    public KimiMessage readMessage(JsonNode root) {
        JsonNode object = requireObject(root, "Kimi message");
        String role = object.path("role").asText(null);
        List<KimiContentBlock> blocks = new ArrayList<KimiContentBlock>();
        JsonNode content = object.path("content");
        if (content.isArray()) {
            for (JsonNode node : content) {
                blocks.add(readContentBlock(node));
            }
        }
        return new KimiMessage(role, blocks, extensions(object, "role", "content"));
    }

    public KimiSession readSession(String json) {
        return readSession(readObject(json, "Kimi session"));
    }

    public KimiSession readSession(JsonNode root) {
        JsonNode object = requireObject(root, "Kimi session");
        Map<String, JsonNode> extensions = extensions(object, "sessionId", "session_id", "model", "mode");
        return new KimiSession(firstText(object, "sessionId", "session_id"),
                object.path("model").asText(null),
                object.path("mode").asText(null),
                extensions);
    }

    public KimiPromptResult readPromptResult(String json) {
        return readPromptResult(readObject(json, "Kimi prompt result"));
    }

    public KimiPromptResult readPromptResult(JsonNode root) {
        JsonNode object = requireObject(root, "Kimi prompt result");
        JsonNode usageNode = object.path("usage");
        KimiUsage usage = usageNode.isObject()
                ? new KimiUsage(usageNode.path("inputTokens").asLong(0L),
                        usageNode.path("outputTokens").asLong(0L),
                        usageNode.path("totalTokens").asLong(0L))
                : null;
        Map<String, JsonNode> extensions = extensions(object,
                "sessionId", "session_id", "stopReason", "stop_reason", "content", "usage");
        return new KimiPromptResult(firstText(object, "sessionId", "session_id"),
                KimiStopReason.of(firstText(object, "stopReason", "stop_reason")),
                object.path("content").asText(null),
                usage,
                extensions);
    }

    public KimiToolCall readToolCall(String json) {
        return readToolCall(readObject(json, "Kimi tool call"));
    }

    public KimiToolCall readToolCall(JsonNode root) {
        JsonNode object = requireObject(root, "Kimi tool call");
        return new KimiToolCall(object.path("id").asText(null),
                object.path("name").asText(null),
                object.path("arguments").deepCopy(),
                extensions(object, "id", "name", "arguments"));
    }

    public KimiError readError(String json) {
        return readError(readObject(json, "Kimi error"));
    }

    public KimiError readError(JsonNode root) {
        JsonNode object = requireObject(root, "Kimi error");
        JsonNode code = object.path("code").isMissingNode() ? null : object.path("code").deepCopy();
        JsonNode data = object.path("data").isMissingNode() ? null : object.path("data").deepCopy();
        return new KimiError(code,
                object.path("message").asText(null),
                data,
                extensions(object, "code", "message", "data"));
    }

    public KimiModel readModel(String json) {
        return readModel(readObject(json, "Kimi model"));
    }

    public KimiModel readModel(JsonNode root) {
        JsonNode object = requireObject(root, "Kimi model");
        return new KimiModel(object.path("id").asText(null),
                object.path("name").asText(null),
                extensions(object, "id", "name"));
    }

    public KimiMode readMode(String json) {
        return readMode(readObject(json, "Kimi mode"));
    }

    public KimiMode readMode(JsonNode root) {
        JsonNode object = requireObject(root, "Kimi mode");
        return new KimiMode(object.path("id").asText(null),
                object.path("name").asText(null),
                extensions(object, "id", "name"));
    }

    public KimiProvider readProvider(String json) {
        return readProvider(readObject(json, "Kimi provider"));
    }

    public KimiProvider readProvider(JsonNode root) {
        JsonNode object = requireObject(root, "Kimi provider");
        return new KimiProvider(object.path("id").asText(null),
                object.path("name").asText(null),
                extensions(object, "id", "name"));
    }

    private JsonNode requireObject(JsonNode root, String what) {
        if (root == null || !root.isObject()) {
            throw new KimiException(what + " must be a JSON object");
        }
        return root;
    }

    private JsonNode readObject(String json, String what) {
        if (json == null) {
            throw new NullPointerException("json");
        }
        try {
            JsonNode root = mapper.readTree(json);
            return requireObject(root, what);
        } catch (KimiException e) {
            throw e;
        } catch (Exception e) {
            throw new KimiException("Failed to parse " + what, e);
        }
    }

    private Map<String, JsonNode> extensions(JsonNode root, String... known) {
        Map<String, Boolean> knownNames = new LinkedHashMap<String, Boolean>();
        for (String name : known) {
            knownNames.put(name, Boolean.TRUE);
        }
        Map<String, JsonNode> result = new LinkedHashMap<String, JsonNode>();
        java.util.Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (!knownNames.containsKey(entry.getKey())) {
                JsonNode value = entry.getValue();
                result.put(entry.getKey(), value == null ? null : value.deepCopy());
            }
        }
        return result;
    }

    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            String value = node.path(name).asText(null);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private KimiContentBlock readContentBlock(JsonNode node) {
        if (node == null || !node.isObject()) {
            return new KimiUnknownContentBlock(null, node == null ? mapper.nullNode() : node);
        }
        String type = node.path("type").asText(null);
        if ("text".equals(type)) {
            return new KimiTextContentBlock(node.path("text").asText(null), node);
        }
        return new KimiUnknownContentBlock(type, node);
    }
}
