# kimi-java-sdk 生产级 Runtime SDK 优化架构与实施规划

> Status: Draft  
> Scope: `easy-4-java/kimi-java-sdk`  
> Branches: `feature/1.0.x` / `feature/2.0.x` / `feature/3.0.x`  
> Target: Production-ready Kimi Code Java Runtime SDK  
> Implementation Method: OpenSpec + Strict TDD

---

## 1. 文档目的

`kimi-java-sdk` 当前已经完成了对 Kimi Code 三类主要运行方式的 Java 封装：

1. CLI 子进程调用；
2. ACP 长生命周期 JSON-RPC 调用；
3. `kimi web` HTTP Server 调用。

当前项目已经具备较完整的基础能力，但整体仍处于：

> **CLI / ACP / Web 能力封装 SDK**

阶段。

下一阶段目标不是继续零散增加 API，而是将其升级为：

> **Production-ready Kimi Agent Runtime Java SDK**

即：

```text
Java Application
       │
       ▼
kimi-java-sdk
       │
       ├── Runtime Abstraction
       ├── Capability Discovery
       ├── Typed Protocol
       ├── Unified Streaming
       ├── Lifecycle Management
       ├── Observability
       └── Reliability
              │
              ▼
          Kimi Code
```

同时必须保持：

```text
feature/1.0.x → Java 8
feature/2.0.x → Java 17
feature/3.0.x → Java 21
```

三条维护线在**业务能力、协议语义和测试行为上保持一致**。

---

## 2. 当前代码基线

### 2.1 三分支定位

当前三个分支并不是三个产品功能版本，而是三条 Java Compatibility Line。

| Branch | Java | Maven Model | Jackson | 定位 |
|---|---:|---:|---:|---|
| `feature/1.0.x` | 8 | 4.0.0 | 2.18.x | Legacy Java |
| `feature/2.0.x` | 17 | 4.0.0 | 2.22.x | Modern Java |
| `feature/3.0.x` | 21 | 4.1.0 | 3.2.x | Current Java |

因此必须建立以下长期规则：

> **版本号代表 Java Runtime Baseline，不代表 SDK Capability Level。**

禁止出现：

```text
1.x 只有 A/B
2.x 多了 C
3.x 才有 D/E/F
```

正确模型必须是：

```text
                Shared Capability Baseline
                         │
          ┌──────────────┼──────────────┐
          │              │              │
       1.0.x          2.0.x          3.0.x
       Java 8         Java 17         Java 21
```

三条线原则上拥有相同：

- public API；
- Runtime 能力；
- ACP 协议能力；
- Server API；
- Event 模型；
- Exception 语义；
- 测试案例；
- 行为约束。

只允许存在必要的技术栈兼容差异。

---

## 3. CodeGraph 当前分析结果

三个当前分支经过 CodeGraph 实际索引：

```text
feature/1.0.x

Files: 25
Nodes: 609
Edges: 1,283
```

```text
feature/2.0.x

Files: 25
Nodes: 608
Edges: 1,278
```

```text
feature/3.0.x

Files: 25
Nodes: 608
Edges: 1,278
```

1.x 多出的结构主要来自 Java 8 UTF-8 compatibility helper。

2.x 与 3.x 调用图完全一致。

这证明当前三条分支业务架构已经高度一致，应继续强化这种模式，而不能让三条线再次独立演进。

---

## 4. 当前总体架构

当前项目实际存在三个 Runtime Surface：

```text
                         kimi-java-sdk
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
      CLI Route            ACP Route           WEB Route
         │                    │                    │
    KimiClient          KimiAcpClient      KimiServerClient
         │                    │                    │
      KimiCli              JSON-RPC             REST
         │                  stdio               HTTP
 KimiCliExecutor             │                    │
         │                    │                    │
   Commons Exec           kimi acp             kimi web
         │                    │                    │
         └────────────────────┼────────────────────┘
                              │
                           Kimi Code
```

---

## 5. 当前三个 Runtime 的职责

### 5.1 CLI Runtime

主要类：

```text
KimiClient
KimiCli
KimiCliExecutor
KimiCliResult
KimiEvent
```

主要能力：

- prompt；
- model；
- session；
- continue；
- login；
- doctor；
- export；
- migrate；
- upgrade；
- provider；
- web；
- ACP command；
- stream-json 输出解析。

