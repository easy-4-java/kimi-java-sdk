/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.model;

public final class KimiUsage {
    private final long inputTokens;
    private final long outputTokens;
    private final long totalTokens;

    KimiUsage(long inputTokens, long outputTokens, long totalTokens) {
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.totalTokens = totalTokens;
    }

    public long getInputTokens() { return inputTokens; }
    public long getOutputTokens() { return outputTokens; }
    public long getTotalTokens() { return totalTokens; }
}
