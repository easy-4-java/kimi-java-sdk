/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.acp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.easy4j.kimi.model.KimiPromptRequest;
import io.github.easy4j.kimi.model.KimiPromptResult;

class KimiAcpTypedProtocolE2ETest {

    private static final String FAKE_AGENT = Paths.get("src", "test", "resources", "fake-acp-agent.py")
            .toAbsolutePath().toString();

    private static KimiAcpConfig config() {
        KimiAcpConfig config = new KimiAcpConfig();
        config.setLocalExecutable("python3");
        config.setAcpSubcommand(null);
        config.setAcpArgs(new String[] {FAKE_AGENT});
        config.setConnectTimeoutMillis(10_000);
        config.setReadTimeoutMillis(10_000);
        return config;
    }

    @Test
    void shouldRunTypedPromptWithoutRemovingRawApi() {
        try (KimiAcpClient client = new KimiAcpClient(config())) {
            client.connect();
            String sessionId = client.newSession("/tmp");

            KimiPromptRequest request = KimiPromptRequest.builder()
                    .sessionId(sessionId)
                    .text("typed prompt")
                    .build();

            List<String> deltas = new ArrayList<String>();
            KimiPromptResult result = client.prompt(request, deltas::add);

            assertEquals(sessionId, result.getSessionId());
            assertEquals("end_turn", result.getStopReason().getValue());
            assertEquals("你好世界", result.getContent());
            assertEquals(2, deltas.size());

            assertNotNull(client.listSessions(),
                    "typed API must be additive; existing raw JsonNode API remains available");
        }
    }

    @Test
    void shouldRejectIncompleteTypedPromptRequest() {
        assertThrows(IllegalStateException.class,
                () -> KimiPromptRequest.builder().text("missing session").build());
        assertThrows(IllegalStateException.class,
                () -> KimiPromptRequest.builder().sessionId("s").build());
    }
}
