# ic2-fabric 电网性能优化实施计划

> 状态：**P0-2 / P0-1(+K1) / P1-1 已实施 + 独立评审 + 评审问题修复完毕**（2026-08）
> 验证：`./gradlew :core:build` 通过；`:core:mcdebugTest` 全量通过（含 SteamTierChainTest/TransformerMatrixTest）
> 配套分析：`plans/energy-network-performance-analysis.md`（瓶颈证据、成本分解、fabric-api 源码级验证）
> 实施范围：仅 `core/src/main/kotlin/ic2_120/content/block/energy/EnergyNetwork.kt`（git diff 确认，未动其他文件）

## 评审结论（独立 subagent，不继承实施者结论）

总体：**有界行为回归，无持久能量不一致**；K1 确认为真实 bugfix；缓存失效机制经 MC 源码逐条验证。评审问题处理：

| # | 问题 | 严重性 | 处理 |
|---|---|---|---|
| 1 | chunk 重载后边界机器最多 20 tick 停摆（新 BE 创建不发 block update，isRemoved 对 null 条目无效） | 中 | **已修**：null 条目实时轻查 `getBlockEntity != null` 即重查（只 null 条目查世界，机器边界不增加查询） |
| 2 | 同 BE 的 storage 对象/能力变化最多 20 tick 用旧引用；僵尸对象则能量有界丢失 | 中（依赖机器实现） | **接受**：本 mod 机器 storage 为稳定对象（构造时创建），不重建；第三方方块按 Energy API 协议能力变化须发 block update，且有 20 tick verify 兜底；风险有界，记录不修 |
| 3 | `cableLoad`（Jade 显示）空载/纯外部注入 tick 不刷新，FilteredValue 无样本不衰减 → 永久残留旧值 | 低 | **已修**：每 20 tick（相位与端点 verify 一致，各网络错开）强制同步一次，窗口可归零；传输时仍即时同步 |
| 4 | `CachedEndpointLookup.lookupSide` 死字段 | 低 | **已删** |
| 5 | K1 快照每传输一次 O(#cables) map 拷贝 + key 字符串构建，部分抵消收益 | 低（性能） | **接受**：map 拷贝是 SnapshotParticipant 标准语义（`pullFromBufferedEnergyByPath` 原本就有），为正确性代价；key 构建比原路径重建便宜 |
| 6 | >512 consumer 时缓存整体 clear 抖动 | 低 | **接受**：极端场景（>512 消费者单网），记录不修 |

---

## 0. 已知问题清单（含"先记录，不改"项）

| # | 问题 | 位置 | 状态 |
|---|---|---|---|
| K1 | `cableTransferRemaining` 扣减未调 `updateSnapshots(tx)`，外部事务回滚时导线容量账本不回滚 | `EnergyNetwork.kt` `pullFromProvidersByPath` / `insertAndDeliver` / `extractFromCable`（对照 `pullFromBufferedEnergyByPath` 已调） | **已随 P0-1 修复**（三处均补 `updateSnapshots`） |
| K2 | 每 tick 对不变拓扑全量 `SIDED.find`（无缓存，`BlockApiLookupImpl` 每次重做世界查询） | `findConsumers`/`findProviders` `:535`/`:550` | **已修复（P0-1 端点缓存）** |
| K3 | `syncCableLoadToLocalStorage` 每 tick O(导线数) 次 `getBlockEntity`，空载也执行 | `:750` | **已修复（P0-2 anyTransfer）** |
| K4 | `buildProviderCandidates` 每 consumer 每 tick 重建候选 + 排序 2 次 | `:768` | **已修复（P1-1 候选缓存）** |

---

## 1. P0-2：`cableLoad` 只在有真实传输时同步（零风险，先行）

**目标**：空载电网 / 孤立导线网络免去每 tick 的 O(N) `getBlockEntity`。

**改动**（`EnergyNetwork.kt`，约 5 行）：
- `pushToConsumers` 维护 `var anyTransfer = false`；
- `pullFromBufferedEnergyByPath` / `pullFromProvidersByPath` 内部真实传输成功（insert>0）处置位；
- 末尾 `if (anyTransfer) syncCableLoadToLocalStorage(world, topology.cableRates)`；
- `consumers.isEmpty()` 提前返回路径保持现状（本来就空载，顺带跳过 sync 是期望行为）。

**风险**：无（`cableLoad` 仅 Jade 显示，无传输时本就全 0）。

## 2. P0-1：边界端点缓存（最大收益，含 K1 修复）

**目标**：消除每 tick 2×B 次 `SIDED.find` 全量重查；`insertAndDeliver`/`extractFromCable` 同步受益。

**设计**（推荐自定义端点缓存，不用 fabric `BlockApiCache`——后者只缓存 (pos→BE/provider)，`findConsumers` 仍需每 tick 遍历 boundaries + 聚合 entryCables，省不了一半；且 energy-api 3.0.0 自身无任何缓存类，已解包确认）：

```kotlin
// 新增缓存条目：SIDED.find 结果 + BE 引用（isRemoved 兜底）
private class CachedEndpointLookup(
    val cablePosLong: Long,
    val neighborPosLong: Long,
    val lookupSide: Direction,
    var blockEntity: BlockEntity?,   // null = 无 BE 邻居
    var storage: EnergyStorage?      // SIDED.find(world, neighborPos, lookupSide) 结果
)
```

- 缓存字段挂 `EnergyNetwork`（与 `topologyCache` 同生命周期）：构建 = 遍历 `topology.boundaries` 逐个 `SIDED.find`；**拓扑失效（`invalidatePathCaches` / `buildTopology`）时清空重建**。
- 取用时的兜底校验（复用 `AdjacentEnergyTransferComponent` 成熟模式）：
  1. `blockEntity?.isRemoved == true` → 该条目重查；
  2. 20 tick 强制全量重查 + 随机相位（`NEIGHBOR_VERIFY_INTERVAL` 同款），兜住"机器 BE 被替换为同为 Energy API 方块、导线侧连接属性不变收不到 block update"的场景；
  3. 相位随机分散，避免全服同 tick 重查尖峰（`AdjacentEnergyTransferComponent.verifyPhase` 同款）。
- `findConsumers`/`findProviders` 改为遍历缓存条目聚合 `Endpoint`（O(B) 纯内存操作，无世界查询），缓存命中时 `supportsInsertion/Extraction` 直接取 `storage` 的字段（若能力变化按 Energy API 协议会发 block update，导线侧 `getStateForNeighborUpdate` → `invalidateConnectionCachesAt` 兜底）。

**K1 修复（随本步）**：`pullFromProvidersByPath` / `insertAndDeliver` / `extractFromCable` 在扣减 `cableTransferRemaining` 前调用 `updateSnapshots(tx)`（`EnergyNetwork` 已是 `SnapshotParticipant<NetworkSnapshot>`，`createSnapshot` 已含容量账本拷贝；对齐 `pullFromBufferedEnergyByPath` 的写法）。

**风险**：中。涉及全部能量流动路径，需 mcdebug 覆盖：传输正确性、变压器升降压、过压爆炸、漏电伤害、回滚场景。

## 3. P1-1：provider 候选路径缓存

**目标**：去掉每 tick 每 consumer 的候选构建 + O(k log k) 排序。

**设计**：
- 候选的 `path` / `pathLossMilliEu` / `providerPosLong` 只依赖拓扑，缓存为 `(consumerEntriesKey, topologyVersion) → List<ProviderPath>`；
- 每 tick 只把当前 providers 的 `storage` 绑定到缓存的 `providerPosLong` 上（storage 引用不缓存）；
- 拓扑失效时清空；缓存上限沿用现有 `trimPathCachesIfNeeded`（512）策略。

**风险**：低中。注意 provider 机器替换时缓存条目仍指向旧 posLong，靠 P0-1 的兜底重查联动。

## 4. P2（可选，不做不影响主要收益）

- 多源 Dijkstra 合并：拓扑失效后从所有 provider entryCables 一次多源，consumer 直接查 dist（无向图对称）。
- ~~`tickAll` 的 `values.toSet()` → 直接迭代~~ **不可行**：`tickIfNeeded` 内的烧毁/超压会 `breakBlock` → `invalidateAt` → 修改 `worldPosToNetwork`，迭代中改 map 会抛 ConcurrentModificationException；`toSet()` 拷贝是防并发修改的必要保护，保持现状。
- 漏电检测 `hasLeakingCableNearby` 的 (2r+1)³ set 查询 → 按区块分组粗筛。
- `insertAndDeliver`/`extractFromCable` 的 `ensureCapacityTracking` 与 `pushToConsumers` 共享（现状正确，不动）。

---

## 5. 实施顺序与验证

1. P0-2（零风险）→ 构建 + 现有 mcdebug 回归；✅
2. P0-1 + K1（最大收益）→ 构建 + 新增 mcdebug 测试矩阵（传输/变压器/过压/回滚）；✅（构建 + 现有 mcdebug 全量通过）
3. P1-1 → 构建 + 回归；✅
4. P2 按需。

**验证命令**：`./gradlew build`（服务端+客户端编译）；mcdebug 测试参考 `docs/guides/mcdebug-test-guide.md`（be.tick 驱动、slot 速查）。
**生产部署**：forge1 走 `deploy-waiting/forge1/` + `deploy-forge1.sh`；**是否重启 forge1 需用户明确要求**（AGENTS.md 部署规则）。

## 6. 触发条件（什么时候值得做）

- 生产服 TPS 正常 → 只做 P0-2（零风险预防），P0-1 留待下次动电网；
- 玩家基地规模电网（数百导线/数十机器）出现卡顿 → 按 1→4 顺序全做。
