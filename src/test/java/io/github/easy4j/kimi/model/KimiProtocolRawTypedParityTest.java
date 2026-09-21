/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

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
    @Test
    void shouldParseDescriptorAndSessionModelsFromCallerOwnedRawNodes() throws Exception {
        com.fasterxml.jackson.databind.JsonNode ignored = null;
        // The fully-qualified marker above is intentionally absent from production use;
        // this test exercises every remaining raw-node overload on Jackson 3.
        JsonNode sessionRaw = JsonMapper.builder().build().readTree(
                "{\"sessionId\":\"s2\",\"model\":\"kimi-k2\",\"mode\":\"code\",\"future\":1}");
        JsonNode toolRaw = JsonMapper.builder().build().readTree(
                "{\"id\":\"call_2\",\"name\":\"search\",\"arguments\":{\"q\":\"x\"},\"future\":2}");
        JsonNode errorRaw = JsonMapper.builder().build().readTree(
                "{\"code\":40001,\"message\":\"bad\",\"data\":{\"x\":1},\"future\":3}");
        JsonNode modelRaw = JsonMapper.builder().build().readTree(
                "{\"id\":\"kimi-k2\",\"name\":\"Kimi K2\",\"future\":4}");
        JsonNode modeRaw = JsonMapper.builder().build().readTree(
                "{\"id\":\"code\",\"name\":\"Code\",\"future\":5}");
        JsonNode providerRaw = JsonMapper.builder().build().readTree(
                "{\"id\":\"moonshot\",\"name\":\"Moonshot\",\"future\":6}");

        KimiSession session = mapper.readSession(sessionRaw);
        KimiToolCall tool = mapper.readToolCall(toolRaw);
        KimiError error = mapper.readError(errorRaw);
        KimiModel model = mapper.readModel(modelRaw);
        KimiMode mode = mapper.readMode(modeRaw);
        KimiProvider provider = mapper.readProvider(providerRaw);

        assertEquals("s2", session.getId());
        assertEquals(1, session.getExtensions().get("future").asInt());
        assertEquals("search", tool.getName());
        assertEquals(2, tool.getExtensions().get("future").asInt());
        assertEquals(40001, error.getCode().asInt());
        assertEquals(3, error.getExtensions().get("future").asInt());
        assertEquals(4, model.getExtensions().get("future").asInt());
        assertEquals(5, mode.getExtensions().get("future").asInt());
        assertEquals(6, provider.getExtensions().get("future").asInt());

        sessionRaw.withObject("/").put("future", 99);
        toolRaw.withObject("/").put("future", 99);
        assertEquals(1, session.getExtensions().get("future").asInt());
        assertEquals(2, tool.getExtensions().get("future").asInt());
    }

}
