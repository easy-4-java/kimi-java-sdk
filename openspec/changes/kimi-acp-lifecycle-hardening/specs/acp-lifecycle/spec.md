# ACP 生命周期契约

## Purpose

确保 Kimi ACP 在失败、取消、超时和关闭竞态下不会产生悬挂请求或资源泄漏。

## ADDED Requirements

### Requirement: ACP-01 Explicit connection lifecycle

ACP Client MUST 具有可观察的连接生命周期，并在 READY 之前拒绝需要已初始化连接的操作。

#### Scenario: Duplicate connect

- **WHEN** 已存在连接的 client 再次 connect
- **THEN** SDK 在启动新子进程前明确拒绝，不产生孤儿进程。

#### Scenario: Initialize failure

- **WHEN** 子进程已启动但 initialize 超时或失败
- **THEN** client 不进入 READY，并回收本次拥有的子进程和 pending initialize 请求。

### Requirement: ACP-02 Pending RPCs always terminate

每个已登记 RPC MUST 在成功、RPC error、timeout、close、transport failure 或 process exit 中的一个终态结束并移出 pending registry。

#### Scenario: Process exits with pending RPCs

- **WHEN** ACP 子进程在多个 RPC pending 时退出
- **THEN** 所有 pending future 在有限时间内以同一底层故障类别结束，registry 最终为空。

#### Scenario: Write fails after registration

- **WHEN** RPC 已分配 id 但写 stdin 失败
- **THEN** 该 RPC 不得永久保留在 pending registry。

### Requirement: ACP-03 One terminal outcome per turn

Prompt turn MUST 只有一个可观察终态，response、timeout、cancel、close 竞态不得重复完成或覆盖首次终态。

#### Scenario: Response races timeout

- **WHEN** response 与 timeout 几乎同时发生
- **THEN** future 只完成一次，stream registry 和 timeout task 最终都被清理。

#### Scenario: Cancel races response

- **WHEN** cancel 与正常 response 并发
- **THEN** 调用者只观察一个终态，后续事件不会再次完成同一 turn。

### Requirement: ACP-04 Session prompt isolation

SDK MUST 明确同一 session 的 active turn 并发策略，并不得因 registry 覆盖造成不同 turn 串流。

#### Scenario: Second prompt on active session

- **WHEN** 同一 session 已有 active prompt 又提交第二个 prompt
- **THEN** SDK 在发送第二个请求前明确拒绝或使用可证明安全的唯一关联键，不覆盖首个 stream。

#### Scenario: Different sessions

- **WHEN** 两个不同 session 并发 prompt
- **THEN** delta 和 completion 按 session/request 关联，不互相串流。

### Requirement: ACP-05 Reader failure propagation

ACP transport reader MUST 将不可恢复 EOF、解析错误、帧上限错误和 IO failure 传播给受影响工作，不能静默退出。

#### Scenario: Malformed frame

- **WHEN** agent 输出无法解析的协议帧
- **THEN** SDK 返回 protocol/transport failure，并按连接可恢复性处理 pending work。

### Requirement: ACP-06 Callback isolation

用户 streaming callback 抛出的异常 MUST NOT 终止 ACP reader thread 或破坏其他 session 的协议处理。

#### Scenario: Delta callback throws

- **WHEN** 某 session 的 onDelta 抛 RuntimeException
- **THEN** 该错误被明确关联到对应 turn，其他 session 和 transport reader 继续按契约工作。

### Requirement: ACP-07 Idempotent resource cleanup

close MUST 幂等地释放 client 拥有的 Process、stdin、reader、timer、pending RPC 和 prompt stream。

#### Scenario: Repeated close

- **WHEN** 多个线程重复调用 close
- **THEN** 最终资源只释放一次，不抛出资源已释放导致的次生异常，并且 client 不再接受新操作。
