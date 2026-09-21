/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class KimiProtocolDescriptorModelTest {

    private final KimiProtocolMapper mapper = new KimiProtocolMapper();

    @Test
    void shouldParseToolCallAndKeepRawArguments() {
        KimiToolCall tool = mapper.readToolCall(
                "{\"id\":\"call_1\",\"name\":\"search\","
                        + "\"arguments\":{\"q\":\"kimi\"},\"future\":123}");

        assertEquals("call_1", tool.getId());
        assertEquals("search", tool.getName());
        assertEquals("kimi", tool.getArguments().path("q").asText());
        assertEquals(123, tool.getExtensions().get("future").asInt());
    }

    @Test
    void shouldParseStructuredErrorWithoutDroppingData() {
        KimiError error = mapper.readError(
                "{\"code\":-32601,\"message\":\"method not found\","
                        + "\"data\":{\"method\":\"x\"},\"future_error\":true}");

        assertEquals(-32601, error.getCode().asInt());
        assertEquals("method not found", error.getMessage());
        assertEquals("x", error.getData().path("method").asText());
        assertEquals(true, error.getExtensions().get("future_error").asBoolean());
    }

    @Test
    void shouldParseModelModeAndProviderDescriptors() {
        KimiModel model = mapper.readModel(
                "{\"id\":\"kimi-k2\",\"name\":\"Kimi K2\",\"contextWindow\":131072}");
        KimiMode mode = mapper.readMode(
                "{\"id\":\"code\",\"name\":\"Code\",\"experimental\":true}");
        KimiProvider provider = mapper.readProvider(
                "{\"id\":\"moonshot\",\"name\":\"Moonshot\",\"region\":\"global\"}");

        assertEquals("kimi-k2", model.getId());
        assertEquals("Kimi K2", model.getName());
        assertEquals(131072, model.getExtensions().get("contextWindow").asInt());

        assertEquals("code", mode.getId());
        assertEquals("Code", mode.getName());
        assertEquals(true, mode.getExtensions().get("experimental").asBoolean());

        assertEquals("moonshot", provider.getId());
        assertEquals("Moonshot", provider.getName());
        assertNotNull(provider.getExtensions().get("region"));
    }
}
