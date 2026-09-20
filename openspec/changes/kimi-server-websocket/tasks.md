# kimi-server-websocket 实施任务

## 1. 上游协议门禁
- [ ] 1.1 [WSS-01..07] 固定目标 Kimi Code 版本和二进制/来源；记录 `/api/v1/ws` 实际 handshake。
- [ ] 1.2 [WSS-01] 记录认证方式、正常关闭码、错误关闭码、heartbeat/keepalive 行为；未知项不得猜测。
- [ ] 1.3 [WSS-02,WSS-06] 采集最小真实 event fixtures 和 correlation 字段。
- [ ] 1.4 执行 `openspec validate kimi-server-websocket --strict`。

## 2. RED
- [ ] 2.1 [WSS-01] 本地 WS server 写 auth success/failure 测试。
- [ ] 2.2 [WSS-02,WSS-03] 写双 session + disconnect-after-prompt 测试，证明不串流且不重复 POST。
- [ ] 2.3 [WSS-04] 写 reconnect 上限、认证失败不重连、close-during-backoff 测试。
- [ ] 2.4 [WSS-05] 写 oversized frame、slow consumer、queue overflow 测试。
- [ ] 2.5 [WSS-06] 写 unknown event fixture。
- [ ] 2.6 [WSS-07] 写 external server ownership/close 测试。

## 3. GREEN
- [ ] 3.1 [WSS-01] 实现已验证 handshake/auth。
- [ ] 3.2 [WSS-02,WSS-06] 实现 frame decode、typed mapping、correlation。
- [ ] 3.3 [WSS-03] 高层 prompt+stream 流程保证请求最多一次。
- [ ] 3.4 [WSS-04] 实现有界 reconnect policy。
- [ ] 3.5 [WSS-05] 实现 bounded frame/queue。
- [ ] 3.6 [WSS-07] 实现幂等 close 和资源释放。

## 4. REFACTOR / SECURITY
- [ ] 4.1 token/headers/log/toString 全面脱敏测试。
- [ ] 4.2 审查新增 WebSocket 依赖许可证、CVE、Java 8 支持和二进制体积。

## 5. 三线验证
- [ ] 5.1 feature/3.0.x 本地 WS + 真实 Kimi contract tests。
- [ ] 5.2 feature/2.0.x 同语义验证。
- [ ] 5.3 feature/1.0.x 同语义验证。
- [ ] 5.4 thread/socket/timer/queue leak tests 通过后才能标记完成。