典型调用链：

```text
KimiClient.promptAndParse()
        │
        ▼
KimiCli.promptJson()
        │
        ▼
KimiCli.prompt(RunOptions)
        │
        ▼
KimiCliExecutor.execute()
        │
        ▼
KimiCliExecutor.runProcess()
        │
        ▼
kimi
        │
        ▼
stream-json
        │
        ▼
KimiEvent
```

CLI Runtime 适合：

- 单次 Prompt；
- 自动化任务；
- 简单同步调用；
- CLI 管理操作。

---

## 6. ACP Runtime

当前 ACP 是整个 SDK 中最重要的 Runtime。

```text
KimiAcpClient
       │
       ▼
ProcessBuilder
       │
       ▼
kimi acp
       │
       ▼
JSON-RPC 2.0 / stdio
```

已经实现：

```text
initialize
authenticate
logout

session/new
session/load
session/resume
session/list
session/fork
session/close
session/delete

session/prompt
session/cancel

session/set_model
session/set_mode
```

内部具备：

```text
pendingRpcs
promptStreams
rpcIds
reader thread
timer
CompletableFuture
Process lifecycle
```

ACP Runtime 实际已经具备 Agent Runtime 的基本形态：

```text
Long-running Runtime
      │
      ├── Session
      ├── Streaming
      ├── Async
      ├── Cancel
      ├── RPC correlation
      └── Runtime lifecycle
```

因此后续设计中：

> **ACP 应成为 kimi-java-sdk 的主 Runtime。**

CLI 和 Server 是其他运行方式，而不是 ACP 的替代品。

---

## 7. Server Runtime

Server Runtime：

```text
KimiServerClient
       │
       ├── start()
       │      │
       │      └── spawn kimi web
       │
       └── attach()
              │
              └── existing kimi web
```

当前提供：

```text
meta
healthz

createSession
listSessions

postPrompt
messages
abort

GET
POST
DELETE
```

生命周期分为：

```text
SDK owned server
      │
      └── start()

Externally managed server
      │
      └── attach()
```

这一设计应该保留。

当前最大能力缺口是：

```text
/api/v1/ws
```

WebSocket Event Channel 仍未进入 SDK。

---

## 8. 当前主要问题

当前不是缺少基础功能，而是存在八类架构债务。

### 8.1 Runtime API 不统一

现在调用方需要自己理解：

```text
KimiClient
KimiAcpClient
KimiServerClient
```

分别是什么。

未来 Agent Buddy 等系统不应该直接依赖这些实现细节。

### 8.2 协议对象弱类型

目前大量：

```java
JsonNode
Map<String, Object>
Object
```

存在于正式 API 中。

典型例如：

```text
KimiEvent.toolCalls → Object
KimiEvent.data      → Object
KimiEvent.error     → Object
```

这会把协议解析责任重新推给 SDK 用户。

### 8.3 Event Model 不完整

当前：

```text
KimiEvent
├── type
├── content
├── toolCalls
├── data
└── error
```

无法形成稳定的事件驱动 API。

### 8.4 Streaming API 不统一

CLI、ACP 和 Server 使用不同模式表达 Streaming。

调用方无法使用统一代码消费事件。

### 8.5 ACP 生命周期仍需要生产级强化

需要重点处理：

```text
Process crash
initialize timeout
reader thread exit
close + prompt race
cancel + complete race
timeout + result race
pending RPC cleanup
callback exception
connection broken
```

### 8.6 Server 缺少 WebSocket

REST 只能完成 Command/Query。

Agent 的实时事件仍然无法作为正式 SDK API 消费。

### 8.7 Process 输出无上限

当前：

```text
ByteArrayOutputStream stdout
ByteArrayOutputStream stderr
```

会持续将输出缓存到 JVM Heap。

大输出或长时间 Agent Run 存在潜在 OOM 风险。

### 8.8 三分支容易再次漂移

当前三个 Git 分支已经发生历史 divergence。

如果没有明确同步策略，未来：

```text
1.x fix A
2.x fix B
3.x feature C
```

会快速导致三条线无法维护。

---

## 9. 目标架构

目标升级为：

