# kimi-typed-protocol 评审与验证记录

## Approval

状态：**APPROVED_FOR_IMPLEMENTATION**

用户已明确要求按 OpenSpec 推进 3.0.x canonical implementation，并在 canonical 通过后同步 2.0.x / 1.0.x。本文记录 typed protocol 的 TDD 与三线语义证据。

## Strict Validation

OpenSpec CLI：`1.13.1`

3.0.x 主 CI 在 Maven verify 前执行 8 个 Change 的 strict validate；`kimi-typed-protocol` 持续通过。

## Fixture Inventory

当前 typed protocol 只对“有证据的 shape”做强类型化；不确定数据继续通过 raw / extensions 保留。

| Fixture / Harness | 来源 | 用途 | 状态 |
| --- | --- | --- | --- |
| `KimiProtocolMapperTest` inline JSON | SDK contract fixture | Message/Text/UnknownBlock/unknown message fields | VERIFIED_FOR_SDK_CONTRACT |
| `KimiProtocolCoreModelTest` inline JSON | SDK contract fixture | Session/PromptResult/Usage/StopReason/immutability | VERIFIED_FOR_SDK_CONTRACT |
| `KimiProtocolDescriptorModelTest` inline JSON | SDK contract fixture | ToolCall/Error/Model/Mode/Provider | VERIFIED_FOR_SDK_CONTRACT |
| `KimiProtocolRawTypedParityTest` inline raw JsonNode | SDK cross-Jackson contract fixture | typed/raw fact parity、future fields、defensive snapshot | VERIFIED_CROSS_BRANCH |
| `fake-acp-agent.py` | controlled ACP process fixture | typed ACP prompt adapter + legacy raw API coexistence | VERIFIED_E2E |
| CLI stream-json typed mapping | 尚未纳入本 Change | CLI transport → domain | NOT_CLAIMED |
| Server REST/WS typed mapping | 尚未纳入本 Change | Server transport → domain | NOT_CLAIMED |

因此本 Change 的完成只表示核心 typed domain + ACP typed adapter + raw escape hatch 完成，不表示 CLI/Server 全部 transport 已类型化。

## TDD Cycle 1 — Message / ContentBlock / Unknown preservation

### RED

Commit: `26f886b54d0a2821ee2f2275d408463fbab86428`

CI run: `35546109040`

结果：BUILD FAILURE，typed mapper/model 尚不存在。

### GREEN

Commit: `31a9f4748f0c6f9869525fcb6680b84dbebda54a`

CI run: `35546177832`

结果：BUILD SUCCESS。

实现：
- `KimiMessage`
- `KimiContentBlock`
- `KimiTextContentBlock`
- `KimiUnknownContentBlock`
- unknown content block raw payload preservation
- unknown message field extensions

## TDD Cycle 2 — Session / PromptResult / Usage / StopReason

### RED

Commit: `a4e7e00d69e18facfabae7572daa955ddf63ebc0`

CI run: `35546235871`

结果：BUILD FAILURE。

### GREEN

Commit: `fffd19a7cde39bec5ac62182ea3086de8ac9440b`

CI run: `35546316085`

结果：BUILD SUCCESS。

实现：
- `KimiSession`
- `KimiPromptResult`
- `KimiUsage`
- `KimiStopReason`
- future-field preservation
- immutable extension map / defensive copy

## TDD Cycle 3 — ToolCall / Error / Model / Mode / Provider

### RED

Commit: `d44c84b0790575f5ff8758804d5509161318d3c4`

CI run: `35546382766`

结果：BUILD FAILURE。

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
- raw arguments / error data / unknown descriptor fields

## TDD Cycle 4 — PromptRequest + ACP Typed Adapter

### RED

Commit: `ab7bfb924398adddaca9b47fcfcd6058a2e9244b`

CI run: `35546529461`

结果：BUILD FAILURE。

### GREEN

Commit: `2b2a8a7cbb52cdfedf5b5c0c13352c584bd3cf1a`

CI run: `35546611696`

结果：BUILD SUCCESS。

实现：
- immutable `KimiPromptRequest.builder()`
- required sessionId/text validation
- ACP typed sync/async prompt overload
- typed `KimiPromptResult`
- legacy string ACP API 与 raw JsonNode API 保留

## TDD Cycle 5 — Full Raw-Node Typed Parity on 3.0.x

### Initial attempt and fixture correction

第一次 mapper refactor 暴露 raw-node overload fixture 不完整；在补齐完整 descriptor/session raw-node fixture 后，先恢复 pre-refactor mapper 形成有效 RED。

