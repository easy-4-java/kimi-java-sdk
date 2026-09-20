# Kimi 三分支能力一致性提案

## Why

`feature/1.0.x`、`feature/2.0.x`、`feature/3.0.x` 当前分别对应 Java 8、17、21。CodeGraph 与 tip diff 已证明三条线的业务结构高度一致，但 Git 历史已经 diverged。如果继续按“各分支各自开发”推进，很容易出现 1.x 修了 A、2.x 修了 B、3.x 才有 C 的功能漂移。

本 Change 把“三条分支是兼容线，不是功能版本”变成可执行的规范、Contract Tests、CI gate 与发布门禁。

## What Changes

- 固化三条分支的 Capability Parity 原则。
- 规定允许差异仅限 Java/Jackson/Maven/JUnit/平台 API 等兼容实现。
- 建立跨分支共享 Contract Tests 和 fixture snapshots。
- 建立 Public API、Capability、Exception、Event、Protocol semantic parity 检查。
- 建立推荐同步顺序：规范/测试 → 3.0.x 实现主线 → 2.0.x → 1.0.x。
- 建立 CI matrix 和生产就绪门禁，要求三线分别有真实测试证据。
- 修正 CI 文案/配置漂移等维护债务，避免注释和实际 JDK 不一致。

## Capabilities

### New Capabilities
- `branch-parity`：三条 Java compatibility line 的能力、API、测试与发布一致性契约。

## Scope and Non-Goals

本 Change 不要求三分支源代码逐字相同，也不要求依赖版本一致。它约束的是可观察业务语义和公共能力。不会为了减少分支数量强制合并 Java 8/17/21 线。

## Dependencies

这是最终集成门禁，覆盖其余七个 Change。任何 Change 的完成声明必须经过本 Change 定义的三线验证。

## Branch Support

本 Change 本身适用于全部三条分支，并定义它们之间的同步规则。

## Impact

影响 CI、测试组织、release checklist、README compatibility matrix、可能新增 API diff/fixture compare 脚本。不会改变 Kimi wire protocol。

## Approval Status

DRAFT / NOT_APPROVED。当前三条线只证明“现状接近”，不等于未来 parity 已自动满足。
