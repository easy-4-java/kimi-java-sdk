# Kimi Process Runtime Hardening 技术设计

## Context

进程执行不是简单 `exec + wait`：要同时保证 argv 正确、stdout/stderr 持续排空、内存有界、timeout 可靠、退出码保真、stdin 生命周期明确、取消可诊断，以及关闭时不遗留子孙进程。

## Decisions

### D1. Bounded Capture

stdout 和 stderr 分别配置最大保留量。无论达到上限与否，读取线程仍持续排空 pipe 直到进程终结，避免子进程因 pipe 满阻塞。短命令默认保留前缀还是尾部需固定；建议普通 CLI 捕获前缀 + truncation marker，managed/server 日志使用 tail buffer。策略必须可测试并写入 result metadata。

### D2. Per-execution Context

新增 execution context：workingDirectory、inheritEnvironment、environment overrides/removals、stdin payload。提交执行时复制 snapshot，不允许调用后 mutable config 改变已运行进程。

### D3. Explicit Outcome

结果不再只依赖 exitCode 特殊值。建议稳定 outcome/category：
SUCCESS、NON_ZERO_EXIT、SPAWN_FAILURE、TIMEOUT、CANCELLED、IO_FAILURE、OUTPUT_LIMIT_FAILURE（仅协议 frame 等必须失败时）。

真实 exit code 存在时保留。timeout/cancel 时若 OS 无真实 exit code，不伪造普通 CLI exit code。

### D4. Timeout and Cancellation

timeout 是 deadline-driven；取消是 caller-driven，必须可区分。二者进入终止流程后等待 stdout/stderr 有界排空和资源 join，再完成 result/future，避免“future 已完成但后台线程仍泄漏”。

### D5. Process Tree Termination

优先 graceful signal/destroy，等待可配置 grace；未退出则 force kill。Java 17/21 可使用 ProcessHandle descendants；Java 8 需要 OS-aware 兼容策略或可靠的 launcher/group 方案。无法证明清理完整的平台必须在验证报告标记限制，不能假装已支持。

### D6. Argv and Shell Safety

可执行路径是一个 argv 元素，不解析 shell 语法；参数通过 Commons Exec/ProcessBuilder 的 argv contract 传递，路径和 prompt 中的空格、Unicode、引号字符不得产生 shell expansion。现有 `addArgument(arg,false)` 的行为通过 fixture 固化。

### D7. UTF-8 and Streaming Decode

stdout/stderr 默认按 UTF-8 解释；流式读取必须使用跨 chunk decoder，不能在多字节字符边界产生乱码。Java 8 与新 JDK 仅实现 API 不同，输出语义一致。

### D8. Resource Ownership

executor 只终止自己启动的 process。InputStream/OutputStream、reader task、watchdog/timer、permit（若未来有并发控制）都绑定 execution terminal cleanup，释放最多一次。

## Defaults

具体默认上限在实现评审确定；建议 stdout/stderr 各 1 MiB 作为初始保留量，必须可配置。普通 CLI timeout 保持现有 600 秒兼容，除非用户批准变更。

## Validation

- 100MB 级 stdout/stderr producer，验证 Heap 保留有界且 pipe 不死锁；
- 无换行连续输出；
- UTF-8 拆块；
- spawn failure/non-zero/timeout/cancel；
- child + grandchild process；
- stdin EOF；
- concurrent calls with different env/cwd；
- repeated execution leak baseline。

## Migration / Rollback

旧 `KimiCliResult` getter 保留，新 metadata 增量增加。若无法一次共享 ACP/Server process helper，先加固 CLI，再以 Contract Test 驱动抽取；不得为了复用提前大重构。
