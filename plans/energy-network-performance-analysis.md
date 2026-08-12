# ic2-fabric 电网性能分析（只读分析，未改任何代码）

> 日期：2026-08（基于当前 main 分支代码）
> 范围：`EnergyNetwork.kt` + `EnergyNetworkManager.kt` + 相关调用链
> 结论先行：电网每 tick 对**不变拓扑做全量重查**是主要浪费；`syncCableLoadToLocalStorage` 的 O(导线数) `getBlockEntity` 是第二大浪费；事务层稳态零分配，不值得优化。

---

## 1. 架构与数据流

- 导线 BE **无 ticker**（`BaseCableBlock.getTicker` 返回 null），电网由 `ServerTickEvents.END_SERVER_TICK` → `EnergyNetworkManager.tickAll(world)`（`Ic2_120.kt:196`）统一驱动，每世界每 tick 一次。
- 每网络每 tick：`tickIfNeeded` → `pushToConsumers`（`EnergyNetwork.kt:564`）+ 每 100 tick 的漏电/烧毁/超压检查。
- 能量流（`pushToConsumers`）：
  1. `findConsumers` / `findProviders`（`:535`/`:550`）：遍历全部边界，逐个 `EnergyStorage.SIDED.find(world, neighborPos, side)`。
  2. Phase 1：池能量分发（`pullFromBufferedEnergyByPath`，通常 `energy==0` 直接跳过）。
  3. Phase 2：providers → consumers 直送。每个 consumer 调 **2 次** `pullFromProvidersByPath`（首轮限额定 + 余量无限，`:598-613`）。
  4. `syncCableLoadToLocalStorage`（`:750`）：遍历全部导线，每根 `world.getBlockEntity(pos)`，只为更新 Jade 显示的 `cableLoad`。
- 外部注/抽（`insertAndDeliver` `:117` / `extractFromCable` `:178`）：由其它 mod 经 Energy API 直接向导线 BE 的 `energyStorage` 注入/抽取时触发（变压器等本 mod 机器不走此路，走 push/pull）。每次调用也全量 `findConsumers`/`findProviders` + Dijkstra。

---

## 2. 每 tick 成本分解（单网络）

设 N=导线数，B=边界数，C=消费者数，P=供电者数，k≈C×P×entry 数（候选路径数）。

| 步骤 | 位置 | 成本 | 性质 |
|---|---|---|---|
| `findConsumers` + `findProviders` | `:535` `:550` | 2×B 次 `SIDED.find` + `supportsInsertion/Extraction` | **每 tick 全量重查，无缓存** |
| `totalProviderEnergy` 计算 | `:594` | P 次 `simulateExtraction`（各 1 事务） | 每 tick |
| `pullFromProvidersByPath` ×2/consumer | `:677` | 每 consumer：2 次 `buildProviderCandidates`（构建+排序 O(k log k)）+ 每候选 1 次 `simulateInsertion` + 1 次真实事务 | **每 tick 重建候选并排序 2 次** |
| `ensureCapacityTracking` | `:103` | O(N) 重建 `cableTransferRemaining` map | 每 tick 1 次/网络（惰性，已优化） |
| `syncCableLoadToLocalStorage` | `:750` | O(N) 次 `world.getBlockEntity` | **每 tick 全量，即使无任何传输** |
| 每 100 tick：漏电/烧毁/超压 | `:257` `:336` `:401` | 漏电对每个实体 (2r+1)³ 次 set 查询；超压 O(B) 次 `getBlockEntity` | 低频，大型电网仍可观 |

放大因子（一次典型的大型电网：N=500 导线，B=50，C=30，P=20）：
- 每 tick ≈ 100 次 `SIDED.find`（每次内部 = chunk 4 槽扫描 + `getBlockEntity` HashMap 查找 + provider 身份哈希查找 + lambda）≈ 每秒 2000 次；
- 每 tick ≈ 30 consumer × 2 轮 × (k=30 候选构建+排序 + 每候选 2 事务) ≈ 每秒数千次候选构建 + 数千次事务；
- 每 tick 500 次 `getBlockEntity` ≈ 每秒 1 万次（仅为了 Jade 的 `cableLoad` 显示）。

---

