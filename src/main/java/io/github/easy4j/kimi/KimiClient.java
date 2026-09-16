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
package io.github.easy4j.kimi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.easy4j.kimi.cli.KimiCli;
import io.github.easy4j.kimi.cli.KimiCliExecutor;
import io.github.easy4j.kimi.cli.KimiCliResult;
import io.github.easy4j.kimi.model.KimiEvent;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * High-level Java facade that wraps every local {@code kimi} CLI invocation
 * behind ergonomic, strongly-typed methods.
 *
 * <p>This class is the recommended entry point for the CLI route. It owns a
 * single {@link KimiClientConfig} and a single {@link KimiCli}, forwarding the
 * configured defaults to every call so callers only supply the call-specific
 * overrides.</p>
 *
 * <h3>Non-interactive runs</h3>
 * <p>{@link #prompt(String)} maps to {@code kimi --prompt <p>} (plain text on
 * stdout); {@link #promptJson(String)} adds {@code --output-format stream-json}
 * and {@link #promptAndParse(String)} additionally decodes the JSONL into
 * {@link KimiEvent}s.</p>
 *
 * <h3>Other routes</h3>
 * <p>For IDE-style long-lived sessions use
 * {@code io.github.easy4j.kimi.acp.KimiAcpClient} (JSON-RPC over stdio); for
 * the REST surface of {@code kimi web} use
 * {@code io.github.easy4j.kimi.server.KimiServerClient}.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see KimiClientConfig
 * @see KimiCli
 */
public class KimiClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KimiClient.class);
    private static final ObjectMapper MAPPER =
            JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    private final KimiClientConfig config;
    private final KimiCli cli;

    /**
     * Creates a new client backed by the given configuration. A default
     * {@link KimiCli} and {@link KimiCliExecutor} are constructed
     * automatically.
     *
     * @param config runtime configuration; must not be {@code null}.
     * @throws NullPointerException if {@code config} is {@code null}.
     */
    public KimiClient(KimiClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.config.validate();
        this.cli = new KimiCli(this.config, new KimiCliExecutor(this.config));
    }

    /**
     * Creates a new client that delegates to the supplied {@link KimiCli}.
     *
     * <p>This constructor exists primarily for testing &mdash; it lets a
     * caller substitute a {@link KimiCli} backed by a mocked executor while
     * still using the default behaviour of the surrounding facade.</p>
     *
     * @param config runtime configuration; must not be {@code null}.
     * @param cli    the CLI facade to delegate to; must not be {@code null}.
     * @throws NullPointerException if either argument is {@code null}.
     */
    public KimiClient(KimiClientConfig config, KimiCli cli) {
        this.config = Objects.requireNonNull(config, "config");
        this.cli = Objects.requireNonNull(cli, "cli");
    }

    // ============================================================
    // basic info
    // ============================================================

    /**
     * Runs {@code kimi --version}.
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult version() {
        return cli.version();
    }

    /**
     * Runs {@code kimi --help}.
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult help() {
        return cli.help();
    }

    /**
     * Probes CLI availability with {@code kimi --version} and the configured
     * probe timeout.
     *
     * @return {@code true} when the local CLI is reachable.
     */
    public boolean isAvailable() {
        return cli.executor().probe();
    }

    // ============================================================
    // prompt — non-interactive execution
    // ============================================================

    /**
     * Sends {@code prompt} non-interactively and returns the plain-text
     * assistant answer on stdout.
     *
     * @param prompt the prompt to feed to the agent.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult prompt(String prompt) {
        return cli.prompt(prompt);
    }

    /**
     * Sends {@code prompt} pinned to {@code model}.
     *
     * @param prompt the prompt to feed to the agent.
     * @param model  the model identifier to pin for this run.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult prompt(String prompt, String model) {
        return cli.prompt(prompt, model);
    }

    /**
     * Sends {@code prompt} resuming a saved session.
     *
     * @param prompt    the prompt to feed to the agent.
     * @param sessionId the session to resume.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult promptWithSession(String prompt, String sessionId) {
        return cli.promptWithSession(prompt, sessionId);
    }

    /**
     * Sends {@code prompt} continuing the most recent session of the current
     * directory.
     *
     * @param prompt the prompt to feed to the agent.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult promptContinueLast(String prompt) {
        return cli.promptContinueLast(prompt);
    }

    /**
     * Convenience wrapper that runs {@code promptJson(prompt)} and decodes the
     * stream-json standard output into {@link KimiEvent}s.
     *
     * <p>Blank lines and lines that fail to parse are silently skipped; the
     * returned list contains only successfully-decoded events in order.</p>
     *
     * @param prompt the prompt to feed to the agent.
     * @return the parsed events; never {@code null}.
     */
    public List<KimiEvent> promptAndParse(String prompt) {
        KimiCliResult result = cli.promptJson(prompt);
        return parseJsonlOutput(result.getStdout());
    }

    // ============================================================
    // interactive sessions
    // ============================================================

    /**
     * Launches an interactive {@code kimi} session with the configured
     * defaults. Note the SDK captures the child streams, so full TUI
     * rendering is not available through this route.
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult startSession() {
        return cli.startInteractive();
    }

    /**
     * Resumes a saved session interactively.
     *
     * @param sessionId the session id (or name) to resume.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult resumeSession(String sessionId) {
        return cli.resumeSession(sessionId);
    }

    /**
     * Continues the most recent session of the current directory.
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult continueLastSession() {
        return cli.continueLastSession();
    }

    // ============================================================
    // auth / diagnostics / lifecycle
    // ============================================================

    /**
     * Runs {@code kimi login} (device-code OAuth flow).
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult login() {
        return cli.login();
    }

    /**
     * Runs {@code kimi doctor}.
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult doctor() {
        return cli.doctor();
    }

    /**
     * Exports a session as a ZIP archive (confirmation skipped).
     *
     * @param sessionId the session id to export.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult exportSession(String sessionId) {
        return cli.exportSession(sessionId);
    }

    /**
     * Runs {@code kimi migrate}.
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult migrate() {
        return cli.migrate();
    }

    /**
     * Runs {@code kimi upgrade --yes} (skips the confirmation prompt).
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult upgrade() {
        return cli.upgradeYes();
    }

    /**
     * Returns the underlying {@link KimiCli} for advanced callers.
     *
     * @return the CLI facade backing this client; never {@code null}.
     */
    public KimiCli cli() {
        return cli;
    }

    /**
     * Returns the runtime configuration used by this client.
     *
     * @return the configuration; never {@code null}.
     */
    public KimiClientConfig getConfig() {
        return config;
    }

    private List<KimiEvent> parseJsonlOutput(String stdout) {
        List<KimiEvent> events = new ArrayList<KimiEvent>();
        if (stdout == null || stdout.isEmpty()) {
            return events;
        }
        for (String line : stdout.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                events.add(MAPPER.readValue(trimmed, KimiEvent.class));
            } catch (Exception e) {
                log.debug("Failed to parse JSONL line: {}", trimmed, e);
            }
        }
        return events;
    }

    /**
     * Closes this client. The default implementation is a no-op because the
     * underlying {@link KimiCliExecutor} does not hold any long-lived
     * resources.
     */
    @Override
    public void close() {
    }
}
