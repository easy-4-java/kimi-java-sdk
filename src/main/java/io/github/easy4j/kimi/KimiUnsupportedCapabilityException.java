/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi;

import io.github.easy4j.kimi.runtime.KimiCapability;

public class KimiUnsupportedCapabilityException extends KimiException {

    private final KimiCapability capability;

    public KimiUnsupportedCapabilityException(KimiCapability capability) {
        super(KimiErrorCategory.UNSUPPORTED_CAPABILITY,
                "kimi runtime does not support capability: " + capability);
        this.capability = capability;
    }

    public KimiCapability getCapability() {
        return capability;
    }
}
