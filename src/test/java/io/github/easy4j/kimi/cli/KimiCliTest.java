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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.easy4j.kimi.KimiClientConfig;

/**
 * Contract tests for {@link KimiCli} driven without a socket: outgoing
 * argument lists are verified through the echo fixture.
 *
 * @since 1.0.0
 */
class KimiCliTest {

    /** Absolute path of the argument-echoing fixture script (surefire runs from the module base dir). */
    private static final String ECHO =
            java.nio.file.Paths.get("src", "test", "resources", "kimi-echo.sh").toAbsolutePath().toString();

    private static KimiClientConfig echoConfig() {
        KimiClientConfig config = new KimiClientConfig();
        config.setLocalExecutable(ECHO);
        config.setLocalTimeoutSeconds(2);
        return config;
    }

    private static KimiCli echoCli() {
        return new KimiCli(echoConfig(), new KimiCliExecutor(echoConfig()));
    }

    private static KimiCli cliWith(KimiClientConfig config) {
        return new KimiCli(config, new KimiCliExecutor(config));
    }

    @Test
    void shouldExposeExecutor() {
        assertNotNull(echoCli().executor());
    }

    @Test
    void shouldDelegateVersion() {
        KimiCliResult result = echoCli().version();
        assertEquals(0, result.getExitCode());
        assertTrue(result.getStdout().contains("--version"));
    }

    @Test
    void shouldDelegateHelp() {
        KimiCliResult result = echoCli().help();
        assertEquals(0, result.getExitCode());
        assertTrue(result.getStdout().contains("--help"));
    }

    // ----------------------------------------------------------------
    // prompt
    // ----------------------------------------------------------------

    @Test
    void shouldDelegatePrompt() {
        KimiCliResult result = echoCli().prompt("Fix the failing test");
        String out = result.getStdout();
        assertTrue(out.contains("--prompt"));
        assertTrue(out.contains("Fix the failing test"));
        assertFalse(out.contains("--output-format"));
    }

    @Test
    void shouldDelegatePromptWithModel() {
        KimiCliResult result = echoCli().prompt("hi", "kimi-k2-turbo-preview");
        String out = result.getStdout();
        assertTrue(out.contains("--model kimi-k2-turbo-preview"));
        assertTrue(out.contains("--prompt hi"));
    }

    @Test
    void shouldDelegatePromptJson() {
        KimiCliResult result = echoCli().promptJson("hi");
        assertTrue(result.getStdout().contains("--output-format stream-json"));
    }

    @Test
    void shouldDelegatePromptWithSession() {
        KimiCliResult result = echoCli().promptWithSession("hi", "sess-1");
        String out = result.getStdout();
        assertTrue(out.contains("--session sess-1"));
        assertTrue(out.contains("--prompt hi"));
    }

    @Test
    void shouldDelegatePromptContinueLast() {
        KimiCliResult result = echoCli().promptContinueLast("hi");
        String out = result.getStdout();
        assertTrue(out.contains("--continue"));
        assertTrue(out.contains("--prompt hi"));
    }

    @Test
    void shouldRejectBlankPrompt() {
        assertThrows(IllegalArgumentException.class, () -> echoCli().prompt("  "));
        assertThrows(IllegalArgumentException.class,
                () -> echoCli().prompt(new KimiCli.RunOptions(null, null)));
    }

    @Test
    void shouldPropagateConfigDefaultsToPrompt() {
        KimiClientConfig config = echoConfig();
        config.setDefaultModel("kimi-k2-turbo-preview");
        config.setAddDirs(new String[] {"/extra"});
        config.setSkillsDirs(new String[] {"/skills"});
        config.setAgent("reviewer");
        KimiCliResult result = cliWith(config).prompt("hi");
        String out = result.getStdout();
        assertTrue(out.contains("--model kimi-k2-turbo-preview"));
        assertTrue(out.contains("--add-dir /extra"));
        assertTrue(out.contains("--skills-dir /skills"));
        assertTrue(out.contains("--agent reviewer"));
    }

    // ----------------------------------------------------------------
    // interactive
    // ----------------------------------------------------------------