```text
                       kimi-java-sdk
                            │
                    Public SDK Facade
                            │
             ┌──────────────┴──────────────┐
             │                             │
        Typed API                    Raw Escape Hatch
             │                             │
             ▼                             ▼
        KimiRuntime                    JsonNode
             │
       Capability Layer
             │
       Event / Streaming
             │
       Lifecycle Layer
             │
   ┌─────────┼───────────────┐
   │         │               │
 CLI       ACP            Server
Runtime   Runtime         Runtime
   │         │               │
Process   JSON-RPC       HTTP + WS
   │         │               │
   └─────────┼───────────────┘
             │
          Kimi Code
```

---

## 10. Runtime SPI

新增统一 Runtime abstraction。

建议概念模型：

```java
public interface KimiRuntime extends AutoCloseable {

    KimiCapabilities capabilities();

    KimiHealth health();

    KimiPromptResult prompt(KimiPromptRequest request);

    void cancel(String runId);

    @Override
    void close();
}
```

具体 Runtime：

```text
KimiRuntime
├── KimiCliRuntime
├── KimiAcpRuntime
└── KimiServerRuntime
```

Runtime 不要求所有能力完全相同。

通过：

```text
KimiCapabilities
```

声明能力。

---

## 11. Capability API

Capability 不只是 Kimi SDK 内部需要，未来所有 Agent Runtime SDK 都应该具备。

建议：

```text
KimiCapabilities
│
├── PROMPT
├── STREAMING
├── SESSION
├── SESSION_RESUME
├── SESSION_FORK
├── CANCEL
├── MODEL_SWITCH
├── MODE_SWITCH
├── TOOLS
├── SKILLS
├── MCP
├── PROVIDERS
├── ACP
├── SERVER
└── WEBSOCKET
```

调用方：

```java
if (runtime.capabilities().supports(KimiCapability.STREAMING)) {
    ...
}
```

禁止上层系统依赖：

```java
if (runtime instanceof KimiAcpClient)
```

这对 Agent Buddy 尤其重要。

---

## 12. Typed Protocol Model

JSON Raw API 保留，但正式 Public API 应逐步强类型化。

目标模型：

```text
KimiSession

KimiPromptRequest
KimiPromptResult

KimiMessage
KimiMessageRole

KimiContentBlock
├── KimiTextBlock
├── KimiToolCallBlock
├── KimiToolResultBlock
└── KimiErrorBlock

KimiModel
KimiMode
KimiProvider
KimiUsage
KimiStopReason
KimiError
```

Raw API：

```java
JsonNode rawRequest(...);
```

仍允许高级使用者访问底层协议。

原则：

> Typed API 为默认，Raw API 为 escape hatch。

---

## 13. Unified Event Model

事件统一为：

```text
KimiRuntimeEvent
│
├── RuntimeStarted
├── RuntimeStopped
│
├── SessionStarted
├── SessionResumed
├── SessionClosed
│
├── TurnStarted
│
├── AssistantMessageStarted
├── AssistantMessageDelta
├── AssistantMessageCompleted
│
├── ToolCallStarted
├── ToolCallDelta
├── ToolCallCompleted
│
├── UsageUpdated
│
├── ErrorEvent
│
└── TurnCompleted
```

调用方不应该继续编写：

```java
if ("agent_message_chunk".equals(event.getType())) {
}
```

而应该能够：

```java
if (event instanceof AssistantMessageDelta) {
}
```

---

## 14. Unified Streaming

统一 Streaming API。

基础兼容层：

```java
KimiEventListener
```

例如：

```java
runtime.prompt(
    request,
    event -> {
        ...
    }
);
```

Java 8：

```text
Callback
CompletableFuture
```

Java 17/21 可以在不改变核心 API 的前提下提供额外 adapter：

```text
Flow.Publisher<KimiRuntimeEvent>
```

禁止为了 Java 21 API 破坏 1.x 与 2.x 的能力一致性。

高级 API 应作为 optional adapter，而不是形成三套业务 API。

---

## 15. ACP Runtime 强化

ACP 是 P0 核心。

### 15.1 生命周期状态机

明确：

```text
NEW
 │
 ▼
CONNECTING
 │
 ▼
INITIALIZING
 │
 ▼
READY
 │
 ├───────────────┐
 │               │
 ▼               ▼
CLOSING        FAILED
 │
 ▼
CLOSED
```

