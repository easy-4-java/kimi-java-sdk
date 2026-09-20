# Kimi Unified Events 技术设计

## Context

统一事件模型不能简单把所有 wire event 改名。CLI/ACP/WS 可能具有不同粒度、时序和字段，设计必须保留 correlation、unknown data 和 transport diagnostics。

## Decisions

### D1. Canonical Event Families

建议公共事件族：
- RuntimeStarted / RuntimeStopped / RuntimeFailed；
- SessionStarted / SessionResumed / SessionClosed；
- TurnStarted / TurnCompleted / TurnFailed / TurnCancelled；
- AssistantMessageStarted / Delta / Completed；
- ToolCallStarted / Delta / Completed；
- UsageUpdated；
- UnknownRuntimeEvent。

Java 8 使用 interface/abstract base + enum type，不使用 sealed class。

### D2. Correlation Envelope

每个可关联事件携带可用的 runtimeId、sessionId、turnId/requestId、messageId、toolCallId 和 sequence/observedAt。并非每个 transport 都能提供全部字段；缺失必须显式为空，不得伪造随机 ID 冒充上游 correlation。SDK 自生成本地 runId 时标记 namespace/source。

### D3. Ordering

SDK SHALL 保证同一 transport connection 上“已观察到”的事件以解析顺序派发。同一 session/turn 的 delta 不重排。跨 session 不承诺全局业务顺序。异步 listener adapter 不得改变单 stream 的顺序。

### D4. Terminal Semantics

一个 turn 只产生一个 terminal outcome：completed、failed 或 cancelled。transport 后续迟到事件可以记录为 diagnostic/ignored-late-event，但不得再次完成业务 Future。

### D5. Unknown Events

上游新增 event type 时返回 UnknownRuntimeEvent，保留原 type/raw payload，不静默丢弃。若 unknown event 属于 transport control 且不能安全继续，允许显式 protocol failure，但要保留原始诊断。

### D6. Listener Isolation and Backpressure

核心 listener 契约同步、顺序、可预测。用户 listener 抛错不允许杀死 transport reader。是否终止该订阅由策略决定，默认记录 listener failure 并停止向该 listener 继续派发，但 transport 可继续服务其他订阅。

对异步/Flow adapter 必须有 bounded queue；队列溢出默认显式失败或应用调用者选定策略，不能无限增长或静默丢业务事件。

### D7. Legacy Bridges

`KimiEvent` 可通过 mapper 转为 canonical event；无法确定语义时映射 Unknown 而非猜测。ACP `onDelta` 继续作为 convenience callback，只消费 AssistantMessageDelta 文本。Server WS 未来直接映射 canonical events。

## Validation

- fixture-driven event mapping；
- same-session ordering；
- multi-session isolation；
- listener exception；
- slow listener/bounded queue；
- unknown event；
- late event after terminal；
- Java 8 callback 与 Java 17/21 Flow adapter semantic parity。

## Migration / Rollback

先新增 event API，再让新 Runtime facade 使用；旧 callback 保留。回滚不会影响底层 CLI/ACP/REST 协议。
