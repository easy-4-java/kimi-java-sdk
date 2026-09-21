/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

class KimiProtocolRawTypedParityTest {

    private final KimiProtocolMapper mapper = new KimiProtocolMapper();

    @Test
    void shouldParseTypedMessageFromCallerOwnedRawNodeWithoutMutatingIt() throws Exception {
        JsonNode raw = JsonMapper.builder().build().readTree(
                "{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"hello\"}],"
                        + "\"future\":{\"x\":1}}");

        KimiMessage typed = mapper.readMessage(raw);

        assertEquals(raw.path("role").asText(), typed.getRole());
        assertEquals(raw.path("content").get(0).path("text").asText(),
                ((KimiTextContentBlock) typed.getContent().get(0)).getText());
        assertEquals(raw.path("future").path("x").asInt(),
                typed.getExtensions().get("future").path("x").asInt());

        raw.withObject("/future").put("x", 99);
        assertEquals(1, typed.getExtensions().get("future").path("x").asInt(),
                "typed model must own a defensive snapshot of caller raw data");
    }

    @Test
    void shouldParseTypedPromptResultFromRawNodeWithSameFacts() throws Exception {
        JsonNode raw = JsonMapper.builder().build().readTree(
                "{\"sessionId\":\"s1\",\"stopReason\":\"end_turn\",\"content\":\"ok\","
                        + "\"usage\":{\"inputTokens\":2,\"outputTokens\":3,\"totalTokens\":5}}");

        KimiPromptResult typed = mapper.readPromptResult(raw);

        assertEquals(raw.path("sessionId").asText(), typed.getSessionId());
        assertEquals(raw.path("stopReason").asText(), typed.getStopReason().getValue());
        assertEquals(raw.path("content").asText(), typed.getContent());
        assertEquals(raw.path("usage").path("totalTokens").asLong(),
                typed.getUsage().getTotalTokens());
    }
}