所有 API 必须检查合法状态。

### 15.2 Process Unexpected Exit

必须实现：

```text
child process exited
        │
        ├── mark runtime FAILED
        ├── fail pendingRpcs
        ├── fail active prompts
        ├── stop reader
        └── emit RuntimeFailed event
```

不允许出现：

```text
process 已死
future 永远 pending
```

### 15.3 Pending RPC Cleanup

任何：

```text
success
error
timeout
cancel
close
process exit
```

都必须确保：

```text
pendingRpcs.remove(id)
```

最终成立。

---

## 16. ACP Timeout / Cancel Race

需要专项覆盖：

```text
Result arrives
    VS
Timeout fires
```

```text
Result arrives
    VS
Cancel requested
```

```text
close()
    VS
prompt()
```

必须保证：

> 一个 Future 只能有一个 terminal outcome。

并确保不会：

- duplicate completion；
- pending map 泄露；
- prompt stream 泄露；
- callback 重复；
- timer task 泄露。

---

## 17. Callback Isolation

用户 callback 不能破坏 ACP Reader Thread。

例如：

```java
onDelta.accept(text);
```

如果业务 callback 抛异常：

```text
RuntimeException
```

不能导致：

```text
reader thread terminate
→ 整个 ACP connection 死亡
```

需要隔离：

```text
try callback
catch callback exception
record/log
continue transport processing
```

---

## 18. Server WebSocket Runtime

新增：

```text
KimiServerEventClient
```

能力：

```text
connect
disconnect
subscribe
heartbeat
reconnect
session events
message events
tool events
runtime events
error handling
```

架构：

```text
KimiServerRuntime
      │
      ├── REST
      │     └── command/query
      │
      └── WebSocket
            └── events
```

最终：

```text
POST Prompt
     │
     ▼
Kimi Server
     │
     ▼
WebSocket Events
     │
     ▼
KimiRuntimeEvent
```

---

## 19. Process Runtime Hardening

`KimiCliExecutor` 必须升级为更生产级的 Process Runner。

增加：

```text
workingDirectory
environment
stdoutLimit
stderrLimit
processId
startupTime
executionTime
gracefulShutdownTimeout
forceKillTimeout
```

---

## 20. Output Memory Protection

禁止无限：

```text
ByteArrayOutputStream
```

建议引入 bounded buffer。

例如：

```text
MAX STDOUT = configurable
MAX STDERR = configurable
```

超出后：

```text
keep tail
      +
truncated=true
```

最终 Result：

```text
KimiCliResult
├── exitCode
├── stdout
├── stderr
├── stdoutTruncated
├── stderrTruncated
├── timedOut
└── duration
```

这样既避免 OOM，也保留诊断能力。

---

## 21. Process Tree Termination

部分 Kimi Runtime 可能继续启动子进程。

因此关闭不能只：

```java
process.destroy();
```

需要设计：

```text
graceful terminate
      │
      ▼
wait timeout
      │
      ▼
force terminate
      │
      ▼
process tree cleanup
```

Java 8 与 Java 17/21 API 差异可以由 compatibility adapter 处理。

---

## 22. Exception Hierarchy

当前：

```text
KimiException
```

过于宽泛。

目标：

```text
KimiException
│
├── KimiConfigurationException
│
├── KimiProcessException
│
├── KimiTimeoutException
│
├── KimiProtocolException
│
├── KimiRpcException
│
├── KimiAuthenticationException
│
├── KimiServerException
│
├── KimiSerializationException
│
└── KimiRuntimeClosedException
```

这样上层系统才能判断：

```text
Retry?
Reconnect?
Relogin?
Restart Runtime?
Create New Session?
Fail permanently?
```

---

## 23. Config Model

当前配置为 mutable Lombok Bean。

目标建议采用：

```java
KimiAcpConfig.builder()
    .executable("kimi")
    .connectTimeout(...)
    .readTimeout(...)
    .maxFrameChars(...)
    .build();
```

要求 immutable after build。

避免运行过程中：

```text
config.setTimeout(...)
```

导致线程间语义不一致。

---

## 24. Observability

加入统一观测对象：

