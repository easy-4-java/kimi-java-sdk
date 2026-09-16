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

import java.util.Objects;

import lombok.Data;

/**
 * Configuration for the Kimi ACP route.
 *
 * <p>{@code kimi acp} speaks JSON-RPC over stdio (newline-delimited JSON).
 * The SDK spawns it as a child process and drives sessions, prompts and
 * streaming updates through that channel &mdash; no sockets, so this route
 * works on every JDK line the SDK supports.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see KimiAcpClient
 */
@Data
public class KimiAcpConfig {

    /** Name or absolute path of the local {@code kimi} executable. */
    private String localExecutable = "kimi";

    /**
     * Subcommand appended after the executable; {@code acp} by default. Set
     * to {@code null} when {@link #localExecutable} is already a complete
     * launch command (e.g. a test double).
     */
    private String acpSubcommand = "acp";

    /** Extra arguments forwarded after the {@code acp} subcommand. */
    private String[] acpArgs;

    /** Timeout in milliseconds for process startup plus the ACP {@code initialize} handshake. */
    private int connectTimeoutMillis = 10_000;

    /** Upper bound in milliseconds for one prompt turn ({@code session/prompt} → response). */
    private int readTimeoutMillis = 600_000;

    /**
     * Hard cap in characters for one JSON-RPC frame on the wire; a frame
     * exceeding it tears the transport down. {@code <= 0} means unbounded.
     * Defaults to 1&nbsp;MiB characters.
     */
    private int maxFrameChars = 1_048_576;

    /**
     * Hard cap in characters for the assistant message content accumulated
     * per prompt; excess chunk text is truncated with a warning. {@code <= 0}
     * means unbounded. Defaults to 1&nbsp;MiB characters.
     */
    private int maxContentChars = 1_048_576;

    /**
     * Validates the configuration.
     *
     * @throws NullPointerException when the executable is {@code null}.
     */
    public void validate() {
        Objects.requireNonNull(localExecutable, "localExecutable");
    }
}
