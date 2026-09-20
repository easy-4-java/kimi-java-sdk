# Kimi Unified Events 提案

## Why

当前三条 Runtime 路径的事件表达不一致：CLI 的 stream-json 解析为弱类型 `KimiEvent`，ACP 通过 `session/update` 与 onDelta callback 交付文本，Server REST 本身没有实时事件，未来 WebSocket 还会引入第三种 wire event。上层应用因此无法用一套代码处理 message delta、tool call、turn completion、usage 和 runtime failure。

本 Change 建立统一、强类型、可关联的 Runtime Event Model 与 Listener/Streaming 契约，让不同 transport 在保持原始事实的前提下映射到共同语义。

## What Changes

- 定义 Runtime、Session、Turn、Assistant Message、Tool Call、Usage、Error 等统一事件类别。
- 事件必须包含足够 correlation 信息，避免不同 session/turn/tool 的增量串流。
- 定义事件顺序、terminal event、重复终态、未知事件和 raw payload 保留规则。
- 提供 Java 8 可用的 listener/callback 核心；Java 17/21 的 `Flow.Publisher` 仅作为可选 adapter。
- 明确 listener 抛错、慢消费者和关闭后的事件处理语义。
- CLI/ACP/Server transport 分别通过 adapter 映射，不改变原 wire protocol。

## Capabilities

### New Capabilities
- `unified-events`：跨 CLI/ACP/Server 的统一 Agent Runtime 事件契约。

## Scope and Non-Goals

本 Change 不实现 Server WebSocket transport，不重新设计 ACP lifecycle，也不替代 Typed Protocol。它定义事件语义和映射边界。

## Dependencies

实现依赖 `kimi-typed-protocol` 的核心 message/content/tool 类型；与 `kimi-runtime-foundation` 对齐 lifecycle/error 语义。Server WebSocket 实现完成后接入同一事件模型。

## Branch Support

```text
feature/1.0.x: required
feature/2.0.x: required
feature/3.0.x: required
```

Java 8 核心事件 API 是兼容基线，Flow 不是能力差异。

## Impact

将新增 event model、transport event mapper 与 listener API。现有 `KimiEvent` 和 ACP onDelta 兼容入口保留，可内部映射或作为 legacy facade。

## Approval Status

DRAFT / NOT_APPROVED。
