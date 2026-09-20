# Branch Parity 契约

## Purpose

保证 Java 8、17、21 三条维护线作为兼容版本拥有相同核心 SDK 能力和可观察行为。

## ADDED Requirements

### Requirement: BRP-01 Core capability parity

三条维护线 MUST 提供相同核心 Runtime capabilities，除非规范明确标记为 Java-version-specific optional adapter。

#### Scenario: New core capability

- **WHEN** 一个已批准 Change 在 feature/3.0.x 增加核心 capability
- **THEN** feature/2.0.x 与 feature/1.0.x 必须实现相同业务 capability 后，该 Change 才能被标记为三线完成。

### Requirement: BRP-02 Only approved compatibility differences

分支间差异 MUST 限于已记录的 Java/Jackson/Maven/JUnit/平台 API 或 optional adapter 兼容实现，不得改变协议、生命周期、错误和安全语义。

#### Scenario: Jackson 2 versus 3

- **WHEN** 3.0.x 使用 Jackson 3、1.x/2.x 使用 Jackson 2
- **THEN** 相同 fixture 的 typed/raw 业务结果保持一致，仅内部 mapper/import 实现不同。

### Requirement: BRP-03 Shared contract verification

每个核心 capability MUST 有跨三线等价的 Contract Tests 或可证明等价的 fixture 验证。

#### Scenario: Cancel semantics

- **WHEN** CancelContractTest 在三条分支运行
- **THEN** 三线观察到相同 terminal category、清理和事件语义。

### Requirement: BRP-04 Public API parity

SDK MUST 生成核心 Public API parity 证据，并对所有差异使用批准的 allowlist。

#### Scenario: Unapproved missing method

- **WHEN** API report 发现 3.0.x 的核心方法在 2.0.x 缺失且不在 allowlist
- **THEN** parity gate 失败，不能发布为能力一致版本。

### Requirement: BRP-05 CI runtime truthfulness

每条分支的 CI MUST 使用声明的真实 JDK baseline，并且 job 名称、文档和实际配置一致。

#### Scenario: Mismatched JDK label

- **WHEN** CI step 名称声明 JDK 21 但配置 java-version=8
- **THEN** consistency check 失败，必须修正文案或配置后才能通过门禁。

### Requirement: BRP-06 Evidence-based completion

三线完成状态 MUST 由对应 commit 上的实际构建/测试/安全/资源证据支持，NOT_RUN/SKIPPED 不得计作 PASS。

#### Scenario: Platform test unavailable

- **WHEN** Windows process-tree test 未实际运行
- **THEN** 报告标记 NOT_RUN/coverage gap，而不是把该平台记为通过。

### Requirement: BRP-07 Synchronized rollback

破坏核心 capability 的回滚 SHALL 同步评估三条线，并明确记录任何临时不一致。

#### Scenario: Regression rollback

- **WHEN** 3.0.x 因回归 revert 一个已同步功能
- **THEN** 2.0.x/1.0.x 同功能状态被评估并同步 revert，或在 release gate 中标记为阻塞差异。
