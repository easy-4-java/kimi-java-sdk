# kimi-process-hardening 实施任务

## 1. 门禁
- [ ] 1.1 [PRC-01..07] 评审 bounded buffer、outcome、process-tree 和 Java 8 策略。
- [ ] 1.2 执行 `openspec validate kimi-process-hardening --strict`。
- [ ] 1.3 记录当前 KimiCliExecutor 的 CodeGraph impact 和现有 timeout/UTF-8 tests。

## 2. RED
- [ ] 2.1 [PRC-01] 构造超大 stdout/stderr、无换行输出，增加内存上限和不死锁断言。
- [ ] 2.2 [PRC-02] 并发 env/cwd snapshot 测试。
- [ ] 2.3 [PRC-03,PRC-04] success/non-zero/spawn/timeout/cancel 分类测试。
- [ ] 2.4 [PRC-05] child+grandchild termination fixture。
- [ ] 2.5 [PRC-06] 空格/CJK/特殊字符 argv + split UTF-8 输出测试。
- [ ] 2.6 [PRC-07] repeated execution resource leak 测试。

## 3. GREEN
- [ ] 3.1 [PRC-01] 实现 bounded stdout/stderr sink，继续排空。
- [ ] 3.2 [PRC-02] 实现 immutable execution context snapshot。
- [ ] 3.3 [PRC-03,PRC-04] 扩展 result/outcome metadata 和 terminal cleanup。
- [ ] 3.4 [PRC-05] 实现两阶段 termination 与平台适配。
- [ ] 3.5 [PRC-06] 固化 argv/UTF-8 decode。
- [ ] 3.6 [PRC-07] 集中资源释放逻辑并保证幂等。

## 4. REFACTOR
- [ ] 4.1 评估 ACP/Server 是否复用 process helper；只有测试证明语义一致才抽取。
- [ ] 4.2 审查默认 output limit/timeout 的迁移影响并更新 README。

## 5. 三线验证
- [ ] 5.1 Java 21/macOS/Linux/Windows 能覆盖的平台运行 process suite。
- [ ] 5.2 Java 17 重复同 suite。
- [ ] 5.3 Java 8 验证替代 process-tree 策略，不能仅编译通过。
- [ ] 5.4 压力与泄漏测试、依赖安全检查、CodeGraph impact review 均记录实际证据。
