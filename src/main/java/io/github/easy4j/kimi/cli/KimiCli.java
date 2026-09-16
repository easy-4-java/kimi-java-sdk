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
package io.github.easy4j.kimi.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.github.easy4j.kimi.KimiClientConfig;

/**
 * Maps every Java call onto a real {@code kimi} command line invocation.
 *
 * <p>One public method per CLI sub-command or flag combination; the assembly
 * rules mirror the kimi CLI reference (flag conflicts the CLI rejects at
 * startup are rejected here eagerly as well).</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see KimiCliExecutor
 * @see KimiCliResult
 */
public class KimiCli {

    private final KimiCliExecutor executor;
    private final KimiClientConfig config;

    /**
     * Creates a new command mapper.
     *
     * @param config  runtime configuration providing the defaults.
     * @param executor subprocess executor the invocations are forwarded to.
     */
    public KimiCli(KimiClientConfig config, KimiCliExecutor executor) {
        this.config = Objects.requireNonNull(config, "config");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /**
     * Returns the subprocess executor for advanced callers.
     *
     * @return the executor; never {@code null}.
     */
    public KimiCliExecutor executor() {
        return executor;
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
        return executor.execute("--version");
    }

    /**
     * Runs {@code kimi --help}.
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult help() {
        return executor.execute("--help");
    }

    // ============================================================
    // interactive sessions
    // ============================================================

    /**
     * Launches an interactive {@code kimi} session with the configured
     * defaults (model, add-dirs, skills-dirs, agent). The child shares this
     * JVM's terminal; the SDK captures and buffers the streams, so TUI
     * rendering is not available through this route.
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult startInteractive() {
        return executor.execute(interactiveArgs(null));
    }

    /**
     * Resumes a saved session interactively via {@code kimi --session <id>}.
     *
     * @param sessionId the session id (or name) to resume.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult resumeSession(String sessionId) {
        Objects.requireNonNull(sessionId, "sessionId");
        return executor.execute(interactiveArgs(sessionId));
    }

    /**
     * Continues the most recent session of the current directory via
     * {@code kimi --continue}.
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult continueLastSession() {
        return executor.execute(interactiveArgsContinue());
    }

    private String[] interactiveArgs(String sessionId) {
        List<String> args = new ArrayList<String>();
        if (config.getDefaultModel() != null) {
            args.add("--model");
            args.add(config.getDefaultModel());
        }
        if (sessionId != null) {
            args.add("--session");
            args.add(sessionId);
        }
        if (config.isContinueLast()) {
            args.add("--continue");
        }
        if ("yolo".equals(config.getAutoApproval())) {
            args.add("--yolo");
        } else if ("auto".equals(config.getAutoApproval())) {
            args.add("--auto");
        }
        if (config.isPlanMode()) {
            args.add("--plan");
        }
        appendDirFlags(args);
        return args.toArray(new String[0]);
    }

    private String[] interactiveArgsContinue() {
        List<String> args = new ArrayList<String>();
        if (config.getDefaultModel() != null) {
            args.add("--model");
            args.add(config.getDefaultModel());
        }
        args.add("--continue");
        if ("yolo".equals(config.getAutoApproval())) {
            args.add("--yolo");
        } else if ("auto".equals(config.getAutoApproval())) {
            args.add("--auto");
        }
        appendDirFlags(args);
        return args.toArray(new String[0]);
    }

    private void appendDirFlags(List<String> args) {
        if (config.getAddDirs() != null) {
            for (String dir : config.getAddDirs()) {
                args.add("--add-dir");
                args.add(dir);
            }
        }
        if (config.getSkillsDirs() != null) {
            for (String dir : config.getSkillsDirs()) {
                args.add("--skills-dir");
                args.add(dir);
            }
        }
        if (config.getAgent() != null) {
            args.add("--agent");
            args.add(config.getAgent());
        }
        if (config.getAgentFile() != null) {
            args.add("--agent-file");
            args.add(config.getAgentFile());
        }
    }

    // ============================================================
    // prompt — non-interactive execution
    // ============================================================

    /**
     * Sends {@code prompt} non-interactively ({@code kimi --prompt <p>}) and
     * returns the plain-text assistant answer on stdout.
     *
     * @param prompt the prompt to feed to the agent.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult prompt(String prompt) {
        return executor.execute(buildPromptArgs(new RunOptions(prompt, null)));
    }

    /**
     * Sends {@code prompt} pinned to {@code model}.
     *
     * @param prompt the prompt to feed to the agent.
     * @param model  the model identifier to pin for this run.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult prompt(String prompt, String model) {
        return executor.execute(buildPromptArgs(new RunOptions(prompt, model)));
    }

    /**
     * Sends {@code prompt} with {@code --output-format stream-json} so the
     * result can be parsed into {@link io.github.easy4j.kimi.model.KimiEvent}s.
     *
     * @param prompt the prompt to feed to the agent.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult promptJson(String prompt) {
        return executor.execute(buildPromptArgs(new RunOptions(prompt, null).outputFormat("stream-json")));
    }

    /**
     * Sends {@code prompt} while resuming a saved session
     * ({@code --prompt <p> --session <id>}).
     *
     * @param prompt    the prompt to feed to the agent.
     * @param sessionId the session to resume.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult promptWithSession(String prompt, String sessionId) {
        return executor.execute(buildPromptArgs(new RunOptions(prompt, null).session(sessionId)));
    }

    /**
     * Sends {@code prompt} continuing the most recent session of the current
     * directory ({@code --prompt <p> --continue}).
     *
     * @param prompt the prompt to feed to the agent.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult promptContinueLast(String prompt) {
        return executor.execute(buildPromptArgs(new RunOptions(prompt, null).continueLast(true)));
    }

    /**
     * Sends {@code prompt} with fully configured options.
     *
     * @param options the run options; must not be {@code null}.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult prompt(RunOptions options) {
        Objects.requireNonNull(options, "options");
        return executor.execute(buildPromptArgs(options));
    }

    private String[] buildPromptArgs(RunOptions options) {
        if (options.session != null && options.continueLast) {
            // The kimi CLI rejects this combination at startup; fail here
            // with a clear message instead.
            throw new IllegalArgumentException(
                    "RunOptions: --session and --continue are mutually exclusive");
        }
        List<String> args = new ArrayList<String>();
        if (options.model != null) {
            args.add("--model");
            args.add(options.model);
        } else if (config.getDefaultModel() != null) {
            args.add("--model");
            args.add(config.getDefaultModel());
        }
        if (options.session != null) {
            args.add("--session");
            args.add(options.session);
        }
        if (options.continueLast) {
            args.add("--continue");
        }
        if (config.getAddDirs() != null) {
            for (String dir : config.getAddDirs()) {
                args.add("--add-dir");
                args.add(dir);
            }
        }
        if (config.getSkillsDirs() != null) {
            for (String dir : config.getSkillsDirs()) {
                args.add("--skills-dir");
                args.add(dir);
            }
        }
        if (config.getAgent() != null) {
            args.add("--agent");
            args.add(config.getAgent());
        }
        if (config.getAgentFile() != null) {
            args.add("--agent-file");
            args.add(config.getAgentFile());
        }
        args.add("--prompt");
        args.add(options.prompt);
        if (options.outputFormat != null) {
            args.add("--output-format");
            args.add(options.outputFormat);
        }
        return args.toArray(new String[0]);
    }

    /**
     * Fluent options for one non-interactive run. Flag conflicts the kimi CLI
     * rejects at startup are rejected here eagerly: {@code --prompt} cannot be
     * combined with {@code --yolo}/{@code --auto}/{@code --plan} (prompt mode
     * applies automatic permissions by itself), and {@code --session} cannot be
     * combined with {@code --continue}.
     */
    public static class RunOptions {

        private final String prompt;
        private String model;
        private String outputFormat;
        private String session;
        private boolean continueLast;

        /**
         * Creates run options bound to the given prompt.
         *
         * @param prompt the prompt to feed to the agent.
         * @param model  optional model pin; may be {@code null}.
         */
        public RunOptions(String prompt, String model) {
            if (prompt == null || prompt.trim().isEmpty()) {
                throw new IllegalArgumentException("kimi prompt must not be blank");
            }
            this.prompt = prompt;
            this.model = model;
        }

        /**
         * Sets the {@code --output-format} flag.
         *
         * @param v {@code text} or {@code stream-json}.
         * @return this builder for chaining.
         */
        public RunOptions outputFormat(String v) {
            this.outputFormat = v;
            return this;
        }

        /**
         * Resumes the given session for this run.
         *
         * @param v session id (or name).
         * @return this builder for chaining.
         */
        public RunOptions session(String v) {
            this.session = v;
            return this;
        }

        /**
         * Continues the most recent session for this run.
         *
         * @param v {@code true} to add {@code --continue}.
         * @return this builder for chaining.
         */
        public RunOptions continueLast(boolean v) {
            this.continueLast = v;
            return this;
        }
    }

    // ============================================================
    // auth
    // ============================================================

    /**
     * Runs {@code kimi login} (RFC 8628 device-code OAuth flow; the
     * verification URL and code are printed on stderr).
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult login() {
        return executor.execute("login");
    }

    // ============================================================
    // doctor
    // ============================================================

    /**
     * Runs {@code kimi doctor} (validates {@code config.toml} and {@code tui.toml}).
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult doctor() {
        return executor.execute("doctor");
    }

    /**
     * Runs {@code kimi doctor config [path]}.
     *
     * @param path optional config path; may be {@code null}.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult doctorConfig(String path) {
        if (path != null) {
            return executor.execute("doctor", "config", path);
        }
        return executor.execute("doctor", "config");
    }

    /**
     * Runs {@code kimi doctor tui [path]}.
     *
     * @param path optional tui config path; may be {@code null}.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult doctorTui(String path) {
        if (path != null) {
            return executor.execute("doctor", "tui", path);
        }
        return executor.execute("doctor", "tui");
    }

    // ============================================================
    // export / migrate / upgrade / vis
    // ============================================================

    /**
     * Exports a session as a ZIP archive, skipping the interactive confirm
     * ({@code kimi export <id> --yes}).
     *
     * @param sessionId the session id to export.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult exportSession(String sessionId) {
        return executor.execute("export", sessionId, "--yes");
    }

    /**
     * Exports a session to a specific path
     * ({@code kimi export <id> --yes --output <path>}).
     *
     * @param sessionId the session id to export.
     * @param output    the target ZIP path.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult exportSessionTo(String sessionId, String output) {
        return executor.execute("export", sessionId, "--yes", "--output", output);
    }

    /**
     * Exports a session keeping the global diagnostic log out of the archive
     * ({@code kimi export <id> --yes --no-include-global-log}).
     *
     * @param sessionId the session id to export.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult exportSessionWithoutGlobalLog(String sessionId) {
        return executor.execute("export", sessionId, "--yes", "--no-include-global-log");
    }

    /**
     * Runs {@code kimi migrate} (interactive data migration from kimi-cli).
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult migrate() {
        return executor.execute("migrate");
    }

    /**
     * Runs {@code kimi upgrade} (confirmation happens on the CLI's TTY).
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult upgrade() {
        return executor.execute("upgrade");
    }

    /**
     * Runs {@code kimi upgrade --yes} (skips the confirmation prompt).
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult upgradeYes() {
        return executor.execute("upgrade", "--yes");
    }

    /**
     * Runs {@code kimi vis [sessionId]} (browser session visualization).
     *
     * @param sessionId optional session id; may be {@code null}.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult vis(String sessionId) {
        if (sessionId != null) {
            return executor.execute("vis", sessionId);
        }
        return executor.execute("vis");
    }

    /**
     * Runs {@code kimi vis [sessionId] --no-open} (serves without opening a browser).
     *
     * @param sessionId optional session id; may be {@code null}.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult visHeadless(String sessionId) {
        if (sessionId != null) {
            return executor.execute("vis", sessionId, "--no-open");
        }
        return executor.execute("vis", "--no-open");
    }

    // ============================================================
    // web server
    // ============================================================

    /**
     * Runs {@code kimi web <args...>} (foreground REST + WebSocket server).
     * The call blocks until the server exits; use {@link #executeWithStdin}
     * via {@link #executor()} or background the process externally for
     * long-running deployments &mdash; or prefer
     * {@code io.github.easy4j.kimi.server.KimiServerClient}, which manages the
     * server lifecycle for you.
     *
     * @param args flags forwarded to the web subcommand.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult web(String... args) {
        String[] all = new String[args.length + 1];
        all[0] = "web";
        System.arraycopy(args, 0, all, 1, args.length);
        return executor.execute(all);
    }

    /**
     * Runs {@code kimi web rotate-token} (invalidates the previous token).
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult webRotateToken() {
        return executor.execute("web", "rotate-token");
    }

    // ============================================================
    // provider management
    // ============================================================

    /**
     * Runs {@code kimi provider add <url>}.
     *
     * @param url the provider registry URL.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult providerAdd(String url) {
        return executor.execute("provider", "add", url);
    }

    /**
     * Runs {@code kimi provider add <url> --api-key <key>}.
     *
     * @param url    the provider registry URL.
     * @param apiKey the API key for the registry.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult providerAddWithApiKey(String url, String apiKey) {
        return executor.execute("provider", "add", url, "--api-key", apiKey);
    }

    /**
     * Runs {@code kimi provider remove <providerId>}.
     *
     * @param providerId the provider id to remove.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult providerRemove(String providerId) {
        return executor.execute("provider", "remove", providerId);
    }

    /**
     * Runs {@code kimi provider list}.
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult providerList() {
        return executor.execute("provider", "list");
    }

    /**
     * Runs {@code kimi provider list --json}.
     *
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult providerListJson() {
        return executor.execute("provider", "list", "--json");
    }

    /**
     * Runs {@code kimi provider catalog list [providerId]}.
     *
     * @param providerId optional provider id filter; may be {@code null}.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult providerCatalogList(String providerId) {
        if (providerId != null) {
            return executor.execute("provider", "catalog", "list", providerId);
        }
        return executor.execute("provider", "catalog", "list");
    }

    /**
     * Runs {@code kimi provider catalog add <providerId>} with optional flags.
     *
     * @param providerId   the catalog provider id to add.
     * @param apiKey       optional {@code --api-key}; may be {@code null}.
     * @param defaultModel optional {@code --default-model}; may be {@code null}.
     * @param baseUrl      optional {@code --base-url}; may be {@code null}.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult providerCatalogAdd(String providerId, String apiKey, String defaultModel, String baseUrl) {
        List<String> args = new ArrayList<String>();
        args.add("provider");
        args.add("catalog");
        args.add("add");
        args.add(providerId);
        if (apiKey != null) {
            args.add("--api-key");
            args.add(apiKey);
        }
        if (defaultModel != null) {
            args.add("--default-model");
            args.add(defaultModel);
        }
        if (baseUrl != null) {
            args.add("--base-url");
            args.add(baseUrl);
        }
        return executor.execute(args.toArray(new String[0]));
    }

    // ============================================================
    // passthrough
    // ============================================================

    /**
     * Runs {@code kimi acp <args...>} as a plain subprocess. Prefer
     * {@code io.github.easy4j.kimi.acp.KimiAcpClient}, which drives the ACP
     * JSON-RPC session for you.
     *
     * @param args extra arguments forwarded to the ACP server.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult acp(String... args) {
        String[] all = new String[args.length + 1];
        all[0] = "acp";
        System.arraycopy(args, 0, all, 1, args.length);
        return executor.execute(all);
    }

    /**
     * Runs an arbitrary {@code kimi} invocation; escape hatch for commands the
     * SDK does not model yet.
     *
     * @param args full argument list after the executable.
     * @return the raw CLI invocation result; never {@code null}.
     */
    public KimiCliResult execute(String... args) {
        return executor.execute(args);
    }
}