```text
KimiRuntimeMetrics
│
├── processStarts
├── processFailures
├── activeSessions
├── activePrompts
├── pendingRpcCount
├── promptDuration
├── rpcDuration
├── timeoutCount
├── cancelCount
├── reconnectCount
└── serializationErrors
```

第一阶段不绑定：

```text
Micrometer
OpenTelemetry
Prometheus
```

而是提供 neutral hooks。

之后可以单独增加 `kimi-java-sdk-micrometer` 等 adapter。

---

## 25. Logging

日志必须区分：

```text
Runtime
Process
Protocol
Session
Turn
Tool
Network
Lifecycle
```

禁止记录：

- API Key；
- Bearer Token；
- authentication payload；
- 完整 Secret；
- 用户敏感环境变量。

Prompt/Response 是否记录必须可配置，默认不得在 INFO 输出完整内容。

---

## 26. 三分支同步规范

这是整个项目后续最重要的工程规则之一。

### 26.1 Capability Parity

每一个 OpenSpec Change 必须声明：

```text
branch_support:

feature/1.0.x: required
feature/2.0.x: required
feature/3.0.x: required
```

除非明确属于 Java-version-specific adapter，否则不能只实现一个分支。

---

## 27. 三分支允许存在的差异

只允许：

```text
Java language/runtime APIs
Jackson 2 vs Jackson 3
JUnit compatibility
Maven model/plugin compatibility
Java-specific process compatibility
```

不得存在：

```text
Different protocol semantics
Different Runtime capabilities
Different session behavior
Different exception semantics
Different lifecycle semantics
```

---

## 28. 分支同步顺序

建议统一采用：

```text
OpenSpec
    │
    ▼
Tests / Contract
    │
    ▼
feature/3.0.x
    │
    ├── feature/2.0.x
    │
    └── feature/1.0.x
```

但：

> 3.0.x 不是产品主版本，而只是实现主线。

同步到 1.x / 2.x 时必须保持行为一致。

---

## 29. Contract Test

建立跨分支相同 Contract Test。

例如：

```text
RuntimeContractTest
PromptContractTest
SessionContractTest
StreamingContractTest
CancelContractTest
LifecycleContractTest
ExceptionContractTest
```

三个分支运行完全相同的行为测试。

这样可以用 CI 自动验证：

```text
1.x behavior
==
2.x behavior
==
3.x behavior
```

---

## 30. TDD 原则

所有改造严格执行：

```text
RED
 │
 ▼
FAIL TEST
 │
 ▼
GREEN
 │
 ▼
MINIMAL IMPLEMENTATION
 │
 ▼
REFACTOR
 │
 ▼
FULL VERIFY
```

禁止先大规模重构，最后再补测试。

---

## 31. P0 实施范围

P0 解决：

> **Runtime 正确性与生产可靠性。**

包括：

```text
P0-01 OpenSpec baseline
P0-02 Cross-branch capability contract
P0-03 ACP lifecycle state machine
P0-04 ACP process-exit propagation
P0-05 Pending RPC cleanup
P0-06 Timeout / Cancel race protection
P0-07 Callback exception isolation
P0-08 Server WebSocket
P0-09 Typed protocol core model
P0-10 Process output memory limits
P0-11 Resource leak tests
P0-12 Three-branch parity CI
```

---

## 32. P1 实施范围

P1 解决：

> **SDK 统一抽象与开发体验。**

包括：

```text
P1-01 KimiRuntime SPI
P1-02 KimiCliRuntime
P1-03 KimiAcpRuntime
P1-04 KimiServerRuntime
P1-05 KimiCapabilities
P1-06 Unified Event Model
P1-07 Unified Streaming
P1-08 Exception Hierarchy
P1-09 Immutable Config Builder
P1-10 Process lifecycle abstraction
P1-11 Raw API escape hatch
```

---

## 33. P2 实施范围

P2 解决：

> **企业集成与 Runtime Ecosystem。**

包括：

```text
P2-01 Metrics
P2-02 Observability hooks
P2-03 OpenTelemetry adapter
P2-04 Retry policy
P2-05 Reconnect policy
P2-06 Spring Boot integration
P2-07 Agent Buddy adapter
P2-08 Runtime diagnostics
P2-09 Advanced examples
P2-10 Production readiness documentation
```

---

## 34. 测试矩阵

### Unit Test

