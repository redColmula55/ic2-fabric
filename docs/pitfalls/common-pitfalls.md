# 踩坑记录

用于记录项目开发过程中确认过的高频坑点，后续新增条目按序号追加。

## 1. `BlockWithEntity` 未显式返回 `MODEL` 导致方块透明/不可见

- 现象：
  - 方块能放置、状态也在变化，但世界里看起来透明或完全不可见。
  - 资源日志不一定有 `Missing model/texture` 报错，容易误判成贴图路径问题。
- 根因：
  - 方块继承 `BlockWithEntity`，但未覆写 `getRenderType`。
  - 在这种情况下可能走到不可见渲染路径，而不是 JSON 模型渲染。
- 修复方式：
  - 在方块类中显式添加：

```kotlin
override fun getRenderType(state: BlockState): BlockRenderType = BlockRenderType.MODEL
```

- 适用范围：
  - 所有继承 `BlockWithEntity` 且依赖 blockstate/model JSON 渲染的方块（作物、机器、容器等）。
- 本项目案例：
  - `CropBlock` 种植 `nether_wart` 后“看不见”。
  - 最终修复点：`src/main/kotlin/ic2_120/content/block/CropBlock.kt`。

## 2. 边界 chunk 上的自动化邻居查询可能形成保活链

- 现象：
  - 两台机器分别放在相邻 chunk 边界两侧，并都安装抽入升级。
  - 两边先被外部加载源“点火”（例如玩家、forceload、区块加载器）后，只解除其中一侧的强制加载，另一侧仍可能让两边机器继续 tick。
  - 实测中，解除单侧 forceload 后，左右两侧机器的抽入逻辑仍持续产生跨 chunk lookup；两边都解除加载源并等待卸载后，计数停止。
- 根因：
  - 加载互锁的直接原因是：自动化逻辑会在 tick 中跨 chunk 调用邻居方块/能力，并且调用前没有统一的“是否允许跨区块访问”的保护。
  - 当 A chunk 的机器 tick 时，它会查询 B chunk 边界机器；B chunk 的机器 tick 时，又反向查询 A chunk。两边都被点火加载过之后，这种跨区块邻居访问可能互相维持对方处于 loaded/ticking 状态。
  - BE tick / 网络 tick 中主动访问邻居能力，例如：

```kotlin
ItemStorage.SIDED.find(world, pos.offset(dir), dir.opposite)
FluidStorage.SIDED.find(world, pos.offset(dir), dir.opposite)
EnergyStorage.SIDED.find(world, neighborPos, side)
world.getBlockEntity(neighborPos)
world.getBlockState(neighborPos)
```

  - 这类访问若跨 chunk 且没有保护，可能让边界另一侧保持 loaded/ticking，形成“已点火后单侧自愈”的保活链。
  - 抽入升级只是实测样本；同类风险也存在于物品弹出、流体抽入/弹出、电网、管道、动能网络、贴脸能量传输、热/蒸汽邻接传输等自动系统中。
- 重要边界：
  - 没有外部加载源时，两台机器不会凭空开始 tick。
  - 仅加 `isChunkLoaded` 不一定能完全阻断已点火后的单侧自愈，因为实测中被解除 forceload 的一侧在 lookup 前已经是 loaded/ticking。
  - 若未来要修，应抽出统一 helper，明确“自动化 tick 是否允许跨 chunk 访问”，而不是只在单个升级组件里零散补判断。
- 当前状态：
  - 本条仅记录已验证洞察与风险面，当前不修。

## 3. Connector 下 Fabric `depends` 不决定初始化顺序——配方加载禁止在 onInitialize 里解析物品

- 现象：
  - 分子重组仪拒收锡锭（输入槽 `canInsert=false`，物品弹回），而配置/物品均正常；引用其他 mod 物品的配方整体静默失效。
- 根因：
  - README 声明主动支持 Sinytra Connector（信雅互联），但 Connector 下 Fabric `depends` 不参与初始化排序。
  - 源码证据链（Connector 1.0.0-beta.49，与生产部署一致）：
    1. `ConnectorModMetadataParser.createForgeMetadata()` 生成的 Forge 元数据**没有 dependencies 段**，Fabric `depends` 从未翻译成 Forge 依赖边；
    2. Forge `ModSorter` 用 Guava 有向图做拓扑排序，无边可排 → 保持 jar 发现序（NFS readdir 序，本质任意）；
    3. `ConnectorEarlyLoader.init()` 把 `LoadingModList.get().getMods()` 原样传入 `FabricLoaderImpl.addFmlMods()`；fork 版 loader 按该顺序追加 `mods`，不跑上游的 `ModResolver`/`ModPrioSorter`（上游有依赖优先排序，此路径完全绕过）；
    4. `EntrypointStorage` 按插入序 invoke `main` 入口。
  - 实测生产日志：`ic2_120_advanced_solar_addon` 17:19:37.9 初始化完成，core `ic2_120:tin_ingot` 17:19:40.3 才注册（晚 2.4 秒）。
  - 结果：在 `onInitialize` 里用 `Registries.ITEM.get(id)` 解析物品的配方加载，解析到 `Items.AIR` 后被 AIR 检查**静默丢弃**。
- 修复方式（本项目约定）：
  - 配方/配置加载不得在 `onInitialize` 里做物品注册表解析。二选一：
    1. 加载延后到 `ServerLifecycleEvents.SERVER_STARTING`（早于世界加载与玩家进入）或 `SERVER_STARTED`；
    2. 存字符串 ID，首次查询时惰性解析并带 AIR 守卫。
  - 丢弃配方必须 WARN 留痕，禁止静默。
- 适用范围：
  - 所有「配置/默认值存物品 ID、加载时解析」的系统（配方表、白名单、模板等），以及任何依赖 mod 初始化顺序的逻辑。
- 决策记录：
  - 评估过给 Connector 打补丁实现 depends→Forge 依赖转换（~50-80 行，但需处理 fabricloader/minecraft/java 特殊键、id 归一化、hiddenMods、循环依赖、行为收紧导致的启动失败回归），结论**不值得**，模组侧自律（本条约定）。
- 本项目案例：
  - `MTRecipes`：锡锭→银锭等引用 core 物品的配方被静默丢弃。已修复：`IC2AdvancedSolarAddon` 改在 `SERVER_STARTING` 调 `MTRecipes.init()`，`MTRecipes.addRecipe` 丢弃时 WARN。
  - 已排查安全（同机机制但无此问题）：`UuCostIndex`（SERVER_STARTED 重建 + 未注册跳过日志）、`UuTemplateData`（字符串 ID + 惰性解析 + AIR 守卫）、各 BlockEntity 的 `by lazy` 物品字段、运行时 handler（扳手/割胶/掉落）、datagen provider（仅 datagen 环境）、JEI/Jade 插件（客户端运行时）。tlm-ic2-addon 与 chemical-addon 仓库无同类模式。
