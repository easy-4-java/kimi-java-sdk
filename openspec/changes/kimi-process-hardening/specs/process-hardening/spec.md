# Process Runtime Hardening 契约

## Purpose

保证 Kimi 本地子进程执行在大输出、超时、取消和多 JDK 环境下资源有界且结果可诊断。

## ADDED Requirements

### Requirement: PRC-01 Bounded stdout and stderr

SDK MUST 独立限制 stdout 和 stderr 的内存保留量，并在超限后继续排空子进程管道。

#### Scenario: Huge stdout

- **WHEN** 子进程输出远大于配置 capture limit
- **THEN** SDK 保留量不超过规定上限、结果标记 truncation，并且子进程不会因 stdout pipe 无人读取而死锁。

#### Scenario: Huge stderr

- **WHEN** stderr 独立超过上限
- **THEN** stdout 的限制和内容不被错误共享，stderr 单独标记截断。

### Requirement: PRC-02 Per-call execution context

SDK SHALL 允许每次调用独立设置工作目录和环境继承/覆盖/移除，并且 execution 启动后上下文不可被外部配置修改影响。

#### Scenario: Concurrent environments

- **WHEN** 两个并发 execution 对同一个环境变量设置不同值
- **THEN** 两个子进程分别看到自己的 snapshot，不相互污染。

### Requirement: PRC-03 Explicit process outcomes

SDK MUST 区分成功、非零退出、spawn failure、timeout、cancel 和 IO/transport failure，并尽可能保留真实 exit code 和已捕获输出。

#### Scenario: Non-zero with diagnostics

- **WHEN** 子进程先输出 stderr 再以非零码退出
- **THEN** 调用者取得真实非零 exit code、stderr 和 NON_ZERO_EXIT 类别。

#### Scenario: Spawn failure

- **WHEN** executable 不存在或无法启动
- **THEN** 返回 spawn failure，不把它伪装成普通 exit -1。

### Requirement: PRC-04 Timeout and cancellation are distinct

运行 timeout 与 caller cancellation MUST 是不同可观察终态，且两者都必须触发有界资源清理。

#### Scenario: Timeout

- **WHEN** execution 超过 deadline
- **THEN** 结果分类为 TIMEOUT，并启动终止流程。

#### Scenario: Caller cancel

- **WHEN** 调用者在进程运行中取消
- **THEN** 结果分类为 CANCELLED，不误报 TIMEOUT。

### Requirement: PRC-05 Process tree cleanup

SDK MUST 对自己启动的进程执行可验证的 graceful/force 终止策略，并对无法保证完整 process-tree 清理的平台明确报告限制。

#### Scenario: Child spawns descendant

- **WHEN** 被取消的 Kimi 测试进程再启动一个长生命周期子进程
- **THEN** 在已声明支持的平台上，终止完成后 owner process tree 不应残留。

### Requirement: PRC-06 Argv and UTF-8 integrity

SDK MUST 以 argv 语义而非 shell 字符串执行 Kimi，并保持空格、Unicode 和 UTF-8 输出的原始语义。

#### Scenario: Prompt contains spaces and CJK

- **WHEN** prompt 参数包含空格、中文和特殊字符
- **THEN** 子进程收到一个原始参数值，不出现额外引号或 shell expansion。

### Requirement: PRC-07 Terminal cleanup

每次 execution MUST 在任意终态后释放其拥有的流、reader task、watchdog/timer 和 process 引用，重复关闭不得二次释放导致失败。

#### Scenario: Repeated failures

- **WHEN** 连续运行大量 spawn failure、timeout 和 cancel 测试
- **THEN** 活跃线程、句柄和子进程数量回到允许的基线范围。