### RED

Commit: `77ff135a3c270dda0ff9d1b733b2637d34815d68`

CI run: `35558539907`

结果：BUILD FAILURE。

失败证明：Session / ToolCall / Error / Model / Mode / Provider 尚不能与 caller-owned raw JsonNode 使用同一 typed mapper 路径。

### GREEN

Commit: `4cb727ef61e79d19a8ea37c43c0bb13eb39acd75`

CI run: `35558587378`

结果：BUILD SUCCESS。

实现：
- 所有核心 typed model 同时支持 String JSON 与 caller-owned JsonNode；
- String 入口统一委托 raw-node 入口，消除重复解析；
- typed model 对 caller raw data 做 defensive snapshot；
- unknown fields 在 raw-node 和 string 两条入口中保持同一语义。

覆盖：
- TYP-05 raw/typed parity；
- 4.1 transport/domain parsing deduplication。

## TDD Cycle 6 — Jackson 2.22 / 2.18 Cross-Branch Parity

### 2.0.x RED

Commit: `e2a11a8ec405e94082eac382cdacdd5e95fe0849`

CI run: `35558668945`

结果：BUILD FAILURE。

### 2.0.x GREEN

Commit: `4fa4d1eb9ccfc821c5f24ad482b2360ceaacf6db`

CI run: `35561433011`

结果：BUILD SUCCESS。

### 1.0.x RED

Commit: `862f28995f87bb71aba4b0d65337eccfb1246293`

CI run: `35558674566`

结果：BUILD FAILURE。

### 1.0.x GREEN

Commit: `27772d3bf4897cd494047f4a38a03a777787b296`

CI run: `35561436043`

结果：BUILD SUCCESS。

实现差异：
- 3.0.x：Jackson 3 `tools.jackson.*`，object properties 使用 Jackson 3 API；
- 2.0.x / 1.0.x：Jackson 2 `com.fasterxml.jackson.*`，extension traversal 使用 `fields()` iterator；
- core typed model、raw-node overload、future-field preservation、defensive-copy 行为保持一致。

`KimiProtocolRawTypedParityTest` 三线在仅归一化 Jackson package 名后内容一致，作为 TYP-06 semantic fixture contract。

## Model Safety Review

- 新 typed domain models 未增加自动输出完整 prompt/token/secret 的自定义 `toString()`；
- raw JsonNode/extension 数据通过 getter 显式访问，不被 logger 自动序列化；
- collection/map 构造使用 defensive copy/unmodifiable view 的已测试路径；
- Error/ToolCall raw data 可包含业务内容，因此后续 observability/logging 层仍必须执行 redaction；typed protocol 本身不把这些对象自动写入日志。

## Verified Requirements

- TYP-01 Typed core models：PASS
- TYP-02 Preserve unknown data：PASS
- TYP-03 Unknown content block：PASS
- TYP-04 Immutable observable state：PASS
- TYP-05 Raw API remains available / typed-raw fact parity：PASS
- TYP-06 Cross-Jackson semantic parity：PASS

## Branch Verification

- `feature/3.0.x@4cb727ef61e79d19a8ea37c43c0bb13eb39acd75`
  - CI run `35558587378`: success
- `feature/2.0.x@4fa4d1eb9ccfc821c5f24ad482b2360ceaacf6db`
  - CI run `35561433011`: success
- `feature/1.0.x@27772d3bf4897cd494047f4a38a03a777787b296`
  - CI run `35561436043`: success

### Important compatibility caveat

当前 1.0.x / 2.0.x 的 GitHub CI workflow 仍显示使用 JDK 21 执行 Maven verify。上述证据足以支持本 Change 的 **Jackson 2/3 semantic parity**，但不作为“Java 8 / Java 17 真实运行时已验证”的证据。

真实 JDK baseline、CI label 与 java-version 一致性属于 `kimi-branch-parity` 的 BRP-05 门禁，后续必须单独纠正和验证。

## Review Conclusion

`kimi-typed-protocol` 在其批准范围内已经完成：
- 规范门禁；
- 6 轮可追溯 TDD / parity 工作；
- raw escape hatch；
- caller-owned JsonNode parity；
- Jackson 3 / 2.22 / 2.18 语义适配；
- 3.0.x / 2.0.x / 1.0.x 同 fixture 验证。

最终状态：

> **COMPLETE / VERIFIED**

该状态仅表示 `kimi-typed-protocol` Change 完成，不代表 CLI/Server typed transport、其它 6 个未完成 OpenSpec Change 或 Java 8/17 runtime gate 已完成。
