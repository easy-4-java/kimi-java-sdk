/*
 * Copyright (c) 2018-present, easy-4-java.
 * Licensed under the Apache License, Version 2.0.
 */
package io.github.easy4j.kimi.runtime;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import io.github.easy4j.kimi.KimiUnsupportedCapabilityException;

/**
 * Immutable set of capabilities for one Kimi runtime.
 */
public final class KimiCapabilities {

    private final Set<KimiCapability> values;

    private KimiCapabilities(Set<KimiCapability> values) {
        this.values = Collections.unmodifiableSet(values);
    }

    public static KimiCapabilities of(KimiCapability... capabilities) {
        EnumSet<KimiCapability> set = EnumSet.noneOf(KimiCapability.class);
        if (capabilities != null) {
            for (KimiCapability capability : capabilities) {
                if (capability != null) {
                    set.add(capability);
                }
            }
        }
        return new KimiCapabilities(set);
    }

    public boolean supports(KimiCapability capability) {
        return capability != null && values.contains(capability);
    }

    public void require(KimiCapability capability) {
        if (!supports(capability)) {
            throw new KimiUnsupportedCapabilityException(capability);
        }
    }

    public Set<KimiCapability> asSet() {
        return values;
    }
}
