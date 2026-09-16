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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.easy4j.kimi.KimiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Client for the Kimi web-server route ({@code kimi web}).
 *
 * <p>Two usage modes:</p>
 * <ul>
 *   <li>{@link #start()} spawns {@code kimi web} as a child process, waits
 *       for {@code /api/v1/healthz}, reads the bearer token from
 *       {@code ~/.kimi-code/server.token} and owns the server lifecycle
 *       ({@link #close()} requests {@code /api/v1/shutdown} and destroys the
 *       process if needed).</li>
 *   <li>{@link #attach(String, String)} connects to an already-running
 *       server; the lifecycle is then owned externally.</li>
 * </ul>
 *
 * <p>The server answers with the envelope {@code {code, msg, data,
 * request_id}}; {@link #get}/{@link #post}/{@link #delete} unwrap it and
 * raise {@link KimiException} on a non-zero business code. Typed helpers
 * cover the core endpoints (meta, sessions, prompts, messages, abort);
 * anything else goes through the same generic methods.</p>
 *
 * <p>Out of scope for this client: the WebSocket event stream
 * ({@code /api/v1/ws}) &mdash; subscribe with your own WS stack using the
 * same bearer token.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see KimiServerConfig
 */
public class KimiServerClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KimiServerClient.class);
    private static final ObjectMapper MAPPER =
            JsonMapper.builder().disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build();

    private final KimiServerConfig config;
    private final AtomicBoolean ownsServer = new AtomicBoolean(false);

    private volatile Process process;
    private volatile String baseUrl;
    private volatile String token;

    /**
     * Creates a new server client bound to the given configuration.
     *
     * @param config runtime configuration; must not be {@code null}.
     */
    public KimiServerClient(KimiServerConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.config.validate();
    }

    /**
     * Spawns {@code kimi web}, waits for it to become healthy and reads the
     * bearer token from the configured token file.
     *
     * @return the base URL the server answered on.
     * @throws KimiException when the server does not become healthy in time.
     */
    public String start() {
        if (baseUrl != null) {
            throw new IllegalStateException("kimi server client already started/attached");
        }
        List<String> command = new java.util.ArrayList<String>();
        command.add(config.getLocalExecutable());
        command.add("web");
        command.add("--host");
        command.add(config.getHost());
        command.add("--port");
        command.add(String.valueOf(config.getPort()));
        command.add("--no-open");
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new KimiException("Failed to spawn kimi web: " + config.getLocalExecutable(), e);
        }
        ownsServer.set(true);
        // Drain child output so the process never blocks on a full pipe.
        Thread drainer = new Thread(() -> {
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                while (reader.readLine() != null) {
                    // discard
                }
            } catch (IOException e) {
                log.debug("kimi web output drain ended: {}", e.getMessage());
            }
        }, "kimi-web-drain");
        drainer.setDaemon(true);
        drainer.start();

        long deadline = System.currentTimeMillis() + config.getStartupTimeoutMillis();
        String candidate = "http://" + config.getHost() + ":" + config.getPort();
        Exception lastError = null;
        while (System.currentTimeMillis() < deadline) {
            if (!process.isAlive()) {
                throw new KimiException("kimi web exited during startup");
            }
            try {
                get(candidate, "/api/v1/healthz");
                baseUrl = candidate;
                token = readToken();
                return baseUrl;
            } catch (Exception e) {
                lastError = e;
                sleepQuietly(300);
            }
        }
        process.destroy();
        throw new KimiException("kimi web did not become healthy within "
                + config.getStartupTimeoutMillis() + " ms", lastError);
    }

    /**
     * Attaches to an already-running server.
     *
     * @param baseUrl server base URL (e.g. {@code http://127.0.0.1:58627}).
     * @param token   bearer token; may be {@code null} for unauthenticated endpoints.
     * @return the base URL.
     */
    public String attach(String baseUrl, String token) {
        Objects.requireNonNull(baseUrl, "baseUrl");
        String trimmed = baseUrl.trim().replaceAll("/+$", "");
        this.baseUrl = trimmed;
        this.token = token;
        return trimmed;
    }

    /**
     * Runs {@code GET /api/v1/meta}.
     *
     * @return the {@code data} payload; never {@code null}.
     */
    public JsonNode meta() {
        return get("/api/v1/meta");
    }

    /**
     * Runs {@code GET /api/v1/healthz} (unauthenticated).
     *
     * @return the {@code data} payload; never {@code null}.
     */
    public JsonNode healthz() {
        return get("/api/v1/healthz");
    }

    /**
     * Creates a session bound to a working directory
     * ({@code POST /api/v1/sessions}).
     *
     * @param cwd the workspace directory.
     * @return the {@code data} payload; never {@code null}.
     */
    public JsonNode createSession(String cwd) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("metadata", singletonMap("cwd", cwd));
        return post("/api/v1/sessions", body);
    }

    /**
     * Lists sessions ({@code GET /api/v1/sessions}).
     *
     * @return the {@code data} payload; never {@code null}.
     */
    public JsonNode listSessions() {
        return get("/api/v1/sessions");
    }

    /**
     * Submits a text prompt to a session
     * ({@code POST /api/v1/sessions/{id}/prompts}).
     *
     * @param sessionId target session.
     * @param text      prompt text.
     * @return the {@code data} payload; never {@code null}.
     */
    public JsonNode postPrompt(String sessionId, String text) {
        Map<String, Object> block = new LinkedHashMap<String, Object>();
        block.put("type", "text");
        block.put("text", text);
        java.util.List<Object> content = new java.util.ArrayList<Object>();
        content.add(block);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("content", content);
        return post("/api/v1/sessions/" + sessionId + "/prompts", body);
    }

    /**
     * Reads the messages of a session ({@code GET /api/v1/sessions/{id}/messages}).
     *
     * @param sessionId target session.
     * @return the {@code data} payload; never {@code null}.
     */
    public JsonNode messages(String sessionId) {
        return get("/api/v1/sessions/" + sessionId + "/messages");
    }

    /**
     * Aborts the running turn of a session
     * ({@code POST /api/v1/sessions/{id}:abort}).
     *
     * @param sessionId target session.
     * @return the {@code data} payload; never {@code null}.
     */
    public JsonNode abort(String sessionId) {
        return post("/api/v1/sessions/" + sessionId + ":abort", null);
    }

    /**
     * Performs an authenticated GET and unwraps the response envelope.
     *
     * @param path request path beginning with {@code /}.
     * @return the envelope {@code data} node; never {@code null}.
     */
    public JsonNode get(String path) {
        return exchange("GET", path, null);
    }

    /**
     * Performs an authenticated GET against an absolute URL (used during
     * startup before the base URL is pinned).
     *
     * @param baseUrl absolute base URL.
     * @param path    request path beginning with {@code /}.
     * @return the envelope {@code data} node; never {@code null}.
     */
    public JsonNode get(String baseUrl, String path) {
        return exchange(baseUrl, "GET", path, null);
    }

    /**
     * Performs an authenticated POST with a JSON body and unwraps the envelope.
     *
     * @param path request path beginning with {@code /}.
     * @param body POJO serialized as JSON; may be {@code null}.
     * @return the envelope {@code data} node; never {@code null}.
     */
    public JsonNode post(String path, Object body) {
        return exchange("POST", path, body);
    }

    /**
     * Performs an authenticated DELETE and unwraps the envelope.
     *
     * @param path request path beginning with {@code /}.
     * @return the envelope {@code data} node; never {@code null}.
     */
    public JsonNode delete(String path) {
        return exchange("DELETE", path, null);
    }

    /**
     * Stops the server this client started (graceful shutdown endpoint first,
     * then process destroy) or detaches from an attached one.
     */
    @Override
    public void close() {
        if (ownsServer.compareAndSet(true, false)) {
            try {
                post("/api/v1/shutdown", null);
            } catch (Exception e) {
                log.debug("kimi web shutdown endpoint failed: {}", e.getMessage());
            }
        }
        Process current = process;
        if (current != null) {
            current.destroy();
            process = null;
        }
        baseUrl = null;
        token = null;
    }

    // ============================================================
    // HTTP internals
    // ============================================================

    private JsonNode exchange(String method, String path, Object body) {
        String base = baseUrl;
        if (base == null) {
            throw new IllegalStateException("kimi server client not started/attached");
        }
        return exchange(base, method, path, body);
    }

    private JsonNode exchange(String base, String method, String path, Object body) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(base + path).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(config.getConnectTimeoutMillis());
            connection.setReadTimeout(config.getReadTimeoutMillis());
            String currentToken = token;
            if (currentToken != null && !"/api/v1/healthz".equals(path)) {
                connection.setRequestProperty("Authorization", "Bearer " + currentToken);
            }
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                byte[] bytes = MAPPER.writeValueAsBytes(body);
                connection.setFixedLengthStreamingMode(bytes.length);
                OutputStream out = connection.getOutputStream();
                out.write(bytes);
                out.flush();
                out.close();
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String payload = readAll(stream);
            JsonNode envelope;
            try {
                envelope = MAPPER.readTree(payload);
            } catch (Exception e) {
                throw new KimiException("kimi server " + method + " " + path + " returned non-JSON (HTTP " + status + ")");
            }
            int code = envelope.path("code").asInt(status == 200 ? 0 : status);
            if (code != 0) {
                throw new KimiException("kimi server " + method + " " + path + " failed: code=" + code
                        + " msg=" + envelope.path("msg").asText(""));
            }
            return envelope.path("data");
        } catch (IOException e) {
            throw new KimiException("kimi server " + method + " " + path + " I/O failed", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readToken() {
        String explicit = config.getToken();
        if (explicit != null && !explicit.trim().isEmpty()) {
            return explicit.trim();
        }
        Path path = Paths.get(config.getTokenPath());
        if (Files.isReadable(path)) {
            try {
                String value = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            } catch (IOException e) {
                log.debug("kimi server token unreadable at {}: {}", path, e.getMessage());
            }
        }
        log.warn("kimi server token not found at {}; unauthenticated calls will fail", config.getTokenPath());
        return null;
    }

    private String readAll(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                builder.append(buffer, 0, read);
            }
        }
        return builder.toString();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Map<String, Object> singletonMap(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put(key, value);
        return map;
    }
}
