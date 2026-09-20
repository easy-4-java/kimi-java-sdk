# Kimi Server WebSocket 技术设计

## Context

当前 Server Client 已支持 start/attach 两种模式，并维护 baseUrl/token。WebSocket 应复用同一 Server identity，但其连接、重连和消息消费拥有独立生命周期。

## Decisions

### D1. 上游契约先验收后实现

必须先记录目标 Kimi Code 版本、真实 `/api/v1/ws` handshake、认证位置（header/query/subprotocol 等）、server event fixture、关闭码、keepalive 行为。规范只要求“按已验证契约执行”，不凭注释猜 wire 格式。

### D2. WebSocket Client 生命周期

建议状态：NEW → CONNECTING → OPEN → RECONNECT_WAIT（可选） → CLOSING → CLOSED；不可恢复协议/认证错误进入 FAILED。close 幂等，关闭后不重连。

### D3. Authentication Ownership

复用 `KimiServerConfig` 的 token source，但 token 只在握手所需位置注入；日志、异常、toString、metrics attribute 均不得输出原值。external attach 的 token 所有权仍属于调用者。

### D4. Event Correlation

WS frame 先解析 transport envelope，再映射 typed protocol/canonical events。优先使用上游提供的 session/message/turn/request/tool ids；没有可靠 correlation 时不得把事件猜到最近一次 prompt。

### D5. REST + WS Race

提交 prompt 前是否必须确保 WS OPEN/ready，由目标协议行为决定。首版建议：高层 streaming prompt 先建立并确认事件连接，再发 REST prompt；若无法证明 server 订阅已生效，需真实测试确定 readiness signal。请求最多发送一次，断线不自动重发 prompt。

### D6. Reconnect

只在网络类 transient failure 且调用者策略允许时重连。认证失败、协议不兼容、显式 close 不重连。使用 bounded exponential backoff + jitter，上限和次数可配置。没有上游 resume cursor 时，重连只能恢复未来事件，不宣称补齐断线期间事件。

### D7. Heartbeat

若上游有 ping/pong 或应用 heartbeat，按固定版本实现；若没有，不自创业务帧。无论哪种方式，都要有 idle/read deadline 防止半开连接永久占用资源。

### D8. Bounded Resources

限制：
- 单 frame 最大字节/字符；
- 待解析消息数量；
- listener/async queue；
- reconnect task 数；
- 累计 diagnostic tail。

达到上限默认显式失败当前连接/订阅，不无限内存增长。

### D9. Java Compatibility

公共 API 不暴露 JDK 11+ WebSocket 类型。实现可使用兼容第三方库或分支内部 adapter；三线行为契约一致。依赖选择在实现评审确定。

## Validation

使用本地可控 WS server + 固定真实 Kimi fixture，覆盖：
- auth success/failure；
- fragmented frames；
- unknown event；
- normal/abnormal close；
- reconnect；
- close during reconnect；
- slow consumer；
- oversized frame；
- REST prompt + event correlation；
- external/owned server ownership。

## Migration / Rollback

WebSocket 是增量能力；REST API 不删除。出现兼容问题可 revert WS adapter，不影响现有 Server REST。
