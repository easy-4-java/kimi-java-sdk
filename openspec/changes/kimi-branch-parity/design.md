# Kimi 三分支能力一致性技术设计

## Context

当前实际差异：
- 1.0.x：Java 8、Jackson 2.18，CLI executor 有 Java 8 UTF-8 helper；
- 2.0.x：Java 17、Jackson 2.22；
- 3.0.x：Java 21、Maven model 4.1、Jackson 3.x。
业务类和调用图高度一致。

因此 parity 不能用源码 hash 判断，应通过 contract + public surface + semantic fixture 判断。

## Decisions

### D1. Capability Baseline

每个已批准 Change 必须声明 `branch_support`。默认全部 required。只有“明确的 Java-version-specific optional adapter”可以不在 1.x 存在，但其缺失不能降低核心业务 capability。例如 Java 17/21 可提供 Flow adapter，但 STREAMING capability 在 Java 8 仍由 callback/listener 实现。

### D2. Allowed Differences

允许：
- Java language/runtime API；
- Jackson 2 vs 3 import/annotation/mapper API；
- Maven model/plugin/JUnit 版本；
- OS/JDK process cleanup adapter；
- optional modern-JDK convenience adapter。

禁止：
- 不同协议语义；
- 不同 Runtime capability；
- 不同 session/cancel/timeout/error semantics；
- 不同 typed model 业务字段含义；
- 不同 security defaults。

### D3. Contract Test Source

优先维护逻辑上相同的 test classes/fixtures。若三分支无法共享物理文件，则通过脚本比较规范编号和 fixture hash/semantic snapshot，防止某分支悄悄少测试。

### D4. Public API Parity

建立 API surface report，忽略已批准的 Java-version optional adapter 后，核心 package 的 class/method/signature 语义必须一致。Java 类型签名因 Jackson package 不应泄露到统一 public domain API；raw API 的 Jackson 差异可列入允许清单。

### D5. CI Matrix

每条分支使用对应真实 JDK：
- 1.0.x → JDK 8；
- 2.0.x → JDK 17；
- 3.0.x → JDK 21。

CI job 名称、注释和实际 java-version 必须一致。不能出现“Set up JDK 21”但实际配置 8/17 的维护误导。

### D6. Sync Workflow

推荐：
1. OpenSpec / Contract Test 批准；
2. 3.0.x RED → GREEN → REFACTOR；
3. 2.0.x 移植，只做兼容适配；
4. 1.0.x 移植，只做兼容适配；
5. parity report；
6. 三线 production gate。

如果 bug 首先在旧分支发现，应先用共享 failing contract 固化，再同步所有受影响分支，而不是只修单线。

### D7. Production Gate

每条线至少：
- compile；
- unit；
- integration/E2E（按能力）；
- resource/process/thread leak；
- dependency vulnerability scan；
- API parity；
- capability parity；
- fixture semantic parity；
- CodeGraph impact review；
- OpenSpec task evidence。

“某平台无法运行”必须标记 NOT_RUN，不算通过。

### D8. Evidence

CI 结果必须记录 branch、commit SHA、JDK、Maven、OS、命令、exit code、test count。禁止只说“CI green”而没有可追溯运行上下文。

## Migration

先建立 parity 工具和当前 baseline，不要求一次把所有历史 divergence rebase/merge。未来每个 Change 按本规则同步。

## Rollback

每条分支使用普通 revert，不 force-push 历史。若新功能必须回滚，应在三线同步回滚或记录明确暂时阻塞状态，不能悄悄造成能力漂移。

## Validation

对本仓库当前三线生成：
- dependency/build baseline；
- public API snapshot；
- capability matrix；
- test inventory；
- CodeGraph node/edge summary；
- CI config consistency report。
