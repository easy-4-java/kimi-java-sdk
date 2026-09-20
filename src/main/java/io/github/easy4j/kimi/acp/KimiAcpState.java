/*
 * Copyright (c) 2018-present, easy-4-java (https://github.com/easy-4-java).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package io.github.easy4j.kimi.acp;

/**
 * Observable lifecycle states of a {@link KimiAcpClient}.
 *
 * @since 2.0.x
 */
public enum KimiAcpState {
    NEW,
    CONNECTING,
    INITIALIZING,
    READY,
    CLOSING,
    CLOSED,
    FAILED
}