```text
Config
Builder
Event parsing
Protocol serialization
Exception mapping
Capability
Bounded buffer
```

### Process Test

```text
normal exit
non-zero exit
timeout
huge stdout
huge stderr
stdin
UTF-8
process termination
```

### ACP Test

```text
initialize
session/new
session/prompt
stream delta
cancel
timeout
RPC error
process exit
malformed JSON
callback exception
concurrent requests
close during request
```

### Server Test

```text
start
attach
health
session
prompt
abort
HTTP error
auth error
shutdown
WebSocket
reconnect
```

---

## 35. Resource Leak Test

必须明确检查：

```text
Threads
ExecutorService
ScheduledExecutorService
Process
InputStream
OutputStream
HTTP connection
WebSocket
pendingRpcs
promptStreams
scheduled timeout task
```

测试结束后必须满足：

```text
active resources == baseline
```

---

## 36. Production Readiness Gate

每条分支发布前必须经过：

```text
compile
unit tests
integration tests
E2E tests
resource leak tests
thread leak tests
process leak tests
dependency vulnerability scan
static analysis
API compatibility
CodeGraph impact review
```

---

## 37. CI Matrix

最终形成：

```text
                     SDK Contract
                          │
          ┌───────────────┼───────────────┐
          │               │               │
       Java 8          Java 17          Java 21
       1.0.x           2.0.x           3.0.x
          │               │               │
          └───────────────┼───────────────┘
                          │
                    Behavior Parity
```

CI 不应该只验证 build succeeds，还需要验证：

```text
capability parity
contract test parity
```

---

## 38. OpenSpec Change 拆分建议

不要做成一个巨型 Change。

建议：

```text
openspec/changes/

kimi-runtime-foundation
kimi-acp-lifecycle-hardening
kimi-typed-protocol
kimi-unified-events
kimi-server-websocket
kimi-process-hardening
kimi-observability
kimi-branch-parity
```

---

## 39. Change 01 — kimi-runtime-foundation

负责：

```text
Runtime SPI
Capability
Common Result
Common Request
Common Lifecycle
Exception hierarchy
```

依赖：none。

---

## 40. Change 02 — kimi-acp-lifecycle-hardening

负责：

```text
State machine
Unexpected exit
RPC cleanup
Turn cleanup
Timeout race
Cancel race
Close race
Callback isolation
```

这是优先级最高的实现 Change。

---

## 41. Change 03 — kimi-typed-protocol

负责：

```text
Session
Message
ContentBlock
PromptRequest
PromptResult
ToolCall
Usage
Model
Mode
Provider
```

---

## 42. Change 04 — kimi-unified-events

负责：

```text
Runtime events
Session events
Message events
Tool events
Turn events
Error events
```

---

## 43. Change 05 — kimi-server-websocket

负责：

```text
WS connect
events
heartbeat
disconnect
reconnect
event decoding
lifecycle
```

---

## 44. Change 06 — kimi-process-hardening

负责：

```text
bounded stdout
bounded stderr
working directory
environment
duration
PID
graceful shutdown
force shutdown
process tree
```

---

## 45. Change 07 — kimi-observability

负责：

```text
metrics
runtime diagnostics
event hooks
tracing hooks
```

---

## 46. Change 08 — kimi-branch-parity

负责：

```text
Cross-branch policy
Contract Test
CI matrix
Capability parity
API parity
```

---

## 47. OpenSpec Change 标准内容

每个 Change 至少必须包含：

```text
proposal.md
design.md
tasks.md
specs/*
```

Proposal 回答：

```text
为什么做？
解决什么问题？
哪些能力变化？
```

Design 回答：

```text
怎么设计？
边界是什么？
状态机是什么？
协议是什么？
```

Specs 回答：

```text
系统 MUST 做什么？
系统 MUST NOT 做什么？
```

Tasks 回答：

```text
严格 TDD 下如何一步步完成？
```

---

## 48. 建议实施顺序

最终顺序建议：

```text
Phase 0
Documentation / OpenSpec
        │
        ▼
Phase 1
ACP Reliability
        │
        ▼
Phase 2
Typed Protocol
        │
        ▼
Phase 3
Runtime SPI
        │
        ▼
Phase 4
Unified Events / Streaming
        │
        ▼
Phase 5
Server WebSocket
        │
        ▼
Phase 6
Process Hardening
        │
        ▼
Phase 7
Observability
        │
        ▼
Phase 8
Branch Parity / Production Gate
```

