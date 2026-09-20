# Kimi Typed Protocol 提案

## Why

当前 ACP/Server API 大量返回 `JsonNode`，请求使用 `Map<String,Object>`，CLI 的 `KimiEvent` 也以 `Object` 承载 toolCalls/data/error。SDK 用户需要重复理解 JSON shape、字段兼容和错误处理，削弱 Java SDK 的价值。

本 Change 建立核心强类型协议模型，同时保留 Raw JSON escape hatch，避免因上游协议演进把 SDK 锁死。

## What Changes

- 引入 Session、PromptRequest/Result、Message、ContentBlock、ToolCall/Result、Usage、Model、Mode、Provider、StopReason、Error 等核心类型。
- Typed API 默认处理已知稳定字段。
- 未知字段/事件必须可保留或通过 raw view 访问，不能因 SDK 版本较旧就无条件丢数据。
- ACP 与 Server 的 transport model 和 domain model 分层，避免 HTTP/JSON-RPC 细节污染 public domain API。
- Jackson 2/3 分支采用各自正确扩展机制，但对调用者保持相同语义。

## Capabilities

### New Capabilities
- `typed-protocol`：Kimi 核心请求、结果、消息和内容块的强类型契约。

## Scope and Non-Goals

本 Change 不定义 Runtime 生命周期、不实现 WebSocket、不定义统一事件时序、不删除 raw JsonNode API，也不承诺建模所有未核验的 Kimi 上游字段。

## Dependencies

与 Runtime Foundation 可并行设计；Unified Events 和 Server WebSocket 应依赖本 Change 的稳定核心类型。

## Branch Support

三线全部 required；2.x/1.x 使用 Jackson 2，3.x 使用 Jackson 3，JSON round-trip 语义一致。

## Impact

新增 model 包和 mapper/adapter。现有 JsonNode 方法继续保留，typed overload 作为增量 API。

## Approval Status

DRAFT / NOT_APPROVED。
