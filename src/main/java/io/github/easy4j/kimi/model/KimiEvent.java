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
package io.github.easy4j.kimi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * Single JSON-Lines event emitted by {@code kimi --prompt <p> --output-format
 * stream-json}.
 *
 * <p>The stream-json output prints one JSON object per line: assistant
 * messages (possibly carrying {@code tool_calls}) followed by tool messages;
 * thinking content is not part of the JSONL stream. The shape is deliberately
 * loose &mdash; {@code data} carries anything the SDK does not model
 * explicitly, and unknown fields are tolerated so the SDK stays forward
 * compatible with CLI revisions.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KimiEvent {

    /** Discriminator string identifying the event kind (e.g. {@code assistant}, {@code tool}). */
    private String type;

    /** Human-readable text of the event, may be {@code null}. */
    private String content;

    /** Tool call payload present on assistant messages that request tools. */
    private Object toolCalls;

    /** Type-specific structured payload; schema depends on {@link #type}. */
    private Object data;

    /** Error payload present when the agent reports a failure for the current step. */
    private Object error;
}
