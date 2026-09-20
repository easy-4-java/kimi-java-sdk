# Kimi ACP 生命周期加固提案

## Why

`KimiAcpClient` 已具备长生命周期子进程、JSON-RPC 请求关联、session、streaming callback、cancel 和 timeout，是当前最接近完整 Agent Runtime 的实现。生产风险集中在异常路径：子进程意外退出、reader thread 停止、initialize 失败、timeout/result/cancel/close 竞态、pending RPC 与 prompt stream 清理、用户 callback 抛异常。

这些问题若不先固化契约，上层 Runtime SPI 和统一事件层只会把不确定性包装得更深。

## What Changes

- 建立 ACP 生命周期状态机与合法操作边界。
- 子进程或 reader 异常时，所有 pending RPC 和 active prompt 必须有限时间内结束。
- 明确 request/prompt 的 success/error/timeout/cancel/close/process-exit 清理语义。
- 对 timeout、cancel、result、close 的竞态定义单终态规则。
- 用户 streaming callback 抛异常不能杀死协议 reader。
- 加入 malformed frame、frame/content 上限与连接失效后的诊断契约。
- 加入资源泄漏与重复 connect/close 测试。

## Capabilities

### New Capabilities

- `acp-lifecycle`：ACP 连接、请求、turn、竞态和资源所有权的可靠性契约。

### Modified Capabilities

无；首次纳入 OpenSpec。

## Scope and Non-Goals

覆盖现有 ACP client 的可靠性，不新增未核验的 ACP 方法，不在本 Change 设计统一 Runtime facade、typed domain model 或 Server WebSocket。

## Dependencies

无实现前置依赖，应优先于 Runtime Foundation adapter 和 Unified Events 的实际接入。

## Branch Support

```text
feature/1.0.x: required
feature/2.0.x: required
feature/3.0.x: required
```

## Impact

主要影响 `KimiAcpClient`、`KimiAcpConfig`、ACP fake agent 与 E2E tests。可能改变异常路径的失败速度和错误分类，但不得改变成功请求的 ACP 协议语义。

## Approval Status

DRAFT / NOT_APPROVED。实现前必须用 fake ACP agent 先建立失败用例。
