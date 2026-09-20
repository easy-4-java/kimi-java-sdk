# kimi-unified-events 实施任务

## 1. 规范门禁
- [ ] 1.1 [EVT-01..07] 评审事件 taxonomy、correlation 和 terminal 语义。
- [ ] 1.2 执行 `openspec validate kimi-unified-events --strict`。
- [ ] 1.3 收集 CLI stream-json、ACP update 与 Server WS（若已核验）fixtures。

## 2. RED
- [ ] 2.1 [EVT-01,EVT-05] 为已知/未知 event mapping 写失败测试。
- [ ] 2.2 [EVT-02,EVT-03] 写多 session correlation 与顺序测试。
- [ ] 2.3 [EVT-04] 写 duplicate/late terminal 竞态测试。
- [ ] 2.4 [EVT-06] 写 listener throw isolation 测试。
- [ ] 2.5 [EVT-07] 写 slow consumer/bounded queue 测试。

## 3. GREEN
- [ ] 3.1 [EVT-01] 实现 Java 8 可表达的 canonical event hierarchy。
- [ ] 3.2 [EVT-02,EVT-03] 实现 correlation envelope 与顺序派发。
- [ ] 3.3 [EVT-04] 接入单终态 guard。
- [ ] 3.4 [EVT-05] 实现 UnknownRuntimeEvent/raw payload。
- [ ] 3.5 [EVT-06] 实现 listener isolation。
- [ ] 3.6 [EVT-07] 实现可配置 bounded async/Flow adapter。

## 4. Transport adapters
- [ ] 4.1 CLI KimiEvent → canonical event；无法判断时映射 Unknown。
- [ ] 4.2 ACP session/update → canonical event；legacy onDelta 保持兼容。
- [ ] 4.3 Server WS 完成后接入同一 mapper，不另建平行事件体系。

## 5. 三线验证
- [ ] 5.1 三分支运行相同 event fixture snapshot。
- [ ] 5.2 Java 17/21 Flow adapter 与 Java 8 listener 的业务事件序列一致。
- [ ] 5.3 重复压力测试确认无 queue/listener/thread 泄漏。
