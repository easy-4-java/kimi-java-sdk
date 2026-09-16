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

import java.util.Objects;

import lombok.Data;

/**
 * Configuration for the Kimi CLI subprocess route.
 *
 * <p>Plain POJO (Spring {@code @ConfigurationProperties}-bindable). Every
 * field maps onto one or more {@code kimi} command line flags; {@code null}
 * fields are simply omitted from the assembled invocation.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 * @see KimiClient
 */
@Data
public class KimiClientConfig {

    /**
     * Approval mode forwarded to interactive and prompt runs: {@code yolo}
     * (ask on demand) or {@code auto} (never ask). Mutually exclusive &mdash;
     * the kimi CLI rejects both flags together.
     */
    private String autoApproval;

    /** Name or absolute path of the local {@code kimi} CLI executable. */
    private String localExecutable = "kimi";

    /** Command execution timeout in seconds (passed to the OS-level watchdog). */
    private int localTimeoutSeconds = 600;

    /** Timeout in seconds used by {@link KimiCliExecutor#probe()} when verifying CLI availability. */
    private int localProbeTimeoutSeconds = 5;

    /** Default model (e.g. {@code kimi-k2-turbo-preview}); forwarded as {@code --model} when set. */
    private String defaultModel;

    /** Resume a specific session id; forwarded as {@code --session <id>}. */
    private String session;

    /** Resume the most recent session of the current directory; forwarded as {@code --continue}. */
    private boolean continueLast;

    /** Plan mode; forwarded as {@code --plan} for interactive runs. */
    private boolean planMode;

    /** Extra directories granted to the agent; each becomes a repeatable {@code --add-dir} flag. */
    private String[] addDirs;

    /** Extra skill directories; each becomes a repeatable {@code --skills-dir} flag. */
    private String[] skillsDirs;

    /** Agent name forwarded as {@code --agent} when set. */
    private String agent;

    /** Agent definition file forwarded as {@code --agent-file} when set. */
    private String agentFile;

    /**
     * Validates mutually exclusive flag combinations eagerly, mirroring the
     * CLI's own startup rejection rules.
     *
     * @throws IllegalStateException when {@code autoApproval} is neither
     *                               {@code yolo} nor {@code auto}, or when
     *                               {@code session} and {@code continueLast}
     *                               are both set.
     */
    public void validate() {
        if (autoApproval != null && !"yolo".equals(autoApproval) && !"auto".equals(autoApproval)) {
            throw new IllegalStateException("autoApproval must be 'yolo' or 'auto': " + autoApproval);
        }
        if (session != null && continueLast) {
            throw new IllegalStateException("--session and --continue are mutually exclusive");
        }
        Objects.requireNonNull(localExecutable, "localExecutable");
    }
}