    @Test
    void shouldDelegateInteractiveSession() {
        KimiClientConfig config = echoConfig();
        config.setDefaultModel("kimi-k2-turbo-preview");
        config.setAutoApproval("yolo");
        config.setPlanMode(true);
        KimiCliResult result = cliWith(config).startInteractive();
        String out = result.getStdout();
        assertTrue(out.contains("--model kimi-k2-turbo-preview"));
        assertTrue(out.contains("--yolo"));
        assertTrue(out.contains("--plan"));
        assertFalse(out.contains("--prompt"));
    }

    @Test
    void shouldDelegateResumeSession() {
        KimiCliResult result = echoCli().resumeSession("sess-9");
        assertTrue(result.getStdout().contains("--session sess-9"));
    }

    @Test
    void shouldDelegateContinueLast() {
        KimiCliResult result = echoCli().continueLastSession();
        assertTrue(result.getStdout().contains("--continue"));
    }

    @Test
    void shouldRejectConflictingApprovalValue() {
        KimiClientConfig config = echoConfig();
        config.setAutoApproval("bogus");
        assertThrows(IllegalStateException.class, config::validate);
    }

    // ----------------------------------------------------------------
    // auth / doctor / lifecycle
    // ----------------------------------------------------------------

    @Test
    void shouldDelegateLogin() {
        assertTrue(echoCli().login().getStdout().contains("login"));
    }

    @Test
    void shouldDelegateDoctor() {
        assertTrue(echoCli().doctor().getStdout().contains("doctor"));
        assertTrue(echoCli().doctorConfig(null).getStdout().contains("config"));
        assertTrue(echoCli().doctorConfig("/tmp/config.toml").getStdout().contains("/tmp/config.toml"));
        assertTrue(echoCli().doctorTui(null).getStdout().contains("tui"));
    }

    @Test
    void shouldDelegateExport() {
        assertTrue(echoCli().exportSession("sess-1").getStdout().contains("--yes"));
        String toFile = echoCli().exportSessionTo("sess-1", "/tmp/s.zip").getStdout();
        assertTrue(toFile.contains("--output /tmp/s.zip"));
        assertTrue(echoCli().exportSessionWithoutGlobalLog("sess-1").getStdout().contains("--no-include-global-log"));
    }

    @Test
    void shouldDelegateMigrateAndUpgrade() {
        assertTrue(echoCli().migrate().getStdout().contains("migrate"));
        assertTrue(echoCli().upgrade().getStdout().contains("upgrade"));
        assertTrue(echoCli().upgradeYes().getStdout().contains("--yes"));
    }

    @Test
    void shouldDelegateVis() {
        assertTrue(echoCli().vis(null).getStdout().contains("vis"));
        assertTrue(echoCli().vis("sess-1").getStdout().contains("sess-1"));
        assertTrue(echoCli().visHeadless(null).getStdout().contains("--no-open"));
    }

    @Test
    void shouldDelegateWeb() {
        assertTrue(echoCli().web("--port", "6000").getStdout().contains("web --port 6000"));
        assertTrue(echoCli().webRotateToken().getStdout().contains("rotate-token"));
    }

    // ----------------------------------------------------------------
    // provider
    // ----------------------------------------------------------------

    @Test
    void shouldDelegateProviderCommands() {
        assertTrue(echoCli().providerAdd("https://example").getStdout().contains("provider add https://example"));
        assertTrue(echoCli().providerAddWithApiKey("https://example", "sk")
                .getStdout().contains("--api-key sk"));
        assertTrue(echoCli().providerRemove("p1").getStdout().contains("remove p1"));
        assertTrue(echoCli().providerList().getStdout().contains("list"));
        assertTrue(echoCli().providerListJson().getStdout().contains("--json"));
        assertTrue(echoCli().providerCatalogList(null).getStdout().contains("catalog list"));
        assertTrue(echoCli().providerCatalogList("p1").getStdout().contains("catalog list p1"));
        String add = echoCli().providerCatalogAdd("p1", "sk", "m1", "https://api").getStdout();
        assertTrue(add.contains("--api-key sk"));
        assertTrue(add.contains("--default-model m1"));
        assertTrue(add.contains("--base-url https://api"));
    }

    // ----------------------------------------------------------------
    // passthrough
    // ----------------------------------------------------------------

    @Test
    void shouldDelegateAcpAndRawExecute() {
        assertTrue(echoCli().acp().getStdout().contains("acp"));
        assertTrue(echoCli().execute("--version").getStdout().contains("--version"));
    }
}
