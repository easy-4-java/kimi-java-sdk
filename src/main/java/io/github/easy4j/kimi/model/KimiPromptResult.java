/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.model;

import java.util.Map;

import tools.jackson.databind.JsonNode;

public final class KimiPromptResult {
    private final String sessionId;
    private final KimiStopReason stopReason;
    private final String content;
    private final KimiUsage usage;
    private final Map<String, JsonNode> extensions;

    KimiPromptResult(String sessionId, KimiStopReason stopReason, String content,
            KimiUsage usage, Map<String, JsonNode> extensions) {
        this.sessionId = sessionId;
        this.stopReason = stopReason;
        this.content = content;
        this.usage = usage;
        this.extensions = KimiSession.immutableExtensions(extensions);
    }

    public String getSessionId() { return sessionId; }
    public KimiStopReason getStopReason() { return stopReason; }
    public String getContent() { return content; }
    public KimiUsage getUsage() { return usage; }
    public Map<String, JsonNode> getExtensions() { return extensions; }
}
