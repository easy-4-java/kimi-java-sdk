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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.easy4j.kimi.KimiException;
import io.github.easy4j.kimi.model.KimiPromptRequest;
import io.github.easy4j.kimi.model.KimiPromptResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Client for the Kimi ACP route: spawns {@code kimi acp} as a child process
 * and drives it over JSON-RPC on stdio (newline-delimited JSON).
 *
 * <p>Lifecycle: {@link #connect()} performs the ACP {@code initialize}
 * handshake, then sessions are managed with {@link #newSession(String)},
 * {@link #loadSession(String)} / {@link #resumeSession(String)} /
 * {@link #forkSession(String)} / {@link #closeSession(String)} /
 * {@link #deleteSession(String)}. A turn is {@link #prompt(String, String,
 * Consumer)}: the agent streams {@code session/update} notifications
 * ({@code agent_message_chunk} deltas surface on the callback) and the
 * response completes the returned future with the accumulated content and the
 * agent's stop reason. {@link #cancel(String)} interrupts a running turn.</p>
 *
 * <p>The client is thread-safe: concurrent prompts on different sessions are
 * correlated by session id; each client owns exactly one {@code kimi acp}
 * child process. {@link #close()} terminates it.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see KimiAcpConfig
 * @see KimiAcpTurnResult
 */
public class KimiAcpClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KimiAcpClient.class);

    private final KimiAcpConfig config;
    private final ObjectMapper mapper =
            JsonMapper.builder().disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .build();
    private final Map<Long, CompletableFuture<JsonNode>> pendingRpcs = new ConcurrentHashMap<Long, CompletableFuture<JsonNode>>();
    private final Map<String, PromptStream> promptStreams = new ConcurrentHashMap<String, PromptStream>();
    private final AtomicLong rpcIds = new AtomicLong();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicReference<KimiAcpState> state =
            new AtomicReference<KimiAcpState>(KimiAcpState.NEW);
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "kimi-acp-timer");
        thread.setDaemon(true);
        return thread;
    });

    private volatile Process process;
    private volatile PrintWriter stdin;
    private volatile String agentVersion;
    private volatile String protocolVersion;

    /**
     * Creates a new client bound to the given configuration.
     *
     * @param config runtime configuration; must not be {@code null}.
     */
    public KimiAcpClient(KimiAcpConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.config.validate();
    }

    /**
     * Spawns the {@code kimi acp} child process and performs the ACP
     * {@code initialize} handshake.
     *
     * @return the agent-reported version string, may be {@code null}.
     * @throws KimiException when the process fails to start or the handshake
     *                        fails or times out.
     */
    public String connect() {
        if (closed.get()) {
            throw new IllegalStateException("kimi acp client is closed");
        }
        // CAS guard: a second connect would orphan the first child process.
        if (!connected.compareAndSet(false, true)) {
            throw new IllegalStateException("kimi acp client is already connected");
        }
        state.set(KimiAcpState.CONNECTING);
        List<String> command = new ArrayList<String>();
        command.add(config.getLocalExecutable());
        if (config.getAcpSubcommand() != null) {
            command.add(config.getAcpSubcommand());
        }
        if (config.getAcpArgs() != null) {
            for (String arg : config.getAcpArgs()) {
                command.add(arg);
            }
        }
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(false);
        try {
            process = builder.start();
            state.set(KimiAcpState.INITIALIZING);
        } catch (IOException e) {
            connected.set(false);
            state.set(KimiAcpState.NEW);
            throw new KimiException("Failed to spawn kimi acp: " + config.getLocalExecutable(), e);
        }
        stdin = new PrintWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8), true);
        Thread reader = new Thread(this::readLoop, "kimi-acp-reader");
        reader.setDaemon(true);
        reader.start();

        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("protocolVersion", Integer.valueOf(1));
        Map<String, Object> clientCaps = new LinkedHashMap<String, Object>();
        params.put("clientCapabilities", clientCaps);
        try {
            JsonNode result = await(request("initialize", params), config.getConnectTimeoutMillis(), "initialize");
            if (result.hasNonNull("protocolVersion")) {
                protocolVersion = result.path("protocolVersion").asText(null);
            }
            if (result.hasNonNull("agentInfo")) {
                agentVersion = result.path("agentInfo").path("version").asText(null);
            }
            state.set(KimiAcpState.READY);
            return agentVersion;
        } catch (RuntimeException e) {
            // Handshake failure leaves the child alive — destroy it here so a
            // discarded client cannot leak the process, and allow a retry.
            Process current = process;
            if (current != null) {
                current.destroy();
            }
            process = null;
            stdin = null;
            connected.set(false);
            state.set(KimiAcpState.NEW);
            throw e;
        }
    }

    /**
     * Authenticates via the {@code login} auth method.
     *
     * @return the raw JSON-RPC result node; never {@code null}.
     */
    public JsonNode authenticate() {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("methodId", "login");
        return await(request("authenticate", params), config.getConnectTimeoutMillis(), "authenticate");
    }

    /**
     * Discards the managed provider token.
     *
     * @return the raw JSON-RPC result node; never {@code null}.
     */
    public JsonNode logout() {
        return await(request("logout", new LinkedHashMap<String, Object>()), config.getConnectTimeoutMillis(), "logout");
    }

    /**
     * Creates a new session.
     *
     * @param cwd working directory for the session; may be {@code null}.
     * @return the new session id; never {@code null}.
     */
    public String newSession(String cwd) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        if (cwd != null) {
            params.put("cwd", cwd);
        }
        params.put("mcpServers", new ArrayList<Object>());
        JsonNode result = await(request("session/new", params), config.getConnectTimeoutMillis(), "session/new");
        String sessionId = firstText(result, "sessionId", "session_id");
        if (sessionId == null || sessionId.isEmpty()) {
            throw new KimiException("kimi acp session/new returned no sessionId");
        }
        return sessionId;
    }

    /**
     * Loads a disk session, replaying its history as session/update
     * notifications before responding.
     *
     * @param sessionId the session id to load.
     * @return the raw JSON-RPC result node; never {@code null}.
     */
    public JsonNode loadSession(String sessionId) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("sessionId", sessionId);
        params.put("mcpServers", new ArrayList<Object>());
        return await(request("session/load", params), config.getConnectTimeoutMillis(), "session/load");
    }

    /**
     * Resumes a session without history replay.
     *
     * @param sessionId the session id to resume.
     * @return the raw JSON-RPC result node; never {@code null}.
     */
    public JsonNode resumeSession(String sessionId) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("sessionId", sessionId);
        return await(request("session/resume", params), config.getConnectTimeoutMillis(), "session/resume");
    }

    /**
     * Enumerates sessions known to the agent.
     *
     * @return the raw JSON-RPC result node; never {@code null}.
     */
    public JsonNode listSessions() {
        return await(request("session/list", new LinkedHashMap<String, Object>()),
                config.getConnectTimeoutMillis(), "session/list");
    }

    /**
     * Forks a session into a new one.
     *
     * @param sessionId the session id to fork.
     * @return the new session id; never {@code null}.
     */
    public String forkSession(String sessionId) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("sessionId", sessionId);
        JsonNode result = await(request("session/fork", params), config.getConnectTimeoutMillis(), "session/fork");
        String newId = firstText(result, "sessionId", "session_id");
        if (newId == null || newId.isEmpty()) {
            throw new KimiException("kimi acp session/fork returned no sessionId");
        }
        return newId;
    }

    /**
     * Disposes a session (best effort; unknown ids are not an error).
     *
     * @param sessionId the session id to close.
     */
    public void closeSession(String sessionId) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("sessionId", sessionId);
        await(request("session/close", params), config.getConnectTimeoutMillis(), "session/close");
    }

    /**
     * Permanently deletes a session.
     *
     * @param sessionId the session id to delete.
     */
    public void deleteSession(String sessionId) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("sessionId", sessionId);
        await(request("session/delete", params), config.getConnectTimeoutMillis(), "session/delete");
    }

    /**
     * Runs one prompt turn asynchronously.
     *
     * @param sessionId the session to prompt.
     * @param text      the text prompt.
     * @param onDelta   optional streaming callback invoked serially per
     *                  {@code agent_message_chunk} text.
     * @return a future completed with the turn result, or completed
     *         exceptionally with a {@link KimiException}.
     */
    public CompletableFuture<KimiAcpTurnResult> promptAsync(String sessionId, String text, Consumer<String> onDelta) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(text, "text");
        if (closed.get()) {
            throw new IllegalStateException("kimi acp client is closed");
        }
        PromptStream stream = new PromptStream(sessionId, onDelta);
        PromptStream active = promptStreams.putIfAbsent(sessionId, stream);
        if (active != null) {
            throw new KimiException("kimi acp session already has an active prompt: " + sessionId);
        }
        Map<String, Object> content = new LinkedHashMap<String, Object>();
        content.put("type", "text");
        content.put("text", text);
        List<Object> blocks = new ArrayList<Object>();
        blocks.add(content);
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("sessionId", sessionId);
        params.put("prompt", blocks);
        final CompletableFuture<JsonNode> response;
        try {
            response = request("session/prompt", params);
        } catch (RuntimeException e) {
            promptStreams.remove(sessionId, stream);
            throw e;
        }
        CompletableFuture<KimiAcpTurnResult> future = response.thenApply(node -> {
            RuntimeException callbackFailure = stream.callbackFailure();
            if (callbackFailure != null) {
                throw new KimiException("kimi acp prompt callback failed", callbackFailure);
            }
            String stopReason = firstText(node, "stopReason", "stop_reason");
            return new KimiAcpTurnResult(sessionId, stopReason, stream.content());
        });
        stream.bind(future);
        scheduleTimeout(future, config.getReadTimeoutMillis(), "session/prompt turn");
        future.whenComplete((r, error) -> {
            promptStreams.remove(sessionId, stream);
            if (error != null) {
                response.completeExceptionally(error);
            }
        });
        return future;
    }

    /**
     * Runs one typed prompt turn asynchronously while preserving the legacy
     * string-based ACP API.
     */
    public CompletableFuture<KimiPromptResult> promptAsync(
            KimiPromptRequest request, Consumer<String> onDelta) {
        Objects.requireNonNull(request, "request");
        return promptAsync(request.getSessionId(), request.getText(), onDelta)
                .thenApply(result -> KimiPromptResult.of(
                        result.getSessionId(), result.getStopReason(), result.getContent()));
    }

    /**
     * Runs one typed prompt turn while preserving the legacy string-based ACP API.
     */
    public KimiPromptResult prompt(KimiPromptRequest request, Consumer<String> onDelta) {
        Objects.requireNonNull(request, "request");
        KimiAcpTurnResult result = prompt(request.getSessionId(), request.getText(), onDelta);
        return KimiPromptResult.of(
                result.getSessionId(), result.getStopReason(), result.getContent());
    }

    /**
     * Runs one prompt turn, blocking until it completes.
     *
     * @param sessionId the session to prompt.
     * @param text      the text prompt.
     * @param onDelta   optional streaming callback.
     * @return the turn result; never {@code null}.
     * @throws KimiException when the turn fails or times out.
     */
    public KimiAcpTurnResult prompt(String sessionId, String text, Consumer<String> onDelta) {
        try {
            return promptAsync(sessionId, text, onDelta).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KimiException("kimi acp prompt interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof KimiException) {
                throw (KimiException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new KimiException("kimi acp prompt failed", cause);
        }
    }

    /**
     * Interrupts the running turn of a session.
     *
     * @param sessionId the session to cancel.
     */
    public void cancel(String sessionId) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("sessionId", sessionId);
        notify("session/cancel", params);
        PromptStream stream = promptStreams.get(sessionId);
        if (stream != null) {
            stream.cancel();
        }
    }

    /**
     * Sets the model of a session ({@code session/set_model}).
     *
     * @param sessionId the session to adjust.
     * @param modelId   the model identifier.
     * @return the raw JSON-RPC result node; never {@code null}.
     */
    public JsonNode setModel(String sessionId, String modelId) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("sessionId", sessionId);
        params.put("modelId", modelId);
        return await(request("session/set_model", params), config.getConnectTimeoutMillis(), "session/set_model");
    }

    /**
     * Sets the collaboration mode of a session ({@code session/set_mode}).
     *
     * @param sessionId the session to adjust.
     * @param modeId    the mode identifier.
     * @return the raw JSON-RPC result node; never {@code null}.
     */
    public JsonNode setMode(String sessionId, String modeId) {
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put("sessionId", sessionId);
        params.put("modeId", modeId);
        return await(request("session/set_mode", params), config.getConnectTimeoutMillis(), "session/set_mode");
    }

    /**
     * Returns the agent version reported by {@code initialize}, or
     * {@code null} before {@link #connect()}.
     *
     * @return the agent version, may be {@code null}.
     */
    public String getAgentVersion() {
        return agentVersion;
    }

    /**
     * Returns the negotiated ACP protocol version, or {@code null} before
     * {@link #connect()}.
     *
     * @return the protocol version, may be {@code null}.
     */
    public String getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * Returns whether the client has been closed.
     *
     * @return {@code true} after {@link #close()}.
     */
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * Returns the current ACP lifecycle state.
     *
     * @return the lifecycle state; never {@code null}.
     */
    public KimiAcpState getState() {
        return state.get();
    }

    /**
     * Terminates the {@code kimi acp} child process and releases the timer.
     * Idempotent.
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        state.set(KimiAcpState.CLOSING);
        connected.set(false);
        timer.shutdownNow();
        PrintWriter writer = stdin;
        stdin = null;
        if (writer != null) {
            writer.close();
        }
        Process current = process;
        process = null;
        if (current != null) {
            current.destroy();
            // A child that ignores SIGTERM must not outlive the client —
            // escalate to destroyForcibly (SIGKILL) after a short grace period.
            try {
                if (!current.waitFor(500, TimeUnit.MILLISECONDS)) {
                    current.destroyForcibly();
                }
            } catch (InterruptedException e) {
                current.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
        failAllPending(new KimiException("kimi acp client closed"));
        state.set(KimiAcpState.CLOSED);
    }

    // ============================================================
    // transport internals
    // ============================================================

    private CompletableFuture<JsonNode> request(String method, Map<String, Object> params) {
        KimiAcpState currentState = state.get();
        if (!"initialize".equals(method) && currentState != KimiAcpState.READY) {
            throw new KimiException("kimi acp client is not ready: state=" + currentState);
        }
        long id = rpcIds.incrementAndGet();
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("jsonrpc", "2.0");
        payload.put("id", Long.valueOf(id));
        payload.put("method", method);
        payload.put("params", params);
        CompletableFuture<JsonNode> future = new CompletableFuture<JsonNode>();
        Long rpcId = Long.valueOf(id);
        pendingRpcs.put(rpcId, future);
        future.whenComplete((result, error) -> pendingRpcs.remove(rpcId, future));
        try {
            writeJson(payload);
        } catch (RuntimeException e) {
            pendingRpcs.remove(rpcId, future);
            future.completeExceptionally(e);
            throw e;
        }
        return future;
    }

    private void notify(String method, Map<String, Object> params) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("jsonrpc", "2.0");
        payload.put("method", method);
        payload.put("params", params);
        writeJson(payload);
    }

    private void writeJson(Map<String, Object> payload) {
        if (closed.get()) {
            throw new KimiException("kimi acp client is closed");
        }
        String line;
        try {
            line = mapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new KimiException("kimi acp RPC serialization failed", e);
        }
        PrintWriter writer = stdin;
        if (writer == null) {
            throw new KimiException("kimi acp client is not connected");
        }
        synchronized (this) {
            writer.println(line);
            if (writer.checkError()) {
                throw new KimiException("kimi acp stdin write failed (child exited?)");
            }
        }
    }

    private void readLoop() {
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (closed.get()) {
                    return;
                }
                if (line.trim().isEmpty()) {
                    continue;
                }
                if (line.length() > effectiveMaxFrameChars()) {
                    KimiException error = new KimiException(
                            "kimi acp frame exceeded maxFrameChars=" + config.getMaxFrameChars());
                    log.warn("kimi acp frame over cap, tearing transport down");
                    failTransport(error);
                    process.destroy();
                    return;
                }
                handleFrame(line);
            }
            if (!closed.get()) {
                failTransport(new KimiException("kimi acp stdout closed (child exited)"));
            }
        } catch (IOException e) {
            if (!closed.get()) {
                failTransport(new KimiException("kimi acp stdout read failed", e));
            }
        } catch (KimiException e) {
            if (!closed.get()) {
                failTransport(e);
                Process current = process;
                if (current != null) {
                    current.destroy();
                }
            }
        } finally {
            connected.set(false);
        }
    }

    private void handleFrame(String frame) {
        JsonNode node;
        try {
            node = mapper.readTree(frame);
        } catch (Exception ex) {
            throw new KimiException("kimi acp protocol received invalid JSON frame", ex);
        }
        if (node.hasNonNull("id")) {
            CompletableFuture<JsonNode> pending = pendingRpcs.remove(Long.valueOf(node.get("id").asLong()));
            if (pending == null) {
                return;
            }
            if (node.hasNonNull("error")) {
                pending.completeExceptionally(new KimiException(
                        "kimi acp RPC failed: " + node.get("error").toString()));
            } else {
                pending.complete(node.path("result"));
            }
            return;
        }
        String method = node.path("method").asText("");
        JsonNode params = node.path("params");
        if ("session/update".equals(method)) {
            onSessionUpdate(params);
            return;
        }
        log.debug("Ignored kimi acp notification: method={}", method);
    }

    private void onSessionUpdate(JsonNode params) {
        String sessionId = firstText(params, "sessionId", "session_id");
        PromptStream stream = sessionId == null ? null : promptStreams.get(sessionId);
        if (stream == null) {
            return;
        }
        JsonNode update = params.path("update");
        String kind = update.path("sessionUpdate").asText(update.path("session_update").asText(""));
        if (!"agent_message_chunk".equals(kind) && !"agent_message".equals(kind)) {
            log.debug("Ignored kimi acp update kind={}", kind);
            return;
        }
        JsonNode content = update.path("content");
        String text = content.path("text").asText(update.path("text").asText(""));
        if (!text.isEmpty()) {
            stream.append(text);
        }
    }

    private void scheduleTimeout(CompletableFuture<?> future, long timeoutMillis, String what) {
        if (timeoutMillis <= 0) {
            return;
        }
        java.util.concurrent.ScheduledFuture<?> guard = timer.schedule(() -> future
                .completeExceptionally(new KimiException("kimi acp " + what + " timed out after "
                        + timeoutMillis + " ms")), timeoutMillis, TimeUnit.MILLISECONDS);
        future.whenComplete((r, error) -> guard.cancel(false));
    }

    private JsonNode await(CompletableFuture<JsonNode> future, long timeoutMillis, String what) {
        scheduleTimeout(future, timeoutMillis, what);
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KimiException("kimi acp " + what + " interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof KimiException) {
                throw (KimiException) cause;
            }
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new KimiException("kimi acp " + what + " failed", cause);
        }
    }

    private void failTransport(KimiException error) {
        if (!closed.get()) {
            state.set(KimiAcpState.FAILED);
        }
        connected.set(false);
        failAllPending(error);
    }

    private void failAllPending(KimiException error) {
        for (Map.Entry<Long, CompletableFuture<JsonNode>> entry : pendingRpcs.entrySet()) {
            CompletableFuture<JsonNode> future = pendingRpcs.remove(entry.getKey());
            if (future != null) {
                future.completeExceptionally(error);
            }
        }
        for (Map.Entry<String, PromptStream> entry : promptStreams.entrySet()) {
            // Streams are cleaned by their own prompt futures; nothing else to
            // release here.
            log.debug("kimi acp prompt stream orphaned: session={}", entry.getKey());
        }
        promptStreams.clear();
    }

    private int effectiveMaxFrameChars() {
        return config.getMaxFrameChars() <= 0 ? Integer.MAX_VALUE : config.getMaxFrameChars();
    }

    private String firstText(JsonNode node, String... fields) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        for (String field : fields) {
            String value = node.path(field).asText(null);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    /**
     * Per-turn accumulator: bounded agent-message content plus the optional
     * streaming callback. Callbacks run on the reader thread, serially and in
     * arrival order.
     */
    private final class PromptStream {

        private final String sessionId;
        private final StringBuilder content = new StringBuilder();
        private final Consumer<String> onDelta;
        private boolean truncationWarned;
        private volatile RuntimeException callbackFailure;
        private volatile CompletableFuture<KimiAcpTurnResult> turnFuture;

        PromptStream(String sessionId, Consumer<String> onDelta) {
            this.sessionId = sessionId;
            this.onDelta = onDelta;
        }

        void bind(CompletableFuture<KimiAcpTurnResult> future) {
            this.turnFuture = future;
        }

        void cancel() {
            CompletableFuture<KimiAcpTurnResult> future = turnFuture;
            if (future != null) {
                future.completeExceptionally(
                        new KimiException("kimi acp prompt cancelled: " + sessionId));
            }
        }

        void append(String text) {
            int cap = config.getMaxContentChars();
            String applied = text;
            if (cap > 0) {
                int remaining = cap - content.length();
                if (remaining <= 0) {
                    return;
                }
                if (text.length() > remaining) {
                    applied = text.substring(0, remaining);
                    if (!truncationWarned) {
                        truncationWarned = true;
                        log.warn("kimi acp turn content truncated at maxContentChars={}", cap);
                    }
                }
            }
            content.append(applied);
            if (!applied.isEmpty() && onDelta != null && callbackFailure == null) {
                try {
                    onDelta.accept(applied);
                } catch (RuntimeException e) {
                    callbackFailure = e;
                    log.warn("kimi acp prompt callback failed; transport remains active");
                }
            }
        }

        RuntimeException callbackFailure() {
            return callbackFailure;
        }

        String content() {
            return content.toString();
        }
    }
}
