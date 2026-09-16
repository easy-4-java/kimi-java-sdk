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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link KimiAcpConfig}. @since 1.0.0 */
class KimiAcpConfigTest {

    @Test
    void shouldExposeSensibleDefaults() {
        KimiAcpConfig config = new KimiAcpConfig();
        assertEquals("kimi", config.getLocalExecutable());
        assertEquals(10_000, config.getConnectTimeoutMillis());
        assertEquals(600_000, config.getReadTimeoutMillis());
        assertEquals(1_048_576, config.getMaxFrameChars());
        assertEquals(1_048_576, config.getMaxContentChars());
    }

    @Test
    void shouldAcceptExtraAcpArgs() {
        KimiAcpConfig config = new KimiAcpConfig();
        config.setAcpArgs(new String[] {"--log-level", "debug"});
        config.validate();
        assertEquals(2, config.getAcpArgs().length);
    }

    @Test
    void shouldRejectNullExecutable() {
        KimiAcpConfig config = new KimiAcpConfig();
        config.setLocalExecutable(null);
        assertThrows(NullPointerException.class, config::validate);
    }
}
