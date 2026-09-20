# Unified Events 契约

## Purpose

为所有 Kimi Runtime transport 提供一致、可关联、可扩展的事件语义。

## ADDED Requirements

### Requirement: EVT-01 Canonical event taxonomy

SDK SHALL 将已验证的 runtime、session、turn、assistant message、tool call、usage 和 error 事件映射为稳定的强类型事件类别。

#### Scenario: Assistant text delta

- **WHEN** transport 产生已验证的 assistant message 增量
- **THEN** 调用者收到 AssistantMessageDelta 或等价强类型事件，而无需解析 transport-specific type 字符串。

### Requirement: EVT-02 Correlation isolation

事件 MUST 保留 transport 可提供的 session、turn/request、message 和 tool correlation 信息，并不得把不同执行的增量错误合并。

#### Scenario: Concurrent sessions

- **WHEN** 两个 session 同时产生文本和 tool events
- **THEN** 每个事件可关联到正确 session/turn，listener 不发生跨 session 串流。

### Requirement: EVT-03 Ordered delivery per stream

SDK MUST 对同一已建立事件流保持观察顺序，不重排 message/tool delta。

#### Scenario: Split deltas

- **WHEN** transport 按 A、B、C 顺序给出同一 message 的三个 delta
- **THEN** consumer 观察到的顺序仍为 A、B、C。

### Requirement: EVT-04 Single terminal event

每个 turn MUST 只产生一个业务终态，迟到或重复 transport 终态不得再次完成该 turn。

#### Scenario: Completion followed by late error

- **WHEN** turn 已完成后 transport 又到达迟到 error
- **THEN** 原 turn 结果不被覆盖；迟到事件按诊断策略处理。

### Requirement: EVT-05 Unknown event preservation

SDK MUST 对未知上游事件保留原 type 和 raw payload，除非无法安全继续而明确报告 protocol failure。

#### Scenario: Future event type

- **WHEN** 上游新增 SDK 未识别的事件
- **THEN** 调用者可以观察 UnknownRuntimeEvent/raw payload，不发生静默丢弃。

### Requirement: EVT-06 Listener isolation

用户事件 listener 的异常 MUST NOT 杀死底层 transport reader 或破坏其他订阅。

#### Scenario: Listener throws

- **WHEN** 一个 listener 在处理 delta 时抛异常
- **THEN** 该失败被明确报告，其他 session/订阅仍可继续处理。

### Requirement: EVT-07 Bounded asynchronous delivery

任何异步/Flow 事件 adapter MUST 使用有界缓冲并公开溢出语义，不得以无限队列隐藏慢消费者。

#### Scenario: Slow consumer

- **WHEN** consumer 长期低于事件生产速度并达到上限
- **THEN** SDK 按明确策略失败/取消/降级，不无限增长内存，也不静默丢业务事件。