不能反过来先做大量 facade API，然后再修底层生命周期。

---

## 49. 与 Agent Buddy 的关系

未来 Agent Buddy 不应该直接写：

```text
KimiClient
ClaudeClient
OpenCodeClient
HermesClient
```

然后大量：

```text
if runtime == kimi
```

而应该逐步形成整个 easy-4-java SDK 家族共用的概念：

```text
AgentRuntime
│
├── capabilities()
├── health()
├── prompt()
├── stream()
├── cancel()
├── sessions()
└── close()
```

不同 SDK 实现：

```text
KimiRuntimeAdapter
ClaudeRuntimeAdapter
OpenCodeRuntimeAdapter
HermesRuntimeAdapter
CodexRuntimeAdapter
```

因此本次 `kimi-java-sdk` 的 Runtime SPI 设计应避免做成 Kimi 私有的死胡同。

---

## 50. SDK 家族长期抽象

从目前几个项目已经可以识别：

```text
                    Agent Runtime Java SDK
                              │
            ┌─────────────────┼─────────────────┐
            │                 │                 │
         Process           Session           Service
          Driver            Driver            Driver
            │                 │                 │
           CLI          ACP / RPC / stdio    HTTP / WS
```

Kimi 当前正好完整覆盖：

```text
Process Driver
     │
     └── KimiCliExecutor

Session Driver
     │
     └── KimiAcpClient

Service Driver
     │
     └── KimiServerClient
```

这意味着：

> `kimi-java-sdk` 可以成为后续 easy-4-java Agent Runtime SDK 统一抽象的重要验证项目。

---

## 51. 不在当前阶段做的内容

第一阶段明确不做：

```text
Spring Boot 强绑定
Micrometer 强绑定
特定 Agent Buddy 业务逻辑
自动选择具体模型
业务级 Workflow Engine
Agent PaaS SDK
跨 Runtime 调度
远程 Agent Registry
```

这些属于上层系统，而不是 `kimi-java-sdk` 核心职责。

---

## 52. 最终验收标准

### Architecture

- CLI / ACP / Server 有明确 Runtime 边界；
- Runtime SPI 完成；
- Capability API 完成；
- Raw 与 Typed API 分层明确。

### ACP

- child crash 不产生悬挂 Future；
- pending RPC 无泄漏；
- prompt stream 无泄漏；
- timer 无泄漏；
- cancel / timeout / close race 有测试。

### Server

- REST 完整；
- WebSocket 进入正式 SDK；
- reconnect 可配置。

### Process

- stdout/stderr 有上限；
- 大输出不会导致无限 Heap 增长；
- timeout 可可靠终止进程；
- close 不残留子进程。

### Protocol

- 核心对象强类型；
- Event 强类型；
- Raw JsonNode 仍然可访问。

### Branches

```text
1.0.x
2.0.x
3.0.x
```

能力一致。

### Quality

- Strict TDD；
- contract tests；
- resource leak tests；
- process leak tests；
- CI 全绿；
- API parity 检查通过。

---

## 53. 最终目标

`kimi-java-sdk` 不应最终停留在：

> Java 调用 Kimi CLI 的一个 Wrapper。

目标应升级为：

> **面向 Java 应用和 Agent Platform 的 Kimi Code Runtime SDK。**

最终结构：

```text
                         kimi-java-sdk
                              │
                      Public Runtime API
                              │
                ┌─────────────┴─────────────┐
                │                           │
          Typed Runtime                 Raw API
                │
         Capability Layer
                │
          Unified Events
                │
        Lifecycle / Reliability
                │
       ┌────────┼─────────┐
       │        │         │
      CLI      ACP      Server
       │        │       HTTP + WS
       │        │         │
       └────────┼─────────┘
                │
            Kimi Code
                │
                ▼
        Agent Runtime Ecosystem
```

这也是后续：

```text
Claude Code Java SDK
Codex Java SDK
OpenCode Java SDK
Hermes Java SDK
OpenClaw Java SDK
Kimi Java SDK
```

逐步走向统一 Agent Runtime SDK 体系的重要基础。
