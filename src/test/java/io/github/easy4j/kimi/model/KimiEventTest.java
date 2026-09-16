/*
 * Copyright (c) 2018-present, easy-4-java (https://github.com/easy-4-java).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.easy4j.kimi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link KimiEvent}: JSON round-trips and tolerance of
 * unknown fields.
 *
 * @since 1.0.0
 */
class KimiEventTest {

    private final ObjectMapper mapper = new JsonMapper();

    @Test
    void shouldProvideSensibleDefaults() {
        KimiEvent event = new KimiEvent();

        assertNull(event.getType());
        assertNull(event.getContent());
        assertNull(event.getToolCalls());
    }

    @Test
    void shouldRoundTripJson() throws Exception {
        KimiEvent event = new KimiEvent();
        event.setType("assistant");
        event.setContent("hello");
        event.setToolCalls(Arrays.asList("t1"));

        String json = mapper.writeValueAsString(event);
        KimiEvent decoded = mapper.readValue(json, KimiEvent.class);

        assertEquals("assistant", decoded.getType());
        assertEquals("hello", decoded.getContent());
    }

    @Test
    void shouldTolerateUnknownFields() throws Exception {
        KimiEvent event = mapper.readValue(
                "{\"type\":\"tool\",\"content\":\"running\",\"tool_calls\":[],\"thinking\":\"internal\"}",
                KimiEvent.class);

        assertEquals("tool", event.getType());
        assertEquals("running", event.getContent());
    }

    @Test
    void shouldSkipNonJsonLinesGracefullyAtClientLevel() throws Exception {
        // The client drops unparseable lines; this test guards the model
        // against over-strict deserialization defaults.
        KimiEvent event = mapper.readValue("{\"type\":\"assistant\",\"unknown_field\":123}", KimiEvent.class);
        assertEquals("assistant", event.getType());
    }
}
