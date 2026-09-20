# Typed Protocol 契约

## Purpose

让 Java 调用者通过稳定类型使用 Kimi 核心协议，同时保持对未来未知字段的兼容能力。

## ADDED Requirements

### Requirement: TYP-01 Typed core models

SDK SHALL 为已验证的 session、prompt、message、content block、tool call/result、usage、model、mode、provider、stop reason 和 error 提供强类型表示。

#### Scenario: Typed prompt response

- **WHEN** transport 返回符合已验证 schema 的 prompt 响应
- **THEN** 调用者无需手工读取 JsonNode 即可取得已知核心字段。

### Requirement: TYP-02 Preserve unknown data

SDK MUST 在协议允许演进的对象中保留未知字段或提供等价 raw payload 访问，不因未知字段导致整个响应失败。

#### Scenario: Future field appears

- **WHEN** fixture 含 SDK 尚未建模的嵌套字段
- **THEN** 已知字段仍正常解析，未知数据仍可通过扩展/raw 机制访问。

### Requirement: TYP-03 Unknown content block

Content block 类型族 MUST 对未知 discriminator 保持可表示性。

#### Scenario: New block type

- **WHEN** 上游返回未识别的 content block type
- **THEN** SDK 返回 unknown/raw block，而不是丢弃该 block 或把整个 message 判为解析失败。

### Requirement: TYP-04 Immutable observable state

新公开 domain model SHALL 在构造完成后保持可观察状态稳定，集合不得通过外部引用被无意修改。

#### Scenario: Source collection changes

- **WHEN** 调用者构造模型后修改原始 List/Map
- **THEN** 已构造模型内容不随之变化。

### Requirement: TYP-05 Raw API remains available

引入 Typed API MUST NOT 删除现有 raw JsonNode 能力，并且 typed/raw 对同一响应的底层事实保持一致。

#### Scenario: Advanced caller needs raw field

- **WHEN** 调用者需要尚未类型化的字段
- **THEN** 可以通过保留的 raw API 或 raw payload 取得，不需要 fork SDK。

### Requirement: TYP-06 Cross-Jackson semantic parity

三条分支 MUST 在 Jackson 2/3 差异下保持相同 JSON 可观察语义。

#### Scenario: Same fixture across branches

- **WHEN** 三条分支读取同一协议 fixture
- **THEN** 已知字段、unknown 数据和错误分类具有相同业务结果，仅允许实现 API 差异。
