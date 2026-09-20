# Observability 契约

## Purpose

在不绑定具体 telemetry 厂商的前提下，为 Kimi Runtime 提供稳定、低风险、默认脱敏的生产观测能力。

## ADDED Requirements

### Requirement: OBS-01 Neutral observer API

核心 SDK SHALL 提供不依赖特定 telemetry 框架的 observer/hook，并提供 no-op 默认实现。

#### Scenario: No observer configured

- **WHEN** 调用者不配置任何 observability backend
- **THEN** Runtime 仍按原业务契约工作，不要求 Micrometer/OpenTelemetry 依赖。

### Requirement: OBS-02 Stable metric semantics

SDK MUST 对已声明的 process/session/turn/RPC/timeout/cancel/network 指标使用跨 Runtime 一致语义，并在所有终态平衡 active/pending gauge。

#### Scenario: Failed turn

- **WHEN** 一个 active turn 以失败终结
- **THEN** active turn gauge 最终回到正确值，failure counter 增加，不因异常路径永久偏移。

### Requirement: OBS-03 Bounded diagnostics

Diagnostic snapshot MUST 使用有界数据，并且默认不包含完整 prompt、response、token、API key 或完整环境变量。

#### Scenario: Many failures

- **WHEN** Runtime 连续产生大量错误
- **THEN** snapshot 只保留配置上限内的摘要，内存占用不随失败次数无限增长。

### Requirement: OBS-04 Correlation without falsification

SDK SHALL 暴露本地与上游 correlation 标识，并明确它们的来源，不得把 SDK 自生成 id 冒充上游 request/session id。

#### Scenario: Local run id

- **WHEN** 某 transport 没有上游 turn id
- **THEN** SDK 可以生成 local run id，但 diagnostics/trace 能区分其 source。

### Requirement: OBS-05 Secure logging defaults

SDK MUST 默认对 token、secret、authentication payload 和敏感环境值脱敏，并不得在 INFO 日志记录完整 prompt/response。

#### Scenario: Authentication failure

- **WHEN** server/WS 认证失败
- **THEN** 日志包含 endpoint、错误类别等必要诊断，但不包含 bearer token 原值。

### Requirement: OBS-06 Observer isolation

Observer/exporter 抛出的异常 MUST NOT 改变 Runtime 正常业务结果或终止 transport reader。

#### Scenario: Metrics backend throws

- **WHEN** observer 在 turn completion 回调中抛异常
- **THEN** turn 的业务 completion 保持原结果，observer failure 被单独诊断。

### Requirement: OBS-07 No mandatory vendor dependency

核心 SDK MUST NOT 因启用 observability 基线而强制引入 Micrometer、OpenTelemetry 或 Prometheus runtime。

#### Scenario: Minimal dependency application

- **WHEN** 应用只依赖 kimi-java-sdk 核心 artifact
- **THEN** 可以使用全部 Runtime 基础能力而无需加载第三方 telemetry SDK。
