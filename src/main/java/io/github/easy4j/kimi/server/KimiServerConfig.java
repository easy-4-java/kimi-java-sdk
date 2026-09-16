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
package io.github.easy4j.kimi.server;

import java.util.Objects;

import lombok.Data;

/**
 * Configuration for the Kimi web-server route ({@code kimi web}).
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see KimiServerClient
 */
@Data
public class KimiServerConfig {

    /** Name or absolute path of the local {@code kimi} executable. */
    private String localExecutable = "kimi";

    /** Bind host for a server started by this client; defaults to loopback. */
    private String host = "127.0.0.1";

    /** Bind port for a server started by this client; the CLI auto-retries +1 when busy. */
    private int port = 58627;

    /**
     * Bearer token for an already-running server. Leave {@code null} when
     * {@link KimiServerClient#start()} should read it from
     * {@link #getTokenPath()}.
     */
    private String token;

    /** Token file location the CLI persists its bearer token to. */
    private String tokenPath = System.getProperty("user.home") + "/.kimi-code/server.token";

    /** Base URL of an already-running server; overrides host/port when set. */
    private String baseUrl;

    /** Timeout in milliseconds for HTTP connects. */
    private int connectTimeoutMillis = 5_000;

    /** Timeout in milliseconds for HTTP reads. */
    private int readTimeoutMillis = 120_000;

    /** Upper bound in milliseconds to wait for a started server to become healthy. */
    private int startupTimeoutMillis = 30_000;

    /**
     * Validates the configuration.
     *
     * @throws NullPointerException when the executable is {@code null}.
     */
    public void validate() {
        Objects.requireNonNull(localExecutable, "localExecutable");
    }
}
