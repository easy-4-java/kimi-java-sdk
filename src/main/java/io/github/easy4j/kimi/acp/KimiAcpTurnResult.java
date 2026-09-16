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

import lombok.Data;

/**
 * Outcome of one ACP prompt turn.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see KimiAcpClient#prompt(String, String, java.util.function.Consumer)
 */
@Data
public class KimiAcpTurnResult {

    /** The session the turn ran on. */
    private final String sessionId;

    /** Agent-reported stop reason of the turn (e.g. {@code end_turn}); may be {@code null}. */
    private final String stopReason;

    /**
     * Accumulated assistant text of the turn (all {@code agent_message_chunk}
     * updates concatenated), subject to {@code maxContentChars} truncation.
     */
    private final String content;
}
