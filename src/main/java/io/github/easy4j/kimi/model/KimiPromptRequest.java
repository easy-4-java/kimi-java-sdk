/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.model;

/**
 * Immutable typed prompt request for an existing Kimi session.
 */
public final class KimiPromptRequest {

    private final String sessionId;
    private final String text;

    private KimiPromptRequest(Builder builder) {
        this.sessionId = builder.sessionId;
        this.text = builder.text;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getText() {
        return text;
    }

    public static final class Builder {
        private String sessionId;
        private String text;

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public KimiPromptRequest build() {
            if (sessionId == null || sessionId.trim().isEmpty()) {
                throw new IllegalStateException("sessionId is required");
            }
            if (text == null) {
                throw new IllegalStateException("text is required");
            }
            return new KimiPromptRequest(this);
        }
    }
}
