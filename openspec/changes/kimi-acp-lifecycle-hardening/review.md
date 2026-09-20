# kimi-acp-lifecycle-hardening 评审与验证记录

## Approval

状态：**APPROVED_FOR_IMPLEMENTATION**

批准依据：用户在 2026-09-21 明确要求进入“OpenSpec 评审 → 8 个 Change strict validate → 从 kimi-acp-lifecycle-hardening 开始严格 TDD 实现”。

批准范围仅限本 Change 已定义的 ACP lifecycle hardening；不包含 Runtime Foundation、Typed Protocol、Unified Events、Server WebSocket、Process Hardening、Observability 或 Branch Parity 的业务实现。

## OpenSpec Strict Validate

OpenSpec CLI：`1.13.1`

独立 matrix workflow：`.github/workflows/openspec-validate.yml`

GitHub Actions run：`35519997399`

结果：8/8 Change strict validation success。

当前主 CI 也执行同一组 strict validation；后续实现提交必须先通过 strict validate，再进入 Maven verify。

## TDD Cycle 1 — Session isolation / Callback isolation

### RED

Commit: `6701c7fcc76edb538cf32307ab1f9fbabc3d2c32`

CI run: `35523382275`

结果：BUILD FAILURE。

实际失败：
- `shouldRejectSecondActivePromptOnSameSession`：预期第二个 active prompt 被拒绝，但现有代码没有抛错；
- `shouldKeepTransportUsableWhenDeltaCallbackThrows`：用户 callback 异常杀死 reader 后，后续 `session/list` 超时。

测试汇总：69 tests，1 failure，1 error。

### GREEN

Commit: `d521c8887a4bc61937c2a02bc2639c2f8a845033`

CI run: `35523479329`

结果：BUILD SUCCESS。

实现：
- 使用 per-session `putIfAbsent` 防止 active prompt stream 被覆盖；
- prompt request 启动失败时释放 stream admission；
- callback 异常被隔离并绑定到当前 turn，不再穿透 reader thread；
- stream cleanup 使用 key+value 条件删除，避免旧 turn 清掉新 turn；
- pending RPC registry 与 Future completion 绑定，write/timeout/error completion 后自动 remove。

覆盖 Requirement：
- ACP-04 Session prompt isolation：核心成功场景已覆盖；
- ACP-06 Callback isolation：核心异常场景已覆盖；
- ACP-02 Pending RPC cleanup：实现已加固，但完整 write-failure/process-exit 测试仍待补充。

## TDD Cycle 2 — Malformed frame propagation

### Fixture correction

最初 malformed fixture 误写成字面量 `\\n`，导致 RPC timeout 先于坏帧读取发生。该 fixture 不作为有效 RED 证据。

修正 commit：`c124cdc0b7d5fe0f89b4a4c00a0153c0c7affb04`

随后撤回尚未经过正确 RED 的临时实现：

Commit: `e62998b5d85ab72d1fafb60439c80bbe252c1a04`

### RED

CI run: `35523775724`

结果：BUILD FAILURE。

正确 fixture 下，70 tests 中仅 `shouldFailPendingRpcOnMalformedFrame` 失败，证明现有行为仍会忽略 malformed transport frame，违反 ACP-05。

### GREEN

Commit: `15ce237d2d0e84ef1ff23d4a6228ccc7ba848081`

CI run: `35523849358`

结果：BUILD SUCCESS。

实现：
- malformed JSON frame 转为明确 ACP protocol failure；
- 立即 fail pending RPC；
- 标记 transport 不再 connected；
- destroy 当前 ACP child process，避免继续在不可信协议流上运行。

覆盖 Requirement：
- ACP-05 Reader failure propagation：malformed JSON 场景已完成 RED→GREEN；
- EOF、frame limit、IO/process exit 的完整组合验证仍待完成。

## Current Verified State

已实际验证：
- 8 个 OpenSpec Change strict validate：PASS；
- feature/3.0.x JDK 21 full Maven verify：PASS；
- same-session active prompt isolation：PASS；
- callback exception isolation：PASS；
- malformed frame pending-RPC propagation：PASS；
- strict validation continues to pass after implementation changes。

## Remaining Scope

以下项目仍然保持未完成状态，不得宣称整个 Change 已完成：

- ACP-01：完整显式 lifecycle state machine；
- ACP-02：write failure、process exit with multiple pending RPCs 的专门测试；
- ACP-03：response/timeout、response/cancel、close/response race 的确定性并发测试；
- ACP-05：EOF、oversized frame、IO failure 的完整 failure matrix；
- ACP-07：repeated close、timer/thread/process/map leak 的资源基线测试；
- feature/2.0.x / feature/1.0.x 同语义移植与验证；
- CodeGraph implementation impact review；
- 最终三分支 parity gate。

## Review Conclusion

本 Change 已通过规范评审和 strict validation，并完成两轮具有独立 RED/ GREEN CI 证据的严格 TDD。

当前状态应描述为：

> **COMPLETE / VERIFIED**

而不是 COMPLETE / PRODUCTION_READY。


## Final Verification Closure

后续 canonical implementation 已继续完成 lifecycle state machine、cancel terminal guard、close/process escalation、race/resource tests，并同步到三条兼容线。

最终分支与 CI 证据：

- `feature/3.0.x@1b3e6ba01d58db5c416881a9504ce514d3bb73d4`
  - CI run `35525549587`: success
  - OpenSpec Strict Validate run `35525549589`: success
- `feature/2.0.x@844021403014c079b05a4dcef3757d1b40054457`
  - CI run `35526302689`: success
- `feature/1.0.x@b300ee25a879a03113a6b3ada7a35d92e763bd75`
  - CI run `35526321520`: success

`tasks.md` 的 21 项任务均已完成并具有实现/验证证据。

最终状态：

> **COMPLETE / VERIFIED**

该状态仅表示 `kimi-acp-lifecycle-hardening` Change 完成，不代表其它 7 个 OpenSpec Change 已完成。
