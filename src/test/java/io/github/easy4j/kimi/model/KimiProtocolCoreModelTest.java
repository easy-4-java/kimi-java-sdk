/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class KimiProtocolCoreModelTest {

    private final KimiProtocolMapper mapper = new KimiProtocolMapper();

    @Test
    void shouldParseSessionAndPreserveFutureFields() {
        KimiSession session = mapper.readSession(
                "{\"sessionId\":\"sess_1\",\"model\":\"kimi-k2\",\"mode\":\"code\","
                        + "\"future_session\":{\"branch\":\"main\"}}");

        assertEquals("sess_1", session.getId());
        assertEquals("kimi-k2", session.getModel());
        assertEquals("code", session.getMode());
        assertEquals("main",
                session.getExtensions().get("future_session").path("branch").asText());
    }

    @Test
    void shouldParsePromptResultWithUsageAndUnknownStopReason() {
        KimiPromptResult result = mapper.readPromptResult(
                "{\"sessionId\":\"sess_1\",\"stopReason\":\"future_reason\","
                        + "\"content\":\"done\","
                        + "\"usage\":{\"inputTokens\":12,\"outputTokens\":7,\"totalTokens\":19},"
                        + "\"future_result\":true}");

        assertEquals("sess_1", result.getSessionId());
        assertEquals("future_reason", result.getStopReason().getValue());
        assertEquals("done", result.getContent());
        assertEquals(12L, result.getUsage().getInputTokens());
        assertEquals(7L, result.getUsage().getOutputTokens());
        assertEquals(19L, result.getUsage().getTotalTokens());
        assertEquals(true, result.getExtensions().get("future_result").asBoolean());
    }

    @Test
    void shouldKeepPromptResultImmutableAndDefensive() {
        KimiPromptResult result = mapper.readPromptResult(
                "{\"sessionId\":\"sess_1\",\"stopReason\":\"end_turn\","
                        + "\"content\":\"done\",\"future\":{\"x\":1}}");

        assertNotNull(result.getExtensions());
        assertThrows(UnsupportedOperationException.class,
                () -> result.getExtensions().put("mutate", result.getExtensions().get("future")));

        result.getExtensions().get("future").deepCopy().withObject("/").put("x", 99);
        assertEquals(1, result.getExtensions().get("future").path("x").asInt());
    }
}
