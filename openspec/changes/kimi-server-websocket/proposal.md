# Kimi Server WebSocket 提案

## Why

当前 `KimiServerClient` 已覆盖 `kimi web` 的 REST 核心能力：meta、health、session、prompt、messages、abort 以及通用 GET/POST/DELETE；但代码注释明确将 `/api/v1/ws` WebSocket 事件流留给调用者自行实现。结果是 Server Runtime 只能提交命令和查询状态，无法由 SDK 完成实时 Agent 事件消费。

本 Change 将 WebSocket 纳入正式 SDK，但不会在上游协议尚未核验的地方猜测认证方式、heartbeat 帧或事件 schema。所有 wire 细节在实现前必须固定官方/真实服务 fixture。

## What Changes

- 新增 Server Event/WebSocket client 与连接生命周期。
- 复用 Server bearer token/endpoint 配置，但认证 wire format 必须经过目标版本验证。
- 支持事件订阅、断线、可配置重连、heartbeat/keepalive（若上游协议要求）。
- WebSocket 事件映射到 `kimi-unified-events`，未知事件保留 raw payload。
- REST prompt 与 WS event correlation 必须避免跨 session/turn 串流和重复完成。
- 对队列、单帧、消息累计和重连等待使用有界资源。
- owned server 与 external server 生命周期继续分离，关闭 WS client 不误杀外部 server。

## Capabilities

### New Capabilities
- `server-websocket`：Kimi Web Server 的实时事件连接、恢复、资源管理和事件映射契约。

## Scope and Non-Goals

本 Change 不重新定义 REST API，不替代 ACP，不承诺上游未提供的 exactly-once delivery，不自动重发 prompt，不实现跨主机服务发现。

## Dependencies

实现依赖 `kimi-typed-protocol` 和 `kimi-unified-events`。Server REST lifecycle 继续沿用现有客户端或 Runtime Foundation。

## Branch Support

三线全部 required。Java 8 需要选择兼容的 WebSocket 实现；不能因 JDK 自带 HTTP Client 仅存在于新版本而删减 1.0.x 能力。

## Impact

新增 WebSocket 依赖或内部实现、连接线程/调度器、测试 server/fixtures。任何新增依赖需做体积、许可证和 CVE 审查。

## Approval Status

DRAFT / NOT_APPROVED。上游 WS handshake、认证、heartbeat、事件 schema 在固定目标版本完成实测前标记 UNVERIFIED。
