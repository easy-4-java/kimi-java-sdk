# Kimi Process Runtime Hardening 提案

## Why

当前 CLI 路径和部分 ACP/Server 启动路径都依赖本地子进程。CLI executor 已处理 timeout、stdout/stderr 捕获和 exit code，但输出使用无界 `ByteArrayOutputStream`，working directory/environment 还不是统一的一等配置，timeout 后的 process tree、流清理、诊断元数据与资源边界也需要更严格定义。

长 prompt、大量日志、异常 CLI 输出或子进程再拉起孙进程时，当前实现存在 Heap 增长、残留进程、诊断不足和不同 JDK 行为不一致的风险。

## What Changes

- stdout/stderr 改为可配置有界捕获，超限继续排空并标记截断。
- 每次 execution 支持不可变 working directory / environment snapshot。
- 明确 normal exit、non-zero、spawn failure、timeout、cancel、output failure 的结果分类。
- 增加 duration、pid（可用时）、truncation、termination diagnostics。
- 定义 graceful → force 的两阶段终止与 process tree 清理策略。
- 保持 argv 直传、UTF-8 和 stdin 行为，不引入 shell expansion。
- 将资源所有权和 close/timeout/cancel 的释放顺序纳入测试。

## Capabilities

### New Capabilities
- `process-hardening`：子进程执行、输出边界、上下文、终止和诊断契约。

## Scope and Non-Goals

不在本 Change 设计统一 Runtime facade、不实现 PTY、不实现 shell command parser、不负责 ACP JSON-RPC 协议和 Server WebSocket。

## Dependencies

无规范前置依赖；Runtime Foundation 可消费本 Change 的稳定执行结果。

## Branch Support

三线全部 required。Java 8 缺少 `ProcessHandle`，必须通过兼容实现达到同一“可观察终止结果”，而不是删除 process-tree 清理语义。

## Impact

主要影响 `KimiCliExecutor`、`KimiCliResult`、CLI config，以及 ACP/Server 共享的 process helper（若后续抽取）。默认输出上限可能改变极大输出的完整捕获行为，因此必须在迁移文档中说明 truncation metadata。

## Approval Status

DRAFT / NOT_APPROVED。
