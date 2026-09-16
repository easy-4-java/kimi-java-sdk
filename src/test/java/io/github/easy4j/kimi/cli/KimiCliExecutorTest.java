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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.easy4j.kimi.KimiClientConfig;

/**
 * Unit tests for {@link KimiCliExecutor}: raw argv passing, exit-code
 * preservation, stream capture, stdin piping and the watchdog timeout.
 *
 * @since 1.0.0
 */
class KimiCliExecutorTest {

    /** Absolute path of the argument-echoing fixture script (surefire runs from the module base dir). */
    private static final String ECHO =
            java.nio.file.Paths.get("src", "test", "resources", "kimi-echo.sh").toAbsolutePath().toString();

    private KimiClientConfig configFor(String executable) {
        KimiClientConfig config = new KimiClientConfig();
        config.setLocalExecutable(executable);
        // Short timeouts so failing tests stay fast.
        config.setLocalTimeoutSeconds(2);
        config.setLocalProbeTimeoutSeconds(2);
        return config;
    }

    @Test
    void shouldExecuteSuccessfullyWithCapturedStdout() {
        KimiCliExecutor executor = new KimiCliExecutor(configFor(ECHO));

        KimiCliResult result = executor.execute("hello", "world");

        assertEquals(0, result.getExitCode());
        assertTrue(result.isSuccess());
        assertEquals("hello world", result.getStdout());
    }

    @Test
    void shouldPassArgumentsRawWithoutEmbeddedQuotes() {
        KimiCliExecutor executor = new KimiCliExecutor(configFor(ECHO));

        KimiCliResult result = executor.execute("Write a failing test", "--model", "kimi k2");

        assertEquals("Write a failing test --model kimi k2", result.getStdout(),
                "multi-word arguments must arrive without embedded literal quotes");
    }

    @Test
    void shouldPreserveRealExitCodeAndStreamsOnNonZeroExit() {
        KimiCliExecutor executor = new KimiCliExecutor(configFor("/bin/sh"));

        KimiCliResult result = executor.execute("-c", "echo out-marker; echo err-marker 1>&2; exit 7");

        assertEquals(7, result.getExitCode());
        assertFalse(result.isSuccess());
        assertTrue(result.getStdout().contains("out-marker"), "stdout must survive a non-zero exit");
        assertTrue(result.getStderr().contains("err-marker"), "stderr must survive a non-zero exit");
    }

    @Test
    void shouldReturnIoExceptionMessageWhenExecutableMissing() {
        KimiCliExecutor executor = new KimiCliExecutor(configFor("/nonexistent/path/to/kimi"));

        KimiCliResult result = executor.execute("--version");

        assertEquals(-1, result.getExitCode());
        assertFalse(result.isSuccess());
        assertTrue(result.getStderr() != null && !result.getStderr().isEmpty());
    }

    @Test
    void shouldIgnoreNullArguments() {
        KimiCliExecutor executor = new KimiCliExecutor(configFor(ECHO));

        KimiCliResult result = executor.execute("hello", null, "world");

        assertEquals(0, result.getExitCode());
        assertEquals("hello world", result.getStdout());
    }

    @Test
    void shouldFeedStdinToChildProcess() {
        // `cat` with no file arguments echoes its standard input verbatim,
        // which is how stdin-consuming CLI forms receive their payload.
        KimiCliExecutor executor = new KimiCliExecutor(configFor("/bin/cat"));

        KimiCliResult result = executor.executeWithStdin("secret-api-key");

        assertEquals(0, result.getExitCode());
        assertEquals("secret-api-key", result.getStdout());
    }

    @Test
    void shouldExecuteWithoutStdinAsBefore() {
        KimiCliExecutor executor = new KimiCliExecutor(configFor(ECHO));

        assertEquals("plain", executor.executeWithStdin(null, "plain").getStdout());
        assertEquals("plain", executor.executeWithStdin("", "plain").getStdout());
    }

    @Test
    void shouldReportSuccessFromProbeWhenExecutableWorks() {
        KimiCliExecutor executor = new KimiCliExecutor(configFor(ECHO));

        assertTrue(executor.probe());
    }

    @Test
    void shouldReportFailureFromProbeWhenExecutableMissing() {
        KimiCliExecutor executor = new KimiCliExecutor(configFor("/nonexistent/path/to/kimi"));

        assertFalse(executor.probe());
    }

    @Test
    void shouldTimeoutOnHangingProcess() {
        // Use a short timeout and a command that sleeps for a long time.
        KimiClientConfig config = configFor("/bin/sh");
        config.setLocalTimeoutSeconds(1);
        KimiCliExecutor executor = new KimiCliExecutor(config);

        KimiCliResult result = executor.execute("-c", "sleep 30");

        assertEquals(-1, result.getExitCode());
        assertFalse(result.isSuccess());
        assertTrue(result.isTimeout(), "stderr must carry the timeout notice");
    }
}
