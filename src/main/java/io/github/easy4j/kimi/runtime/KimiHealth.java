/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.runtime;

/**
 * Runtime health is intentionally separate from lifecycle state.
 */
public final class KimiHealth {

    private final boolean healthy;
    private final String summary;

    private KimiHealth(boolean healthy, String summary) {
        this.healthy = healthy;
        this.summary = summary;
    }

    public static KimiHealth healthy(String summary) {
        return new KimiHealth(true, summary);
    }

    public static KimiHealth unhealthy(String summary) {
        return new KimiHealth(false, summary);
    }

    public boolean isHealthy() {
        return healthy;
    }

    public String getSummary() {
        return summary;
    }
}
