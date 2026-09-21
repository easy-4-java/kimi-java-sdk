# kimi-typed-protocol 实施任务

## 1. 门禁与 schema 证据
- [ ] 1.1 [TYP-01..06] 评审本 Change 并固定已验证 wire fixtures；未确认字段保持 raw。
- [x] 1.2 执行 `openspec validate kimi-typed-protocol --strict`。
- [ ] 1.3 建立 CLI/ACP/Server fixture 清单，标注来源和字段稳定性。

## 2. RED
- [x] 2.1 [TYP-01] 为核心 model 写反序列化失败测试。
- [x] 2.2 [TYP-02,TYP-03] 写 future-field 与 unknown-block fixture 测试；验证当前模型不能安全表达。
- [x] 2.3 [TYP-04] 写 collection defensive-copy 测试。
- [ ] 2.4 [TYP-05] 写 typed/raw 一致性测试。
- [ ] 2.5 [TYP-06] 建立跨 Jackson fixture contract。

## 3. GREEN
- [x] 3.1 [TYP-01] 最小实现 Session/Prompt/Message/ContentBlock/Tool/Usage 等 model。
- [x] 3.2 [TYP-02,TYP-03] 实现 extension/raw payload 与 unknown block。
- [x] 3.3 [TYP-04] 实现 immutable builder/defensive copy。
- [x] 3.4 [TYP-05] 增加 typed adapter/overload，不删除 raw API。
- [ ] 3.5 [TYP-06] 分别实现 Jackson 2/3 adapter。

## 4. REFACTOR
- [ ] 4.1 去除 transport/domain 的重复解析逻辑，保持依赖单向。
- [ ] 4.2 审查 equals/hashCode/toString，确保 toString 不泄露 token/secret/完整敏感内容。

## 5. 三线验证
- [x] 5.1 feature/3.0.x fixtures + unit tests。
- [ ] 5.2 feature/2.0.x 同 fixtures。
- [ ] 5.3 feature/1.0.x 同 fixtures。
- [ ] 5.4 比较三线 serialized/parsed semantic snapshots，无未解释差异。
