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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.easy4j.kimi.KimiClientConfig;

/**
 * Thin wrapper around Apache Commons {@code exec} that launches the local
 * {@code kimi} CLI as a child process.
 *
 * <p>Every call to {@link #execute(String...)} performs the following steps:</p>
 * <ol>
 *   <li>Build a {@link CommandLine} rooted at {@link KimiClientConfig#getLocalExecutable()}.</li>
 *   <li>Append each non-{@code null} argument via
 *       {@link CommandLine#addArgument(String, boolean)} with
 *       {@code handleQuoting=false} &mdash; the child is spawned through
 *       {@code exec(argv)}, not a shell, so quoting would embed literal double
 *       quotes inside multi-word arguments (prompts, paths) and corrupt them
 *       on arrival.</li>
 *   <li>Capture stdout and stderr into in-memory buffers; the child always
 *       receives a (possibly empty) stdin pipe that closes right after the
 *       payload, so consumers reading to EOF cannot race the input pump.</li>
 *   <li>Run the process under an {@link ExecuteWatchdog} whose timeout is
 *       derived from {@link KimiClientConfig#getLocalTimeoutSeconds()}.</li>
 *   <li>Return a {@link KimiCliResult} preserving the real exit code and both
 *       captured streams.</li>
 * </ol>
 *
 * <p>The class is intentionally synchronous and stateless (apart from the
 * injected configuration) so it can be safely shared between threads and
 * pooled by higher-level components.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see KimiCliResult
 */
public class KimiCliExecutor {

    private static final Logger log = LoggerFactory.getLogger(KimiCliExecutor.class);

    private final KimiClientConfig config;

    /**
     * Creates a new executor bound to the given configuration.
     *
     * @param config the runtime configuration providing the executable path,
     *               timeout, and probe-timeout settings; must not be {@code null}.
     */
    public KimiCliExecutor(KimiClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Runs the {@code kimi} executable with the given CLI arguments.
     *
     * <p>{@code null} entries in {@code args} are skipped silently to make
     * varargs usage easier. Failure modes:</p>
     * <ul>
     *   <li>Process timeout &mdash; {@link KimiCliResult#isTimeout()} returns
     *       {@code true}; exit code is {@code -1}; stderr starts with the
     *       timeout notice.</li>
     *   <li>Non-zero process exit &mdash; the real exit code is preserved in
     *       {@link KimiCliResult#getExitCode()}, and both captured streams are
     *       returned as-is.</li>
     *   <li>IOException (missing executable, permission denied, etc.) &mdash;
     *       the {@link IOException#getMessage()} is captured in
     *       {@link KimiCliResult#getStderr()} and the exit code is {@code -1}.</li>
     * </ul>
     *
     * @param args CLI arguments to pass to the {@code kimi} binary.
     * @return a {@link KimiCliResult} describing the outcome; never {@code null}.
     */
    public KimiCliResult execute(String... args) {
        return runProcess(null, args);
    }

    /**
     * Runs the {@code kimi} executable with the given CLI arguments, feeding
     * {@code stdin} to the child process.
     *
     * <p>Used by commands that read their payload from standard input. A
     * {@code null} or empty {@code stdin} behaves exactly like
     * {@link #execute(String...)} &mdash; the child receives an immediately
     * closing pipe. Failure modes are identical to the varargs overload.</p>
     *
     * @param stdin optional text piped to the child process's standard input.
     * @param args  CLI arguments to pass to the {@code kimi} binary.
     * @return a {@link KimiCliResult} describing the outcome; never {@code null}.
     */
    public KimiCliResult executeWithStdin(String stdin, String... args) {
        return runProcess(stdin, args);
    }

    /**
     * Lightweight reachability probe used by {@code KimiClient#isAvailable()}.
     *
     * <p>Runs {@code kimi --version} with the configured timeout and returns
     * {@code true} only if the process exits with status {@code 0}. Any
     * exception (missing executable, non-zero exit, timeout) is swallowed and
     * reported as {@code false} so callers can use the result without a
     * try/catch block.</p>
     *
     * @return {@code true} if the local CLI is reachable and reports a version,
     *         {@code false} otherwise.
     */
    public boolean probe() {
        try {
            KimiCliResult result = execute("--version");
            return result.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Decodes the buffer as UTF-8. {@code ByteArrayOutputStream.toString(Charset)}
     * requires Java 10+, so this line uses the String-name overload; UTF-8 is
     * guaranteed on every JVM, making the fallback branch unreachable.
     */
    private static String decodeUtf8(ByteArrayOutputStream buffer) {
        try {
            return buffer.toString("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return buffer.toString();
        }
    }

    private KimiCliResult runProcess(String stdin, String... args) {
        CommandLine cmd = CommandLine.parse(config.getLocalExecutable());
        for (String arg : args) {
            if (arg != null) {
                // handleQuoting=false: the child is spawned via exec(argv), not
                // a shell — commons-exec's default quoting would embed literal
                // double quotes inside arguments containing spaces (prompts,
                // paths), corrupting them on arrival.
                cmd.addArgument(arg, false);
            }
        }

        DefaultExecutor executor = new DefaultExecutor();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        // Always hand the child a (possibly empty) stdin pipe that closes
        // right after the payload: consumers reading to EOF finish instantly,
        // and a closed pipe cannot race the input pump.
        byte[] stdinBytes = stdin == null ? new byte[0] : stdin.getBytes(StandardCharsets.UTF_8);
        executor.setStreamHandler(new PumpStreamHandler(stdout, stderr,
                new ByteArrayInputStream(stdinBytes)));

        long timeoutMs = config.getLocalTimeoutSeconds() * 1000L;
        ExecuteWatchdog watchdog = new ExecuteWatchdog(timeoutMs);
        executor.setWatchdog(watchdog);

        long startNanos = System.nanoTime();
        try {
            int exitCode = executor.execute(cmd);
            // The CLI emits UTF-8; decoding with the platform default charset
            // corrupts non-ASCII output on C-locale environments.
            String out = decodeUtf8(stdout).trim();
            String err = decodeUtf8(stderr).trim();
            log.debug("kimi CLI executed: exitCode={}, stdout.len={}", exitCode, out.length());
            if (watchdog.killedProcess()) {
                return new KimiCliResult(-1, out, "kimi CLI timed out after " + timeoutMs + " ms\n" + err);
            }
            return new KimiCliResult(exitCode, out, err);
        } catch (ExecuteException e) {
            // commons-exec throws ExecuteException for EVERY non-zero exit
            // (and for watchdog kills). The stream pumps are joined before it
            // is thrown, so both buffers are complete — surface them together
            // with the real exit code instead of discarding the output. The
            // deadline check makes the timeout verdict race-free even when
            // {@code watchdog.killedProcess()} has not observed the kill yet.
            String out = decodeUtf8(stdout).trim();
            String err = decodeUtf8(stderr).trim();
            boolean timedOut = watchdog.killedProcess()
                    || System.nanoTime() - startNanos >= timeoutMs * 1_000_000L;
            if (timedOut) {
                return new KimiCliResult(-1, out, "kimi CLI timed out after " + timeoutMs + " ms\n" + err);
            }
            log.debug("kimi CLI failed: exitCode={}, stdout.len={}, stderr.len={}",
                    e.getExitValue(), out.length(), err.length());
            return new KimiCliResult(e.getExitValue(), out, err);
        } catch (IOException e) {
            return new KimiCliResult(-1, "", e.getMessage());
        }
    }
}