## 3. 关键证据（fabric-api 1.20.1 源码级）

- **`EnergyStorage.SIDED.find` 无缓存**：`BlockApiLookupImpl.find`（fabric-api-lookup-api-v1 `impl/lookup/block/BlockApiLookupImpl.java:65-103`）每次调用都重做 `getBlockState` + `getBlockEntity` + `ApiProviderHashMap.get`（身份哈希）。本版本**没有** `CachedBlockEntityApiLookup`；唯一缓存设施是 `BlockApiCache`（按 (world,pos) 缓存 BE 与 provider，`ServerBlockEntityEvents.BLOCK_ENTITY_LOAD/UNLOAD` 自动失效），电网未使用。
- **`Transaction.openOuter()` 稳态零分配**：`TransactionManagerImpl` 按线程 depth 复用 `TransactionImpl`（`fabric-transfer-api-v1 impl/transfer/transaction/TransactionManagerImpl.java:50-62`），open/close 是 int 自增+数组取+枚举写；每笔实际写入才产生 1 个 Long 装箱快照（`SnapshotParticipant.updateSnapshots`）。→ **不值得为省事务改结构**。
- **`World.getBlockEntity` 单次便宜、量大成瓶颈**：loaded chunk 上 = 4 槽 chunk 线性缓存扫描（`ServerChunkManager.java:147-155`）+ `WorldChunk.blockEntities`（HashMap）一次 get。无逐位置记忆化。

---

## 4. 优化建议（按收益/风险排序，均未实施）

### P0-1 缓存边界端点，消除每 tick 全量 `SIDED.find`（收益最大）

`findConsumers`/`findProviders`（`:535`/`:550`）对**不变拓扑每 tick 全量重查**。方案（二选一，推荐 a）：

- **(a) 端点缓存进拓扑**：把 consumers/providers 结果（含 `Endpoint.storage`）挂到 `TopologyCache`，拓扑失效（`invalidatePathCaches`，即连接/导线变化）时重建。**必须处理机器 BE 被替换的失效**：缓存里同时存 `BlockEntity` 引用，`isRemoved()` 时按位置重查——这正是 `AdjacentEnergyTransferComponent` 已用的模式（`content/AdjacentEnergyTransferComponent.kt`，20 tick 强制重查 + isRemoved 兜底 + 随机相位分散）。注意：机器替换为同为 Energy API 的方块时导线侧 `getStateForNeighborUpdate` 不触发（连接属性不变），仅靠 isRemoved/定期重查兜底。
- **(b) 用 `BlockApiCache`**：fabric-api 官方设施，自动按 BE load/unload 失效。但每端点要持有 cache 对象，且 `findConsumers` 还需要 `supportsInsertion/Extraction` 结果，仍需每 tick 遍历边界列表——不如 (a) 一步到位缓存整个端点集合。

收益：消除每 tick 2×B 次完整世界查询；`insertAndDeliver`/`extractFromCable` 同步受益（它们也全量重扫）。

### P0-2 `syncCableLoadToLocalStorage` 只在有传输时执行（收益大、零风险）

`:750` 每 tick 对每根导线 `getBlockEntity`，即使电网空载。`cableLoad` 仅用于 Jade 显示（`CableBlockEntity.cableLoad: Long by FilteredValue(20)`）。

- 在 `pushToConsumers` 里维护 `var anyTransfer = false`，任何真实传输（insert/extract 成功）置位；末尾 `if (anyTransfer) syncCableLoadToLocalStorage(...)`。
- 无消费者/无传输的电网（含孤立导线网络）直接跳过 O(N) 的 `getBlockEntity`。
- 可选进阶：网络维护 `cableEntities: Map<Long, CableBlockEntity>`（BE 在 `setWorld`/`markRemoved` 时登记/注销），彻底去掉 `getBlockEntity`。

### P1-1 缓存 provider 候选路径，去掉每 tick 重建+排序

