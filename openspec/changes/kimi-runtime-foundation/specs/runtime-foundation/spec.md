# Runtime Foundation 契约

## Purpose

为 CLI、ACP、Server 建立共同的 Runtime 可观察语义，同时允许能力差异被显式表达。

## ADDED Requirements

### Requirement: RTF-01 Explicit capabilities

SDK MUST 为每个 Runtime 实例提供稳定的 capability 集合，并且 capability 集合必须反映该 Runtime 实际可兑现的行为。

#### Scenario: Supported capability

- **WHEN** 调用者查询某 Runtime 已实现的能力
- **THEN** capability 查询返回支持，且对应 Contract Test 可以在该分支验证该行为。

#### Scenario: Unsupported capability

- **WHEN** 调用者请求 Runtime 未声明支持的能力
- **THEN** SDK 在产生外部副作用前返回明确 unsupported-capability 错误，不静默切换到其他 Runtime。

### Requirement: RTF-02 Stable lifecycle

SDK SHALL 暴露可观察且单调终结的 Runtime 生命周期；进入 CLOSED 后不得重新报告 READY。

#### Scenario: Normal close

- **WHEN** 调用者关闭 READY Runtime
- **THEN** 生命周期最终进入 CLOSED，重复 close 幂等且不会重复释放资源。

#### Scenario: Fatal runtime failure

- **WHEN** Runtime 因不可恢复底层故障失效
- **THEN** 生命周期进入 FAILED 或等价终态，并保留可诊断原因。

### Requirement: RTF-03 Health is not lifecycle

SDK MUST 将健康状态与生命周期状态区分，并在不可用时提供非敏感诊断。

#### Scenario: Ready but unhealthy

- **WHEN** Runtime 对象仍存在但底层可用性检查失败
- **THEN** health 报告 unhealthy，不伪造 CLOSED，也不把 secret、token 或完整 prompt 写入诊断。

### Requirement: RTF-04 Error taxonomy compatibility

新增 Runtime 错误 MUST 具有稳定分类，同时继续可被现有 `KimiException` 捕获。

#### Scenario: Timeout classification

- **WHEN** Runtime 操作因期限到达失败
- **THEN** 调用者可以稳定识别 timeout 类别，并仍可通过捕获 `KimiException` 兼容处理。

#### Scenario: Closed runtime use

- **WHEN** 调用者在 CLOSED Runtime 上发起操作
- **THEN** SDK 在外部调用前返回 runtime-closed 类错误。

### Requirement: RTF-05 No implicit cross-runtime fallback

SDK MUST NOT 因某 Runtime 调用失败而自动改用 CLI、ACP 或 Server 的另一条路径。

#### Scenario: ACP failure

- **WHEN** ACP prompt 失败
- **THEN** SDK 返回 ACP 的真实失败，不在调用者未授权时改为 CLI prompt。
