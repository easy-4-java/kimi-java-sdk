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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.easy4j.kimi.KimiClient;
import io.github.easy4j.kimi.KimiClientConfig;

/**
 * Unit tests for {@link KimiClient} validation and delegation.
 *
 * @since 1.0.0
 */
class KimiClientTest {

    private static KimiClientConfig echoConfig() {
        KimiClientConfig config = new KimiClientConfig();
        config.setLocalExecutable(
                java.nio.file.Paths.get("src", "test", "resources", "kimi-echo.sh").toAbsolutePath().toString());
        config.setLocalTimeoutSeconds(2);
        return config;
    }

    @Test
    void shouldExposeSensibleDefaults() {
        KimiClientConfig config = new KimiClientConfig();

        assertEquals("kimi", config.getLocalExecutable());
        assertEquals(600, config.getLocalTimeoutSeconds());
        assertEquals(5, config.getLocalProbeTimeoutSeconds());
        assertTrue(config.getSession() == null);
        assertFalse(config.isContinueLast());
    }

    @Test
    void shouldRejectNullConfig() {
        assertThrows(NullPointerException.class, () -> new KimiClient(null));
    }

    @Test
    void shouldRejectInvalidConfig() {
        KimiClientConfig config = echoConfig();
        config.setAutoApproval("bogus");
        assertThrows(IllegalStateException.class, () -> new KimiClient(config));

        KimiClientConfig conflicting = echoConfig();
        conflicting.setSession("sess-1");
        conflicting.setContinueLast(true);
        assertThrows(IllegalStateException.class, () -> new KimiClient(conflicting));
    }

    @Test
    void shouldDelegatePromptAndParse() {
        try (KimiClient client = new KimiClient(echoConfig())) {
            assertTrue(client.prompt("hi").getStdout().contains("--prompt hi"));
            assertTrue(client.prompt("hi", "m1").getStdout().contains("--model m1"));
            assertTrue(client.promptWithSession("hi", "s1").getStdout().contains("--session s1"));
            assertTrue(client.promptContinueLast("hi").getStdout().contains("--continue"));
            assertTrue(client.isAvailable());
        }
    }

    @Test
    void shouldDelegateLifecycle() {
        try (KimiClient client = new KimiClient(echoConfig())) {
            assertTrue(client.version().getStdout().contains("--version"));
            assertTrue(client.login().getStdout().contains("login"));
            assertTrue(client.doctor().getStdout().contains("doctor"));
            assertTrue(client.exportSession("s1").getStdout().contains("export"));
            assertTrue(client.migrate().getStdout().contains("migrate"));
            assertTrue(client.upgrade().getStdout().contains("--yes"));
        }
    }

    @Test
    void shouldCloseWithoutError() {
        KimiClient client = new KimiClient(echoConfig());
        client.close();
    }
}
