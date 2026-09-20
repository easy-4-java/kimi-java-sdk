# kimi-runtime-foundation 实施任务

全部任务默认未批准、未完成。必须严格 TDD。

## 1. 规范门禁
- [ ] 1.1 [RTF-01..05] 评审 proposal/design/spec；交付：批准记录；验证：范围、非目标、三线规则明确。
- [ ] 1.2 [RTF-01..05] 执行 `openspec validate kimi-runtime-foundation --strict`；交付：版本、命令、退出码、完整日志。
- [ ] 1.3 [RTF-01..05] 用 CodeGraph 标记 KimiClient/KimiAcpClient/KimiServerClient 当前依赖与影响面；验证：无未解释循环依赖。

## 2. RED：Contract Tests
- [ ] 2.1 [RTF-01] 先新增 CapabilityContractTest，证明 unsupported capability 当前没有统一失败语义；验证：测试稳定失败。
- [ ] 2.2 [RTF-02,RTF-03] 新增 Lifecycle/Health ContractTest，覆盖 normal close、failed、closed-use；验证：实现前至少一项失败。
- [ ] 2.3 [RTF-04] 新增 ExceptionContractTest，覆盖新分类仍继承 KimiException；验证：实现前失败。
- [ ] 2.4 [RTF-05] 新增 no-fallback 测试，证明失败不会跨 Runtime 静默切换。

## 3. GREEN：最小实现
- [ ] 3.1 [RTF-01] 实现最小 Capability model 和查询 API；验证：2.1 通过。
- [ ] 3.2 [RTF-02,RTF-03] 实现生命周期/health 基础模型；验证：2.2 通过。
- [ ] 3.3 [RTF-04] 实现异常分类层级并保持 KimiException 兼容；验证：2.3 通过。
- [ ] 3.4 [RTF-05] 为 adapter 明确禁用 implicit fallback；验证：2.4 通过。

## 4. REFACTOR 与适配
- [ ] 4.1 [RTF-01..05] 为 CLI/ACP/Server 建立最小 Runtime adapter；交付：不删除旧 API。
- [ ] 4.2 [RTF-01..05] 保持 Java 8 核心 API 可表达；Java 17/21 增强仅做 optional adapter。

## 5. 三线验证
- [ ] 5.1 在 feature/3.0.x 运行完整单测和 Contract Tests。
- [ ] 5.2 移植 feature/2.0.x，仅允许兼容适配；运行相同 Contract Tests。
- [ ] 5.3 移植 feature/1.0.x，仅允许 Java 8/Jackson 兼容适配；运行相同 Contract Tests。
- [ ] 5.4 对三线做 public API/capability parity 检查并记录差异；非允许差异不得完成任务。
