# kimi-observability 实施任务

## 1. 门禁
- [ ] 1.1 [OBS-01..07] 评审 observer SPI、metric semantics、redaction 和 high-cardinality 禁止项。
- [ ] 1.2 执行 `openspec validate kimi-observability --strict`。
- [ ] 1.3 列出 CLI/ACP/Server/WS 可观测点和 ownership，避免重复计数。

## 2. RED
- [ ] 2.1 [OBS-01,OBS-07] dependency test：核心 SDK 无 vendor telemetry 也能运行。
- [ ] 2.2 [OBS-02] success/failure/cancel/timeout 的 gauge/counter balance 测试。
- [ ] 2.3 [OBS-03] 大量失败 diagnostic bounded test。
- [ ] 2.4 [OBS-04] local/upstream correlation source test。
- [ ] 2.5 [OBS-05] token/prompt/env redaction test。
- [ ] 2.6 [OBS-06] observer throw isolation test。

## 3. GREEN
- [ ] 3.1 [OBS-01] 实现 neutral observer + no-op。
- [ ] 3.2 [OBS-02] 接入 process/session/turn/RPC/timeouts/cancel 等稳定打点。
- [ ] 3.3 [OBS-03] 实现 bounded diagnostic snapshot。
- [ ] 3.4 [OBS-04] 实现 correlation context/source。
- [ ] 3.5 [OBS-05] 集中 redaction utility/logging policy。
- [ ] 3.6 [OBS-06] observer failure isolation。

## 4. Optional adapters
- [ ] 4.1 设计但不强制实现 Micrometer adapter；如实现必须单独依赖。
- [ ] 4.2 设计但不强制实现 OpenTelemetry adapter；trace attribute 遵守低基数和脱敏约束。

## 5. 三线验证
- [ ] 5.1 三分支运行相同 observability Contract Tests。
- [ ] 5.2 dependency tree 比较，确认核心未引入 vendor runtime。
- [ ] 5.3 压力测试 observer/no-op/throw 场景，无业务结果变化和资源泄漏。
