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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import io.github.easy4j.kimi.KimiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link KimiServerClient} against an in-process
 * {@code com.sun.net.httpserver} fake of the kimi web API envelope.
 *
 * @since 1.0.0
 */
class KimiServerClientTest {

    private static final JsonMapper MAPPER = new JsonMapper();

    private HttpServer server;
    private int port;
    private final AtomicReference<String> lastAuth = new AtomicReference<String>();
    private volatile boolean unauthorizedMode;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/healthz", exchange -> {
            byte[] body = envelope(0, "ok", map("status", "healthy")).getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/api/v1/meta", exchange -> {
            lastAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = unauthorizedMode
                    ? envelope(40101, "unauthorized", null).getBytes("UTF-8")
                    : envelope(0, "ok", map("version", "0.42", "server_id", "s1")).getBytes("UTF-8");
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/api/v1/sessions", exchange -> {
            String path = exchange.getRequestURI().getPath();
            byte[] body;
            if ("/api/v1/sessions".equals(path) && "GET".equals(exchange.getRequestMethod())) {
                body = envelope(0, "ok", map("items", new String[] {"s1", "s2"})).getBytes("UTF-8");
            } else if ("/api/v1/sessions".equals(path)) {
                body = envelope(0, "ok", map("session_id", "s-new")).getBytes("UTF-8");
            } else if (path.endsWith("/prompts")) {
                body = envelope(0, "ok", map("prompt_id", "p1")).getBytes("UTF-8");
            } else if (path.endsWith(":abort")) {
                body = envelope(0, "ok", map("aborted", true)).getBytes("UTF-8");
            } else if (path.endsWith("/messages")) {
                body = envelope(0, "ok", map("items", new String[] {})).getBytes("UTF-8");
            } else {
                body = envelope(40401, "not found", null).getBytes("UTF-8");
            }
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/api/v1/shutdown", exchange -> exchange.close());
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private KimiServerClient attached() {
        KimiServerClient client = new KimiServerClient(new KimiServerConfig());
        client.attach("http://127.0.0.1:" + port, "test-token");
        return client;
    }

    @Test
    void shouldUnwrapEnvelopeAndSendBearerToken() {
        try (KimiServerClient client = attached()) {
            JsonNode meta = client.meta();
            assertEquals("0.42", meta.path("version").asText());
            assertEquals("s1", meta.path("server_id").asText());
            assertEquals("Bearer test-token", lastAuth.get());
        }
    }

    @Test
    void shouldRaiseOnNonZeroBusinessCode() {
        unauthorizedMode = true;
        try (KimiServerClient client = attached()) {
            KimiException ex = assertThrows(KimiException.class, client::meta);
            assertTrue(ex.getMessage().contains("40101"));
        } finally {
            unauthorizedMode = false;
        }
    }

    @Test
    void shouldSupportCoreEndpoints() {
        try (KimiServerClient client = attached()) {
            assertNotNull(client.healthz());
            JsonNode created = client.createSession("/tmp");
            assertEquals("s-new", created.path("session_id").asText());
            assertNotNull(client.listSessions());
            assertEquals("p1", client.postPrompt("s-new", "hi").path("prompt_id").asText());
            assertNotNull(client.messages("s-new"));
            assertTrue(client.abort("s-new").path("aborted").asBoolean());
        }
    }

    @Test
    void shouldRejectUseBeforeStart() {
        KimiServerClient client = new KimiServerClient(new KimiServerConfig());
        assertThrows(IllegalStateException.class, client::meta);
    }

    private static String envelope(int code, String msg, Map<String, Object> data) {
        Map<String, Object> envelope = new LinkedHashMap<String, Object>();
        envelope.put("code", code);
        envelope.put("msg", msg);
        envelope.put("data", data);
        envelope.put("request_id", "req-1");
        try {
            return MAPPER.writeValueAsString(envelope);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Object> map(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }
}
