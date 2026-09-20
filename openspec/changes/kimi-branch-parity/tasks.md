# kimi-branch-parity 实施任务

## 1. 规范与当前基线
- [ ] 1.1 [BRP-01..07] 评审 allowed differences、optional adapter 和 production gate。
- [ ] 1.2 执行 `openspec validate kimi-branch-parity --strict`。
- [ ] 1.3 记录当前 HEAD baseline、JDK/Jackson/Maven、CodeGraph nodes/edges、test inventory。

## 2. RED：Parity 检测
- [ ] 2.1 [BRP-01] 创建 capability matrix checker，人工制造缺 capability fixture 验证会失败。
- [ ] 2.2 [BRP-02] 建立 allowed-difference 清单与 semantic fixture compare。
- [ ] 2.3 [BRP-03] 抽取/同步 Runtime/Prompt/Session/Streaming/Cancel/Lifecycle/Exception Contract Tests。
- [ ] 2.4 [BRP-04] 生成 Public API snapshot/diff；未 allowlist 差异使 CI 失败。
- [ ] 2.5 [BRP-05] CI config lint 检测 job label/java-version 不一致。
- [ ] 2.6 [BRP-06] verification report 区分 PASS/FAIL/SKIPPED/NOT_RUN。
- [ ] 2.7 [BRP-07] 编写 release/rollback checklist。

## 3. GREEN：CI 与工具
- [ ] 3.1 [BRP-01,BRP-03] 为三条分支建立 capability + contract test gate。
- [ ] 3.2 [BRP-04] 接入 API parity report。
- [ ] 3.3 [BRP-05] 修复现有 CI 文案与实际 JDK 配置一致性。
- [ ] 3.4 [BRP-06] 输出 branch/commit/JDK/Maven/OS/test-count 完整证据。

## 4. 七个 Change 接入
- [ ] 4.1 runtime-foundation 完成三线 parity。
- [ ] 4.2 acp-lifecycle-hardening 完成三线 parity。
- [ ] 4.3 typed-protocol 完成三线 parity。
- [ ] 4.4 unified-events 完成三线 parity。
- [ ] 4.5 server-websocket 完成三线 parity。
- [ ] 4.6 process-hardening 完成三线 parity。
- [ ] 4.7 observability 完成三线 parity。

## 5. Production Readiness
- [ ] 5.1 三线 compile/unit/integration/E2E 记录实际证据。
- [ ] 5.2 三线 resource/thread/process leak tests 记录实际证据。
- [ ] 5.3 dependency vulnerability/static analysis 记录工具版本和输入范围。
- [ ] 5.4 CodeGraph impact review 和 workspace hygiene 通过。
- [ ] 5.5 README compatibility matrix 与真实分支状态一致。
- [ ] 5.6 所有未覆盖项为 0 或被明确设为 release blocker 后，才能宣称 production-ready。
