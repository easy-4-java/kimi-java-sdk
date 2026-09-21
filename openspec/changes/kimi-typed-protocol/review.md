# kimi-typed-protocol 评审与验证记录

## Approval

状态：**APPROVED_FOR_IMPLEMENTATION**

用户已明确要求按 OpenSpec 继续 3.0.x canonical implementation。当前仅对 `kimi-typed-protocol` 已实现范围记录证据；跨 Jackson / 三分支同步尚未完成。

## Strict Validation

OpenSpec CLI：`1.13.1`

所有本轮提交在主 CI 中先执行 8 个 Change 的 strict validate，再进入 Maven verify；strict validate 持续通过。

## TDD Cycle 1 — Message / ContentBlock / Unknown preservation

### RED

Commit: `26f886b54d0a2821ee2f2275d408463fbab86428`

CI run: `35546109040`

结果：BUILD FAILURE。testCompile 因 `KimiProtocolMapper` 等 typed model 尚不存在而失败。

### GREEN

Commit: `31a9f4748f0c6f9869525fcb6680b84dbebda54a`

CI run: `35546177832`

结果：BUILD SUCCESS。

实现：
- `KimiMessage`
- `KimiContentBlock`
- `KimiTextContentBlock`
- `KimiUnknownContentBlock`
- `KimiProtocolMapper.readMessage`
- unknown content block 保留 raw payload
- unknown message field 保留 extensions

## TDD Cycle 2 — Session / PromptResult / Usage / StopReason

### RED

Commit: `a4e7e00d69e18facfabae7572daa955ddf63ebc0`

CI run: `35546235871`

结果：BUILD FAILURE。6 个编译错误全部来自 `KimiSession`、`KimiPromptResult` 和 mapper 方法缺失。

### GREEN

Commit: `fffd19a7cde39bec5ac62182ea3086de8ac9440b`

CI run: `35546316085`

结果：BUILD SUCCESS。

实现：
- `KimiSession`
- `KimiPromptResult`
- `KimiUsage`
- `KimiStopReason`
- session/result future-field preservation
- immutable extension map / defensive copy

## TDD Cycle 3 — ToolCall / Error / Model / Mode / Provider

### RED

Commit: `d44c84b0790575f5ff8758804d5509161318d3c4`

CI run: `35546382766`

结果：BUILD FAILURE，typed descriptor model 与 mapper 方法尚不存在。

### GREEN

Commit: `07a9a25f3afec1f2fd4d474166aab1ecfb508ca0`

CI run: `35546441152`

结果：BUILD SUCCESS。

实现：
- `KimiToolCall`
- `KimiError`
- `KimiModel`
- `KimiMode`
- `KimiProvider`
- raw arguments / error data / unknown descriptor fields 保留

## TDD Cycle 4 — PromptRequest + ACP Typed Adapter

### RED

Commit: `ab7bfb924398adddaca9b47fcfcd6058a2e9244b`

CI run: `35546529461`

结果：BUILD FAILURE，`KimiPromptRequest` 与 ACP typed prompt overload 缺失。

### GREEN

Commit: `2b2a8a7cbb52cdfedf5b5c0c13352c584bd3cf1a`

CI run: `35546611696`

结果：BUILD SUCCESS。

实现：
- immutable `KimiPromptRequest.builder()`
- required sessionId/text validation
- `KimiAcpClient.prompt(KimiPromptRequest, Consumer<String>)`
- `KimiAcpClient.promptAsync(KimiPromptRequest, Consumer<String>)`
- typed adapter 返回 `KimiPromptResult`
- legacy string-based ACP API 与 raw JsonNode API 保持可用

## Current Coverage

当前 3.0.x 已实际覆盖：
- TYP-01 typed core model 的主要模型集合
- TYP-02 unknown field preservation
- TYP-03 unknown content block
- TYP-04 immutable builder / defensive copy 的主要路径
- TYP-05 additive typed ACP adapter，raw API 未删除
- feature/3.0.x fixtures + unit/E2E tests：PASS

## Remaining Scope

以下仍未完成，不得将 Change 标记 COMPLETE：

- 固定并记录 CLI / ACP / Server 的上游真实 fixture 清单；
- TYP-05：同一响应的 typed/raw 底层事实一致性专项验证；
- TYP-06：Jackson 3 → Jackson 2.22 / 2.18 adapter 与 semantic parity；
- transport/domain 重复解析重构；
- equals/hashCode/toString 与敏感数据审查；
- feature/2.0.x 移植与测试；
- feature/1.0.x 移植与测试；
- 三线 serialized/parsed semantic snapshot 比较。

当前状态：

> **IMPLEMENTATION_IN_PROGRESS**
