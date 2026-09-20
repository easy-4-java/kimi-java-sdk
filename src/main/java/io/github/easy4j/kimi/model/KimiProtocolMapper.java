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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

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
        if (json == null) {
            throw new NullPointerException("json");
        }
        try {
            JsonNode root = mapper.readTree(json);
            if (root == null || !root.isObject()) {
                throw new KimiException("Kimi message must be a JSON object");
            }

            String role = root.path("role").asText(null);
            List<KimiContentBlock> blocks = new ArrayList<KimiContentBlock>();
            JsonNode content = root.path("content");
            if (content.isArray()) {
                for (JsonNode node : content) {
                    blocks.add(readContentBlock(node));
                }
            }

            Map<String, JsonNode> extensions = new LinkedHashMap<String, JsonNode>();
            for (Map.Entry<String, JsonNode> entry : root.properties()) {
                String name = entry.getKey();
                if (!"role".equals(name) && !"content".equals(name)) {
                    JsonNode value = entry.getValue();
                    extensions.put(name, value == null ? null : value.deepCopy());
                }
            }
            return new KimiMessage(role, blocks, extensions);
        } catch (KimiException e) {
            throw e;
        } catch (Exception e) {
            throw new KimiException("Failed to parse Kimi message", e);
        }
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
