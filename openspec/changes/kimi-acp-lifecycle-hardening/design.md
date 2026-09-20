# Kimi ACP 生命周期加固技术设计

## Context

当前 ACP client 使用一个 `kimi acp` 子进程、一条 reader thread、一个 ScheduledExecutorService、`pendingRpcs` 和 `promptStreams` 两个并发 Map。正常路径已经清晰，风险来自资源终态在不同线程竞争。

## Decisions

### D1. 显式状态机

建议状态：NEW → CONNECTING → INITIALIZING → READY → CLOSING → CLOSED；任意运行阶段可因不可恢复错误进入 FAILED。重复 connect 不得孤儿化旧进程；FAILED 是否允许 reconnect 由新实例或显式 reset 决定，首版建议新实例。

### D2. Single Terminal Outcome

每个 RPC Future 和 Prompt Future 只允许一个 terminal completion。timeout、cancel、response、reader failure、close 通过 compare-and-complete/原子 guard 竞争，失败方只做必要清理，不覆盖首个终态。

### D3. Pending RPC Ownership

request 创建 id 后，Map entry 生命周期必须绑定 Future completion。任何终态都删除 entry。writeJson 失败时不得留下 pending entry。process/reader 失败时 snapshot 所有 pending，并以同一 runtime-failure 根因结束。

### D4. Prompt Stream Ownership

同一 session 是否允许并发 prompt 必须显式定义。当前 `promptStreams` 以 sessionId 为 key，天然无法安全表示同 session 多 turn；首版建议同 session 单 active turn，第二个 prompt 在发送前返回 busy。不同 session 可并发。

### D5. Reader Failure

EOF、malformed JSON、超大 frame、IOException、子进程退出都应转换为 transport/protocol failure。不可恢复失败触发一次全局 fail-all；不允许 reader 静默退出而 future 永久挂起。

### D6. Callback Isolation

协议 reader 只负责解析和状态推进。用户 callback 抛异常必须捕获并记录；默认不中断 reader。该 turn 的 callback error 是否终止 turn由 API 决策，首版建议标记 listener failure 并结束该 turn，但连接继续可用。

### D7. Timers

每个 timeout task 在 Future 终结后取消引用；client close 时 shutdown timer 并等待有界期限。ScheduledExecutorService 不得在 close 后保留非 daemon 活跃任务。

### D8. Process Close

close 顺序：拒绝新请求 → 标记 CLOSING → fail/cancel active work → 关闭 stdin → 请求/等待子进程退出 → 必要时 force destroy → 停止 reader/timer → 清空 maps → CLOSED。所有步骤幂等。

## Security / Limits

maxFrameChars/maxContentChars 必须实际执行。异常日志不输出完整认证 payload、token 或超长用户内容。

## Validation

使用可编程 fake ACP agent 精确制造：
- initialize timeout；
- EOF；
- malformed JSON；
- delayed response；
- response/cancel race；
- response/timeout race；
- process exit；
- callback throw；
- multi-session concurrency；
- same-session concurrent prompt；
- close while pending。

重复运行后检查线程数、子进程、pending maps、timer queue 恢复基线。

## Migration / Rollback

保留现有 public 方法签名；可靠性改动内部完成。若新增 busy/runtime-state 异常，仍继承 KimiException/既有运行时异常兼容层。可按独立提交 revert，不改变 ACP wire method 名称。
