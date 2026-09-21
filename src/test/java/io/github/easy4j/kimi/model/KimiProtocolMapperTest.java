/*
 * Copyright (c) 2018-present, easy-4-java (https://github.com/easy-4-java).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package io.github.easy4j.kimi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class KimiProtocolMapperTest {

    private final KimiProtocolMapper mapper = new KimiProtocolMapper();

    @Test
    void shouldParseTextContentBlockAsTypedModel() {
        KimiMessage message = mapper.readMessage(
                "{\"role\":\"assistant\",\"content\":[{\"type\":\"text\",\"text\":\"hello\"}]}");

        assertEquals("assistant", message.getRole());
        assertEquals(1, message.getContent().size());

        KimiTextContentBlock block =
                assertInstanceOf(KimiTextContentBlock.class, message.getContent().get(0));
        assertEquals("text", block.getType());
        assertEquals("hello", block.getText());
    }

    @Test
    void shouldPreserveUnknownContentBlockRawPayload() {
        KimiMessage message = mapper.readMessage(
                "{\"role\":\"assistant\",\"content\":[{\"type\":\"future_block\","
                        + "\"nested\":{\"answer\":42},\"flags\":[true,false]}]}");

        KimiUnknownContentBlock block =
                assertInstanceOf(KimiUnknownContentBlock.class, message.getContent().get(0));

        assertEquals("future_block", block.getType());
        assertEquals(42, block.getRaw().path("nested").path("answer").asInt());
        assertEquals(true, block.getRaw().path("flags").get(0).asBoolean());
    }

    @Test
    void shouldPreserveUnknownMessageFields() {
        KimiMessage message = mapper.readMessage(
                "{\"role\":\"assistant\",\"content\":[],"
                        + "\"future_meta\":{\"trace\":\"abc\",\"count\":3}}");

        assertNotNull(message.getExtensions());
        assertEquals("abc",
                message.getExtensions().get("future_meta").path("trace").asText());
        assertEquals(3,
                message.getExtensions().get("future_meta").path("count").asInt());
    }
}
