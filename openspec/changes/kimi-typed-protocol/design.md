# Kimi Typed Protocol 技术设计

## Context

Java SDK 需要在“类型安全”与“协议可扩展”之间平衡。完全暴露 JsonNode 会把解析责任推给用户；完全封闭 POJO 会在上游新增字段时丢信息。

## Decisions

### D1. Domain 与 Transport 分层

Domain：KimiSession、KimiPromptRequest/Result、KimiMessage、ContentBlock、ToolCall、Usage 等。
Transport：ACP JSON-RPC envelope、Server response envelope、CLI stream-json frame。

Transport adapter 负责把 wire shape 转为 domain，不让 JSON-RPC id、HTTP request_id 等污染通用 domain；必要诊断通过 raw metadata 保留。

### D2. ContentBlock 为可扩展类型族

至少建模 text、tool-call、tool-result、error/unknown。Java 8 不使用 sealed hierarchy；采用 interface/abstract base + type discriminator。未知 block MUST 保留 type 与 raw payload。

### D3. Unknown Fields

Jackson 2/3 都必须允许未知字段。对于需要 round-trip 的对象使用 extension map/RawPayload；未知字段不能覆盖已知强类型字段。

### D4. Null / Missing

明确区分“字段缺失”和“显式 null”仅在上游语义需要时保留；普通读取模型允许 null，但写请求时 builder 应避免意外发送无意义 null。删除语义必须由具体 endpoint 规格确认。

### D5. Immutable Public Models

新 domain model 首选构造/Builder 后不可变。Java 8 可用 final fields + builder；不要求 record。集合在构造时 defensive copy。

### D6. Raw Escape Hatch

每个 typed response 不必复制整个 JsonNode API，但必须存在可访问原始 envelope/payload 的受控方式，或者保留并行 raw 方法。Typed API 失败解析未知新字段时不得导致可恢复响应完全不可用。

### D7. Error Mapping

协议 error 建模为 `KimiError`，但 transport failure 与 domain error 不混为一类。HTTP/RPC 错误码原值保留。

## Validation

- fixture-driven JSON read/write；
- unknown field round-trip；
- Jackson 2.18 / 2.22 / 3.x 对应分支；
- Unicode、null、arrays、nested objects；
- raw/typed 同响应一致性。

## Migration / Rollback

旧 JsonNode API 保留；新 typed API 逐步增加。若某字段上游不稳定，先作为 raw/unknown，不猜测类型。回滚只撤销新 model/adapter，不删除原始 API。