`buildProviderCandidates`（`:768`）每 consumer 每 tick 构建 k 个候选并排序 2 次。候选的 `path`/`pathLossMilliEu`/`providerPos` 只依赖拓扑（`TopologyCache`），与 storage 引用无关，可以缓存（key = consumer entries + 拓扑版本）；每 tick 只把当前 providers 的 storage 绑定到缓存的 providerPos 上。注意失效：拓扑失效时清空；provider 机器被替换时靠 isRemoved 重查（与 P0-1 同一兜底）。
收益：消除每 tick 的 O(k log k) 排序与路径重建（`buildPath` 也要重走 prev 链）。

### P1-2 快速路径：无消费者时整段跳过

`:581` 已处理 `consumers.isEmpty()` 提前返回，但 `syncCableLoadToLocalStorage` 仍执行（P0-2 已覆盖）。可再加：`providers.isEmpty() && consumers.isEmpty()` 时连 `ensureCapacityTracking` 都跳过（孤立导线网络每 tick 只有 6 个空气边界的 `SIDED.find` 成本，直接归零）。

### P2-1 多源 Dijkstra 合并

`buildProviderCandidates`/`buildBufferedCandidates` 对每个 consumer 的每个 entryCable 单独跑 Dijkstra（有 `dijkstraCacheByEntries` 缓存，拓扑失效后才重建）。可改为：拓扑失效后**从所有 provider entryCables 做一次多源 Dijkstra**，每个 consumer 直接查 `dist[entry]` 并 `buildPath` 回溯（无向图对称，路径等价）。减少拓扑重建时的重复计算与缓存内存（当前 512 条上限 + 字符串 key 排序）。

### P2-2 `tickAll` 微优化

`EnergyNetworkManager.tickAll`（`EnergyNetworkManager.kt:41`）每 tick `values.toSet()` 拷贝网络集合；可改为直接迭代 `values`（网络集合在 tick 过程中不变）。收益小但零风险。

### P2-3 漏电检测的实体邻近查询

`hasLeakingCableNearby`（`:308`）对每个实体做 (2r+1)³ 次 set 查询（r=outputLevel，100 tick 一次）。可先把 `leakingCables` 转成紧凑网格/按区块分组，或先查实体所在区块是否有漏电导线再细查。低频路径，优先级低。

---

## 5. 顺带发现：正确性/一致性问题

1. **`cableTransferRemaining` 扣减不参与事务快照**：`pullFromProvidersByPath`（`:711` 附近）、`insertAndDeliver`、`extractFromCable` 在 `tx.commit()` 前直接改 `cableTransferRemaining`，**没有调用 `updateSnapshots(tx)`**；而 `pullFromBufferedEnergyByPath`（`:646`）调用了。若外层事务回滚（例如消费方机器事务被取消），能量恢复但导线容量账本不回滚 → 同 tick 后续传输可用容量偏小。`EnergyNetwork` 是 `SnapshotParticipant<NetworkSnapshot>`，理应把容量账本纳入快照。
   **状态：已确认，先记录不改（2026-08）；计划随端点缓存优化（P0-1）一起修，见 `plans/energy-network-optimization-plan.md` 清单 K1。**
2. **`pullFromProvidersByPath` 的 `while` 恒单轮**：循环末尾 `if (!progressed) break` 后紧跟 `break`（`:745-747`），注释为有意单轮；但结构易误导，且首轮 budget（`firstRoundBudget`）耗尽时余量轮（无上限）会再次全量遍历候选——语义等价于"每 consumer 每 tick 至多遍历候选集两次"，符合设计，无需改。

---

## 6. 建议实施顺序

正式实施计划见 **`plans/energy-network-optimization-plan.md`**（含端点缓存设计、失效策略、K1 修复方案、验证与部署规则）。摘要：

1. P0-2（零风险，立即见效：空载/孤立网络彻底免去 O(N) getBlockEntity）
2. P0-1（最大收益：端点缓存，参考 `AdjacentEnergyTransferComponent` 的失效兜底模式）+ K1 快照一致性修复
3. P1-1（候选缓存）→ P1-2（快速路径）→ P2 系列（可选）

> 备注：实施时注意本仓 AGENTS.md——改 Kotlin 后需 `./gradlew build` 验证服务端+客户端编译；电网改动建议用 mcdebug（`core/src/mcdebugTest/kotlin/`）补 tick 级测试矩阵（be.tick 驱动、slot 速查见 `docs/guides/mcdebug-test-guide.md`）。
