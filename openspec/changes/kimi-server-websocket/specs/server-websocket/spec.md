# Server WebSocket 契约

## Purpose

让 Java SDK 能安全、可恢复、可关联地消费 Kimi Web Server 的实时事件。

## ADDED Requirements

### Requirement: WSS-01 Verified handshake and authentication

SDK MUST 按固定目标版本已验证的 WebSocket handshake 与认证契约连接，不得猜测或记录敏感 token。

#### Scenario: Valid authentication

- **WHEN** 调用者使用有效 server endpoint/token 建立事件连接
- **THEN** SDK 按已验证 wire contract 完成握手并进入 OPEN。

#### Scenario: Authentication rejected

- **WHEN** server 拒绝认证
- **THEN** SDK 返回 authentication 类终态，不进入无限重连，也不在日志中暴露 token。

### Requirement: WSS-02 Correlated event delivery

SDK MUST 使用上游可验证的 correlation 字段将 WS 事件关联到正确 session/turn/message/tool，不得按“最近请求”猜测归属。

#### Scenario: Concurrent sessions

- **WHEN** 两个 session 同时通过同一 server 产生实时事件
- **THEN** canonical events 保持正确 session/turn 归属，不交叉拼接。

### Requirement: WSS-03 No implicit prompt replay

WebSocket 断线或重连 MUST NOT 自动重新提交 REST prompt，除非未来独立规格明确提供可证明安全的幂等语义。

#### Scenario: Disconnect after prompt accepted

- **WHEN** prompt 已被 server 接受后 WS 断线
- **THEN** SDK 可以尝试恢复事件连接，但不得自动再次 POST 同一 prompt。

### Requirement: WSS-04 Bounded reconnect

可配置重连 SHALL 具有次数/时间上限，并区分 transient failure、authentication/protocol failure 和 explicit close。

#### Scenario: Repeated network failure

- **WHEN** 网络持续失败且达到策略上限
- **THEN** client 进入明确失败终态，停止创建新的 reconnect task。

#### Scenario: Explicit close during backoff

- **WHEN** client 正在等待重连且调用者 close
- **THEN** backoff task 被取消，连接最终 CLOSED，不再发起握手。

### Requirement: WSS-05 Bounded message processing

SDK MUST 对 frame、待处理消息和异步派发使用有界资源，并在超限时显式报告。

#### Scenario: Oversized frame

- **WHEN** server 发送超过配置上限的 frame
- **THEN** SDK 终止或拒绝该连接并返回明确 protocol/resource-limit 错误，不无限扩展内存。

### Requirement: WSS-06 Unknown event compatibility

SDK MUST 保留未知 WS event 的 type/raw payload，并通过 Unified Events 的 unknown 机制交付或明确失败。

#### Scenario: New event type

- **WHEN** server 返回 SDK 未识别的新事件
- **THEN** 已建立连接不因可安全忽略/保留的未知事件崩溃，调用者可取得 raw 信息。

### Requirement: WSS-07 Ownership-aware close

关闭 WebSocket client MUST 只释放其拥有的连接、线程、队列和 timer；对 external server 不执行 shutdown。

#### Scenario: Attached external server

- **WHEN** 对 attach 模式创建的 WS client 执行 close
- **THEN** 外部 Kimi server 继续运行，SDK 本地资源全部释放。
