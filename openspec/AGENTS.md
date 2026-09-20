# OpenSpec 工作约定

本仓库采用 `spec-driven`：proposal → specs → design → tasks → review → implementation → verification → archive。

## 执行门禁

1. 先读取 `openspec/config.yaml`、目标 change 全部工件、`docs/architecture/runtime-sdk-optimization-plan.md` 与相关源码。
2. 可观察契约写入 `specs/<capability>/spec.md`；技术实现选择写入 `design.md`；执行步骤和验证写入 `tasks.md`。
3. 每个 Requirement 必须有稳定编号、SHALL/MUST 和至少一个 WHEN/THEN Scenario。
4. 实现前执行 `openspec validate <change-id> --strict`；保存 CLI 版本、命令、退出码和完整输出。未执行或失败都不是通过。
5. 用户明确批准对应 Change 后才能进入业务实现；批准前只允许调查、规范和文档调整。
6. 严格 TDD：先提交能稳定失败的测试证据，再最小实现，再重构；不得用“已有代码”追认任务完成。
7. `feature/3.0.x` 可作为实现主线，但不是功能特权线；功能完成后必须同步 `feature/2.0.x`、`feature/1.0.x`。
8. 三线允许 Java/Jackson/Maven/JUnit/进程 API 的兼容适配，不允许改变公开语义、能力集合、生命周期和错误分类。
9. 资源类能力必须验证线程、ScheduledExecutorService、Process、流、HTTP/WS、pending RPC、prompt stream 无泄漏。
10. 未完成真实运行或上游协议核验时，准确标记 NOT_RUN / UNVERIFIED，不得宣称 production-ready。

## Change 依赖

```text
kimi-acp-lifecycle-hardening ─┐
kimi-process-hardening ───────┼─> kimi-runtime-foundation
kimi-typed-protocol ──────────┤
                              ├─> kimi-unified-events
kimi-server-websocket ────────┤
                              └─> kimi-observability
all changes ─────────────────────> kimi-branch-parity
```

规范可以并行评审；实现应优先解决 ACP/Process 的可靠性，再完成统一抽象和事件层。
