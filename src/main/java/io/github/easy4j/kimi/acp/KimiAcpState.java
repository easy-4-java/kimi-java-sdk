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
package io.github.easy4j.kimi.acp;

/**
 * Observable lifecycle states of a {@link KimiAcpClient}.
 *
 * @since 1.0.0
 */
public enum KimiAcpState {

    /** Client exists but does not own an ACP transport yet. */
    NEW,

    /** The child process is being created. */
    CONNECTING,

    /** The child process exists and the ACP initialize handshake is pending. */
    INITIALIZING,

    /** ACP initialize completed successfully and requests may be submitted. */
    READY,

    /** A terminal transport/protocol failure made the connection unusable. */
    FAILED,

    /** Close has started and new work is rejected. */
    CLOSING,

    /** All owned resources have been released. */
    CLOSED
}
