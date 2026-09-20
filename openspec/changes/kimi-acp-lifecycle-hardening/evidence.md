# kimi-acp-lifecycle-hardening 验证证据

## 1. OpenSpec Strict Validation

Workflow commit: `1a810365662c9c445ea1ff554ee211c396c82bbb`

Workflow run: `35519997399`

Pinned CLI: `@fission-ai/openspec@1.13.1`

全部 8 个 Change 均执行：

```text
openspec validate <change-id> --type change --strict --no-interactive
```

结果：

| Change | Job | Result |
|---|---:|---|
| kimi-runtime-foundation | 106102324518 | PASS |
| kimi-acp-lifecycle-hardening | 106102324513 | PASS |
| kimi-typed-protocol | 106102324502 | PASS |
| kimi-unified-events | 106102324343 | PASS |
| kimi-server-websocket | 106102324517 | PASS |
| kimi-process-hardening | 106102324484 | PASS |
| kimi-observability | 106102324551 | PASS |
| kimi-branch-parity | 106102324475 | PASS |

每个 job 日志均记录 `openspec --version = 1.13.1` 和 `Change '<id>' is valid`。

## 2. CodeGraph Baseline

实现前使用 CodeGraph v1.6.0 实际索引：

| Branch | Files | Nodes | Edges |
|---|---:|---:|---:|
| feature/1.0.x | 25 | 609 | 1,283 |
| feature/2.0.x | 25 | 608 | 1,278 |
| feature/3.0.x | 25 | 608 | 1,278 |

关键调用基线已检查：
- `KimiAcpClient.promptAsync` → request / scheduleTimeout / PromptStream / KimiAcpTurnResult
- `KimiAcpClient.request` ← initialize/session/prompt/session management callers
- ACP reader / pending RPC / promptStreams 为生命周期核心资源。

**Post-hardening CodeGraph impact：NOT_RUN**。当前执行环境没有可用本地 Runner，因此不得把 baseline 冒充最终 impact review。

## 3. TDD Cycle 1

### RED

Commit: `e247907b37c727b644941e81f517e4fed7e83e35`

CI run/job: `35520185628 / 106102809432`

Result: **BUILD FAILURE**

```text
Tests run: 72, Failures: 4, Errors: 0
```

失败事实：
- callback exception 杀死 reader，future 变为 TimeoutException；
- malformed JSON 被忽略，future 变为 TimeoutException；
- write-before-connect 后 pendingRpcs expected 0 but was 1；
- same-session 第二个 prompt 未被拒绝。

### GREEN

Commit: `24a51acd038f554438f115ce2d124149f432bd2f`

CI run/job: `35520278533 / 106103048600`

```text
KimiAcpLifecycleHardeningTest: 5/5 PASS
Tests run: 72, Failures: 0, Errors: 0
BUILD SUCCESS
```

## 4. TDD Cycle 2

### RED

Commit: `8d1e8685962e62a4fd86e9d3bb2b627ac840170a`

CI run/job: `35520389264 / 106103337398`

```text
Tests run: 75, Failures: 1
```

唯一失败：`cancel` 只发送 notification，没有终结 active prompt；测试等待后得到 TimeoutException。

### GREEN

Commit: `173d3f70be51822472d83c4c230b0c9ec826bb8f`

CI run/job: `35520462943 / 106103531738`

Result: **PASS**

实现 cancel → active turn exceptional completion，并由既有 completion cleanup 移除 pending RPC / prompt stream。

## 5. TDD Cycle 3

### RED

Commit: `4ceb2460f522a056edae62135a5b237dfadb48df`

CI run/job: `35520544960 / 106103751629`

```text
Tests run: 78, Failures: 0, Errors: 3
```

三个 error 全部为：
`NoSuchMethodException: KimiAcpClient.getState()`

### GREEN

Commit: `409b7cc314237bfe1df4955a57ff90388e83197b`

CI run/job: `35520636076 / 106103986308`

```text
KimiAcpLifecycleHardeningTest: 11/11 PASS
Tests run: 78, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

新增 `KimiAcpState`：
NEW → CONNECTING → INITIALIZING → READY → CLOSING → CLOSED，transport fatal failure → FAILED。

FAILED transport 会在注册新 RPC 前拒绝调用。

## 6. Compatibility Line Evidence

### feature/3.0.x implementation line

JDK: 21  
Branch: `hardening/3.0.x-acp-lifecycle`  
Head validated: `409b7cc314237bfe1df4955a57ff90388e83197b`

```text
KimiAcpLifecycleHardeningTest: 11/11 PASS
Full suite: 78/78 PASS
BUILD SUCCESS
```

### feature/2.0.x compatibility line

PR: #2  
Head: `9361053e3a65fae325182a9e51247ba27e1e5640`  
CI run/job: `35520872348 / 106104609921`  
JDK: Temurin 17.0.20

```text
KimiAcpLifecycleHardeningTest: 11/11 PASS
Full suite: 78/78 PASS
BUILD SUCCESS
```

### feature/1.0.x compatibility line

PR: #3  
Head: `2606afe39e882aaed2f6f2e1f22bc52c35c4def5`  
CI run/job: `35520900971 / 106104684383`  
JDK: Temurin 8u504

```text
KimiAcpLifecycleHardeningTest: 11/11 PASS
Full suite: 78/78 PASS
BUILD SUCCESS
```

## 7. Current Semantic Changes

已验证：
- failed write 不残留 pending RPC；
- same-session active prompt 不被覆盖；
- callback exception 不再杀死 ACP reader；
- malformed JSON 变为明确 protocol failure；
- process exit 使 pending prompt 失败；
- timeout/close 清理 pending registries；
- cancel 立即给 active prompt 单一异常终态；
- lifecycle state 可观察；
- transport fatal failure 进入 FAILED，并拒绝新 RPC；
- recoverable connect/spawn failure 回到 NEW；
- close 幂等。

## 8. Evidence Gaps

仍为 NOT_RUN / NOT_PROVEN：
- 高并发 barrier race；
- stubborn process force-kill；
- 多轮 thread/timer/process leak baseline；
- post-change CodeGraph impact；
- dependency vulnerability/static analysis（本 Change 最终 production gate）。
