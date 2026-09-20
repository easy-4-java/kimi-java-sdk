# Kimi Runtime Foundation 技术设计

## Context

当前三条执行路径分别适合：
- CLI：一次性命令、管理操作、简单 prompt；
- ACP：长生命周期、session、stream、cancel；
- Server：远程/本地 HTTP service。

它们能力不对称，因此统一接口不能假装所有 Runtime 都支持所有操作。设计关键是“稳定公共语义 + 显式 Capability”，而不是最小公分母或把所有功能塞进一个巨型接口。

## Goals / Non-Goals

目标：
1. 上层可在不感知具体实现类的情况下查询能力、健康状态和生命周期；
2. 不支持能力时返回稳定、可诊断结果；
3. 保持三兼容线行为一致；
4. 允许未来映射到 AgentRuntime 家族抽象。

非目标：统一所有原始协议细节、自动 fallback、隐藏真实错误、跨 Runtime session 搬迁。

## Decisions

### D1. Runtime 与 Capability 分离

Runtime 暴露稳定基础操作；可选能力通过 `KimiCapability` 集合声明。调用一个未声明能力 MUST 在发起外部副作用前失败，并返回明确 unsupported-capability 错误。

Capability 建议至少覆盖：PROMPT、STREAMING、SESSION、SESSION_RESUME、SESSION_FORK、CANCEL、MODEL_SWITCH、MODE_SWITCH、TOOLS、SKILLS、MCP、PROVIDERS、ACP、SERVER、WEBSOCKET。

Capability 是事实声明，不是营销标签；每个 capability 必须由 Contract Test 证明。

### D2. Health 与 Lifecycle 分开

Lifecycle 描述对象状态，如 NEW、STARTING/CONNECTING、READY、CLOSING、CLOSED、FAILED；Health 描述当前可用性和诊断信息。CLOSED 不能报告 healthy，FAILED 必须保留稳定原因。

一次性 CLI Runtime 可以将每次调用视为离散 execution，而 facade 自身保持 READY/CLOSED；ACP/Server 则拥有长生命周期。

### D3. Prompt Result 不吞底层事实

统一结果对象保留成功/失败、stop reason、session/run 标识和可选 raw metadata。底层 exit code、RPC code、HTTP status 等不应被统一层删除；应通过 typed diagnostics/raw details 访问。

### D4. Exception Taxonomy

所有新增异常继续继承 `KimiException`，避免破坏现有 catch。分类至少包括：
- Configuration；
- Process；
- Timeout；
- Protocol；
- Rpc；
- Authentication；
- Server；
- Serialization；
- RuntimeClosed；
- UnsupportedCapability。

异常必须携带可诊断的稳定 category；敏感 token/prompt 默认不进入 message。

### D5. Compatibility

Java 8 不依赖 Flow、record、sealed class、ProcessHandle。公共核心必须可由 Java 8 表达；Java 17/21 可增加 adapter，但不得产生功能差异。

配置对象 immutable/builder 的整体迁移不强塞进本 Change；Foundation 只要求 Runtime 在启动/调用时读取稳定 snapshot，后续可由具体 Change 演进。

## Migration

1. 新增 foundation model/interface，不删除现有类。
2. 为现有 CLI/ACP/Server 增加 adapter 或 facade。
3. README 新示例优先 Runtime API，旧 API 标记为兼容层但不立即 deprecated。
4. 只有 Contract Tests 覆盖后才考虑后续 deprecation。

## Rollback

Foundation 为增量 API，可通过 revert 新增类型和 adapter 回滚，不重写现有 CLI/ACP/Server 实现历史。

## Validation

- RuntimeContractTest；
- CapabilityContractTest；
- LifecycleContractTest；
- ExceptionContractTest；
- 三条分支编译与相同行为 fixtures；
- CodeGraph impact review，确认 adapter 未形成不必要循环依赖。

## Open Questions

- Runtime 的 prompt 同步 API 是否直接返回结果或以 async 为核心、sync 为 adapter，由实现评审确定；
- 与未来通用 AgentRuntime 的 package 边界需保持可映射，但本 Change 不创建共享 artifact。
