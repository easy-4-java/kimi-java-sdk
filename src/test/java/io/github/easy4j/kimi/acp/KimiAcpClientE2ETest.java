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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.github.easy4j.kimi.KimiException;

/**
 * End-to-end tests running {@link KimiAcpClient} against a fake ACP agent
 * process (python3, NDJSON JSON-RPC on stdio) — the same wire contract as
 * {@code kimi acp}.
 *
 * @since 1.0.0
 */
class KimiAcpClientE2ETest {

    private static final String FAKE_AGENT = Paths.get("src", "test", "resources", "fake-acp-agent.py")
            .toAbsolutePath().toString();

    private static KimiAcpConfig config() {
        KimiAcpConfig config = new KimiAcpConfig();
        config.setLocalExecutable("python3");
        config.setAcpSubcommand(null);
        config.setAcpArgs(new String[] {FAKE_AGENT});
        config.setConnectTimeoutMillis(10_000);
        config.setReadTimeoutMillis(10_000);
        return config;
    }

    private static KimiAcpConfig config(String... modes) {
        KimiAcpConfig config = config();
        String[] args = new String[modes.length + 1];
        args[0] = FAKE_AGENT;
        System.arraycopy(modes, 0, args, 1, modes.length);
        config.setAcpArgs(args);
        return config;
    }

    @Test
    void shouldExposeLifecycleStateTransitions() {
        KimiAcpClient client = new KimiAcpClient(config());
        assertEquals(KimiAcpState.NEW, client.getState());

        client.connect();
        assertEquals(KimiAcpState.READY, client.getState());

        client.close();
        assertEquals(KimiAcpState.CLOSED, client.getState());
    }

    @Test
    void shouldReturnToNewAfterSpawnFailure() {
        KimiAcpConfig config = config();
        config.setLocalExecutable("/nonexistent/kimi");
        KimiAcpClient client = new KimiAcpClient(config);

        assertEquals(KimiAcpState.NEW, client.getState());
        assertThrows(KimiException.class, client::connect);
        assertEquals(KimiAcpState.NEW, client.getState(),
                "a spawn failure before transport ownership must remain retryable");
        client.close();
        assertEquals(KimiAcpState.CLOSED, client.getState());
    }

    @Test
    void shouldConnectAndInitialize() {
        try (KimiAcpClient client = new KimiAcpClient(config())) {
            String version = client.connect();

            assertEquals("0.0.0-test", version);
            assertEquals("0.0.0-test", client.getAgentVersion());
            assertNotNull(client.getProtocolVersion());
        }
    }

    @Test
    void shouldRunFullSessionLifecycleWithDeltas() {
        try (KimiAcpClient client = new KimiAcpClient(config())) {
            client.connect();

            String sessionId = client.newSession("/tmp");
            assertEquals("sess_fake", sessionId);

            List<String> deltas = new ArrayList<String>();
            KimiAcpTurnResult result = client.prompt(sessionId, "打个招呼", deltas::add);

            assertEquals("end_turn", result.getStopReason());
            assertEquals("你好世界", result.getContent());
            assertEquals(2, deltas.size());
            assertEquals("你好", deltas.get(0));
            assertEquals("世界", deltas.get(1));

            assertEquals("sess_forked", client.forkSession(sessionId));
            client.closeSession(sessionId);
            client.deleteSession(sessionId);
            client.listSessions();
        }
    }

    @Test
    void shouldSupportSessionLoadResumeAndModelChange() {
        try (KimiAcpClient client = new KimiAcpClient(config())) {
            client.connect();

            assertNotNull(client.loadSession("sess_fake"));
            assertNotNull(client.resumeSession("sess_fake"));
            assertNotNull(client.setModel("sess_fake", "kimi-k2-turbo-preview"));
            assertNotNull(client.setMode("sess_fake", "code"));
            assertNotNull(client.authenticate());
            assertNotNull(client.logout());
        }
    }

    @Test
    void shouldFailConnectWhenAgentExitsPrematurely() {
        KimiAcpConfig config = config();
        // `/bin/echo` exits immediately without ever answering initialize.
        config.setLocalExecutable("/bin/echo");
        KimiAcpClient client = new KimiAcpClient(config);
        assertThrows(KimiException.class, client::connect);
        client.close();
    }

    @Test
    void shouldRejectDoubleConnect() {
        try (KimiAcpClient client = new KimiAcpClient(config())) {
            client.connect();
            assertThrows(IllegalStateException.class, client::connect);
        }
    }

    @Test
    void shouldAllowRetryAfterFailedConnectWithoutLeakingProcess() {
        KimiAcpConfig config = config();
        config.setLocalExecutable("/nonexistent/kimi");
        KimiAcpClient client = new KimiAcpClient(config);
        assertThrows(KimiException.class, client::connect);
        assertFalse(client.isClosed(), "failed connect must leave the client open for a retry");
    }

    @Test
    void shouldRejectUseAfterClose() {
        KimiAcpClient client = new KimiAcpClient(config());
        client.close();
        assertTrue(client.isClosed());
        assertThrows(IllegalStateException.class, () -> client.promptAsync("s", "hi", null));
        assertThrows(KimiException.class, () -> client.newSession("/tmp"));
        client.close();
    }

    @Test
    void shouldTimeoutPromptWhenAgentNeverAnswers() throws Exception {
        KimiAcpConfig config = new KimiAcpConfig();
        // `cat` speaks no JSON-RPC: the initialize request is never answered.
        config.setLocalExecutable("/bin/cat");
        config.setConnectTimeoutMillis(1_000);
        config.setReadTimeoutMillis(1_000);
        KimiAcpClient client = new KimiAcpClient(config);
        assertThrows(KimiException.class, client::connect);
        client.close();
    }

    @Test
    void shouldRejectSecondActivePromptOnSameSession() throws Exception {
        KimiAcpConfig config = config("--slow-prompt");
        config.setReadTimeoutMillis(5_000);
        try (KimiAcpClient client = new KimiAcpClient(config)) {
            client.connect();
            String sessionId = client.newSession("/tmp");

            CompletableFuture<KimiAcpTurnResult> first = client.promptAsync(sessionId, "first", null);

            assertThrows(KimiException.class,
                    () -> client.promptAsync(sessionId, "second", null),
                    "a second active turn must not overwrite the first session stream");

            assertEquals("end_turn", first.get(5, TimeUnit.SECONDS).getStopReason());
        }
    }

    @Test
    void shouldFailPendingRpcOnMalformedFrame() {
        KimiAcpConfig config = config("--malformed-on-list");
        config.setConnectTimeoutMillis(1_000);
        try (KimiAcpClient client = new KimiAcpClient(config)) {
            client.connect();

            KimiException error = assertThrows(KimiException.class, client::listSessions);

            assertTrue(error.getMessage().contains("protocol")
                            || error.getMessage().contains("malformed"),
                    "malformed transport data must fail as a protocol error, not a timeout");
            assertEquals(KimiAcpState.FAILED, client.getState(),
                    "fatal protocol corruption must leave the transport in FAILED");
        }
    }

    @Test
    void shouldKeepTransportUsableWhenDeltaCallbackThrows() {
        KimiAcpConfig config = config();
        config.setReadTimeoutMillis(750);
        try (KimiAcpClient client = new KimiAcpClient(config)) {
            client.connect();
            String sessionId = client.newSession("/tmp");

            assertThrows(KimiException.class,
                    () -> client.prompt(sessionId, "callback failure", delta -> {
                        throw new IllegalStateException("listener boom");
                    }));

            assertNotNull(client.listSessions(),
                    "a user callback failure must not terminate the ACP reader transport");
        }
    }
}
