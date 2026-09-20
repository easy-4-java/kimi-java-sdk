# Kimi Observability 提案

## Why

当前 SDK 的日志主要服务开发调试，但作为 Agent Runtime SDK，生产环境需要回答：进程是否频繁失败、活跃 session/turn 数、RPC 延迟、timeout/cancel 数、WebSocket reconnect、序列化错误以及资源是否回收。若直接绑定 Micrometer/OpenTelemetry，会把核心 SDK 与具体监控栈耦合；若完全不提供 hook，上层只能侵入内部实现。

本 Change 定义 neutral observability contract：稳定事件/metric hook、diagnostic snapshot、trace correlation 与敏感信息保护。具体 Micrometer/OpenTelemetry adapter 可后续独立提供。

## What Changes

- 定义 runtime/process/session/turn/RPC/network 的中立观测 hook。
- 定义 counter/gauge/timer 的业务语义和生命周期，避免各 Runtime 自己命名。
- 提供有界、非敏感 diagnostic snapshot，能够定位 runtime 状态、pending work、最近失败。
- 为 run/session/turn/RPC 建立可传播 correlation id/context。
- 统一 logging category 和 redaction 默认策略。
- observer/exporter 自身失败不得破坏 Runtime 正常执行。
- 核心 artifact 不强依赖 Micrometer/OpenTelemetry/Prometheus。

## Capabilities

### New Capabilities
- `observability`：Runtime 观测、诊断、trace correlation、日志脱敏和 exporter 隔离契约。

## Scope and Non-Goals

不在本 Change 实现企业级监控平台、持久化日志、分布式采样服务或具体 dashboard；外部监控 adapter 后续独立实现。

## Dependencies

可在 Runtime Foundation、ACP、Process、WS 逐步接入；Unified Events 可作为一部分数据源，但 observability hook 不等于业务事件流。

## Branch Support

三线全部 required；核心 hook API 使用 Java 8 可表达类型。OpenTelemetry SDK 等现代依赖不能成为核心 artifact 的 mandatory dependency。

## Impact

新增 observability package、diagnostic snapshot、可选 hooks，并需要调整 CLI/ACP/Server 内部关键路径打点。默认 no-op 实现确保未配置监控时开销和行为最小。

## Approval Status

DRAFT / NOT_APPROVED。
