# kimi-acp-lifecycle-hardening 实施任务

## 1. 规范与基线
- [ ] 1.1 [ACP-01..07] 评审并批准本 Change；验证：范围只覆盖现有 ACP 可靠性。
- [ ] 1.2 执行 `openspec validate kimi-acp-lifecycle-hardening --strict` 并保存证据。
- [ ] 1.3 用 CodeGraph 记录 connect/request/readLoop/promptAsync/close 的 caller-callee 基线。

## 2. RED：生命周期与失败传播
- [ ] 2.1 [ACP-01] fake agent 构造 initialize timeout、duplicate connect；验证：现有行为至少一项失败。
- [ ] 2.2 [ACP-02,ACP-05] 构造 pending RPC 时 EOF/process exit/malformed JSON/write failure；验证：不存在无限等待断言。
- [ ] 2.3 [ACP-03] 使用 latch/barrier 构造 response-timeout、response-cancel、close-response 竞态；验证：重复运行可稳定暴露风险。
- [ ] 2.4 [ACP-04] 构造同 session 双 prompt 与不同 session 并发；验证：建立不串流断言。
- [ ] 2.5 [ACP-06] callback throw 测试；验证：第二 session 仍可完成。
- [ ] 2.6 [ACP-07] repeated close + leak baseline 测试；验证：线程/进程/maps/timer 恢复。

## 3. GREEN：最小修复
- [ ] 3.1 [ACP-01] 实现显式状态 guard 和 initialize failure cleanup。
- [ ] 3.2 [ACP-02,ACP-05] 实现 fail-all、pending removal 和 reader terminal propagation。
- [ ] 3.3 [ACP-03] 实现单终态 completion guard 和 timeout task 清理。
- [ ] 3.4 [ACP-04] 实现 same-session admission policy。
- [ ] 3.5 [ACP-06] 隔离用户 callback 异常。
- [ ] 3.6 [ACP-07] 实现幂等 close 与有界 process/timer cleanup。

## 4. REFACTOR
- [ ] 4.1 集中 lifecycle/terminal cleanup，删除重复清理分支；验证：全部 race tests 仍通过。
- [ ] 4.2 审查敏感日志和超大 frame/content 行为。

## 5. 三线与生产门禁
- [ ] 5.1 feature/3.0.x 完整 ACP tests 重复运行并记录。
- [ ] 5.2 feature/2.0.x 同语义移植和验证。
- [ ] 5.3 feature/1.0.x Java 8 同语义移植和验证。
- [ ] 5.4 运行资源泄漏、进程泄漏、CodeGraph impact review；没有实际证据不得勾选。
