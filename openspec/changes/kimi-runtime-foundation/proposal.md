# Kimi Runtime Foundation 提案

## Why

当前 SDK 分别暴露 `KimiClient`、`KimiAcpClient`、`KimiServerClient`。调用者需要理解 CLI、ACP、HTTP 三套实现细节，并自行判断某条路径是否支持 session、streaming、cancel、model switch 等能力。对于 Agent Buddy 或其他上层 Agent Platform，这会形成大量 runtime-specific 分支逻辑。

本 Change 建立最小、稳定、可扩展的 Runtime 公共契约，并提供 Capability、Health、生命周期与错误分类的基础语义。它不要求立刻替换现有客户端，而是为后续 ACP、Server、Events 与跨 SDK Runtime Adapter 提供共同边界。

## What Changes

- 新增统一 Runtime 能力契约，覆盖 capability discovery、health、prompt、cancel、close 的共同语义。
- 新增 Capability 模型，调用者通过能力查询而不是 `instanceof` 判断功能。
- 新增 Runtime lifecycle 与 health 的可观察状态。
- 建立 Runtime 级异常分类基线，区分配置、进程、超时、协议、RPC、认证、服务端、序列化和已关闭状态。
- 明确保留现有 CLI/ACP/Server 客户端作为兼容 API；迁移采用增量 adapter/facade，不一次性破坏旧调用者。
- 为未来 easy-4-java Agent Runtime 家族保留可映射的通用语义，但本 Change 不创建跨仓库公共依赖。

## Capabilities

### New Capabilities

- `runtime-foundation`：Runtime、Capability、Health、Lifecycle、Error Taxonomy 的公共可观察契约。

### Modified Capabilities

无。当前仓库尚无 OpenSpec 主规范，本 Change 使用 ADDED 建立基线。

## Scope and Non-Goals

本 Change 覆盖 Runtime Foundation，不负责 ACP 并发修复、强类型协议对象、统一事件类型、WebSocket 实现、CLI bounded buffer、Metrics 后端或三分支 CI。上述内容由独立 Change 负责。

不在本次范围：
- Spring Boot 自动配置；
- Agent Buddy 业务逻辑；
- 自动选择 Runtime；
- 跨主机 Runtime Registry；
- 跨 SDK 公共 Maven 模块。

## Dependencies

规范评审不依赖其他 Change。实现时建议在 ACP/Process 关键可靠性问题有测试基线后接入现有客户端。

## Branch Support

```text
feature/1.0.x: required
feature/2.0.x: required
feature/3.0.x: required
```

三线必须保持同一可观察契约；Java 17/21 的附加 API 只能作为 optional adapter。

## Impact

固定规划基线：`feature/3.0.x@921a8a1ad213288dd2773298fe848d054ab8e196`。
现有 public API 不删除；新增 facade、model、exception 可能扩大 API 面。异常类型细分必须保留原 `KimiException` 可捕获兼容性。

## Approval Status

DRAFT / NOT_APPROVED。完成评审和 `openspec validate kimi-runtime-foundation --strict` 前不得进入实现完成声明。
