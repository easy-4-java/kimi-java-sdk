# kimi-acp-lifecycle-hardening 评审记录

## Decision

**APPROVED FOR IMPLEMENTATION / IN PROGRESS**

用户于 2026-09-20 明确要求进入 OpenSpec 评审、先执行 8 个 Change 的 strict validate，并从本 Change 开始严格 TDD 实现。

本记录只批准 `kimi-acp-lifecycle-hardening` 已书面定义的范围，不扩展到 Runtime Foundation、Typed Protocol、WebSocket 或其他 Change。

## Specification Gate

- OpenSpec CLI: `1.13.1`
- GitHub Actions run: `35519997399`
- Job: `106102324513`
- Command:
  `openspec validate kimi-acp-lifecycle-hardening --type change --strict --no-interactive`
- Result: `Change 'kimi-acp-lifecycle-hardening' is valid`
- Status: **PASS**

其余 7 个 Change 在同一 matrix run 中也均 strict validate PASS；详见 evidence.md。

## TDD Review

实现严格分为测试提交与生产代码提交，RED 失败证据在修复前由 CI 产生：

1. RED-1 `e247907b...` → 4 个预期失败。
2. GREEN-1 `24a51acd...` → 72/72 tests PASS。
3. RED-2 `8d1e8685...` → cancel 终态 1 个预期失败。
4. GREEN-2 `173d3f70...` → CI PASS。
5. RED-3 `4ceb2460...` → lifecycle state 3 个预期 error。
6. GREEN-3 `409b7cc3...` → 78/78 tests PASS。

三条兼容线随后运行相同 hardening tests：
- Java 21 / Jackson 3：PASS
- Java 17 / Jackson 2：PASS
- Java 8 / Jackson 2：PASS

## Remaining Gates

以下内容尚未完成，因此本 Change 仍为 **IN PROGRESS**，不得宣称全部 production-ready：

- response/timeout/cancel/close 的 barrier 级高并发 race 压测；
- 不同 session 的并发 turn isolation 压测；
- stubborn child process 的有界 graceful/force shutdown；
- 线程、timer、process 的重复循环 leak baseline；
- hardening 后重新运行 CodeGraph impact review；
- 最终三线合并后的 CI 与 workspace/release hygiene。

## Approval Boundary

当前允许继续完成上述剩余任务，并在证据齐全后把三个兼容线 PR 转 Ready/合并。
