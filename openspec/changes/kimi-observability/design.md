# Kimi Observability 技术设计

## Context

Agent Runtime 的观测既要足够具体，又不能泄露 prompt/token。观测层也不能成为新的故障源或形成无界内存缓存。

## Decisions

### D1. Neutral Observer SPI

核心定义轻量 `KimiRuntimeObserver`/等价接口和 no-op 默认实现。回调内容使用 typed context/event，不暴露第三方 telemetry 类型。Micrometer/OpenTelemetry adapter 放到可选 module/artifact 或示例中。

### D2. Stable Metric Semantics

建议标准语义：
- process.starts / process.failures；
- sessions.active；
- turns.active / turns.completed / turns.failed / turns.cancelled；
- rpc.pending / rpc.duration；
- prompt.duration；
- timeouts.total；
- cancels.total；
- websocket.reconnects；
- serialization.errors；
- resource.cleanup.failures。

名称最终可调整，但同一指标在 CLI/ACP/Server 的含义必须一致。Gauge 的增减与资源 ownership 绑定，不能因异常路径漏减。

### D3. Diagnostic Snapshot

Runtime 可返回当前 lifecycle/health、capabilities、active session/turn/pending RPC 数、owned process presence、WS state、最近有限数量 failure summaries。snapshot 使用有界 tail，不存完整 prompt/response/token。

### D4. Correlation Context

本地生成 runtimeId/runId 可以帮助 trace，但要明确 source=SDK-local；上游 sessionId/requestId/RPC id 保持原值 namespace。日志和 observation attributes 使用相关 id，不把整个对象序列化。

### D5. Logging Categories

建议 logger 层次：
- runtime；
- process；
- protocol；
- session；
- turn；
- tool；
- network；
- lifecycle。

INFO 只记录状态和非敏感元数据；完整 prompt/response 默认不记录。DEBUG 也必须遵守 token/secret redaction，敏感内容需要调用者显式 opt-in 且有警告。

### D6. Observer Failure Isolation

observer callback 必须被 try/catch 隔离；默认丢弃 observer failure 并通过备用 logger/diagnostic counter 记录。不能因为 metrics backend 异常导致 prompt/RPC 失败。若用户选择 strict observer policy，必须显式配置且仍不能破坏 transport reader 线程安全。

### D7. Performance

hot-path delta 不应每 token 创建昂贵 tag map。必要时聚合 turn-level metrics。高基数字段（prompt 文本、message 内容、完整 tool args）禁止作为 metric label。

## Validation

- no-op observer 与当前行为一致；
- observer throws；
- metric increment/decrement on success/failure/cancel/timeout；
- diagnostic snapshot bounded；
- redaction fixtures；
- correlation propagation；
- performance smoke/allocations 可观察但不设虚假“零开销”目标。

## Migration / Rollback

默认 observer 为 no-op，因此引入不改变业务结果。若某 Runtime 尚未接入完整指标，capability/文档必须标记 coverage，不伪造零值为“已观测”。
