# 工业升级附属（industrial-upgrade-addon）移植计划书

> 版本：v1.0 ｜ 待最终审阅
> 源版本：IndustrialUpgrade **1.4.0**（1.12.2，提交 `7d2cee224`，2.0 之前最后版本）
> 目标：ic2-fabric 子模块 `industrial-upgrade-addon`

---

## 0. 背景与定位

把工业升级 1.4.0 中 advanced-solar-addon **尚未覆盖**的 6 级主线太阳能发电机（Spectral → Neutron）移植到 ic2-fabric，作为独立附属 `industrial-upgrade-addon`。

- **代码**：复用 advanced-solar-addon 的 `SolarPanelBlockEntity` 基类体系；不移植 industrialupgrade 的任何 Java 行为代码。
- **材质**：只从 industrialupgrade 1.4.0 拷贝 PNG 贴图。
- **数值**：不沿用 1.4.0 原版数值，按 advanced-solar-addon 现有曲线（day 每级 ×8）从 Quantum 之后**改用 ×4**外推。
- **合成**：移植 1.4.0 的级联合成链，用 ic2-fabric datagen 重写。
- **不做**：Sun/Rain 变体系列、护甲、武器、反应堆燃料棒、MFSU、变压器、元素饰带板、模组联动板（Draconic/Botania/Avaritia/Thaumcraft）。

---

## 1. 依赖关系

新附属 `fabric.mod.json`：
```json
"depends": {
  "fabricloader": ">=0.18.0",
  "minecraft": "~1.20.1",
  "java": ">=17",
  "fabric-api": "*",
  "fabric-language-kotlin": "*",
  "ic2_120": "*",
  "ic2_120_advanced_solar_addon": "*"
}
```

**复用 advanced-solar-addon 的内容**（直接 import，不重复定义）：
- 基类：`SolarPanelBlockEntity`、`SolarPanelSync`、`SolarPanelScreenHandler`
- 机器：`MolecularTransformerBlock` / `MolecularTransformerBlockEntity`（用于产 photoniy、Sunnarium、SunnariumPart）
- 物品：`Sunnarium`、`SunnariumPart`、`SunnariumAlloy`、`PhotovoltaicIridiumPlate`（光伏铱板）、`IrradiantUranium`、`EnrichedSunnarium`、`EnrichedSunnariumAlloy`、`IrradiantGlassPane`、`IridiumIronPlate`、`ReinforcedIridiumIronPlate`、`IrradiantReinforcedPlate`、`IridiumIngot`、`QuantumCore`
- 面板：`QuantumSolarPanelBlock`（Spectral 的合成前置）
- 创造栏：`CreativeTab.IC2_SOLAR`

---

## 2. 数值表（day ×4 递推，Quantum 为基准）

| 等级 | 对应1.4.0 | dayPower | nightPower | maxStorage | tier |
|---|---|---|---|---|---|
| (基准) Quantum | QuantumSolar | 4096 | 2048 | 10,000,000 | 5 |
| **1** Spectral 光谱 | SpectralSolar | **16384** | **8192** | **100,000,000** | **6** |
| **2** Proton 质子 | ProtonSolar | **65536** | **32768** | **1,000,000,000** | **7** |
| **3** Singular 奇点 | SingularSolar | **262144** | **131072** | **10,000,000,000** | **8** |
| **4** Admin 光吸 | AdminSolar | **1048576** | **1048576** | **100,000,000,000** | **9** |
| **5** Photonic 光子 | PhotonicSolar | **4194304** | **4194304** | **1,000,000,000,000** | **10** |
| **6** Neutron 中子 | NeutronSolar | **16777216** | **16777216** | **10,000,000,000,000** | **11** |

递推规则：
- dayPower：每级 ×4（Quantum→Spectral 起 ×4）
- nightPower：Singular 及之前 = day/2；Admin 起 = day（全天满发，还原 1.4.0 特色）
- maxStorage：每级 ×10
- tier：每级 +1

---

## 3. 新增方块（7 个）

### 3.1 太阳能发电机（6 个）
位于 `content/block/`，每个一对类（Block + BlockEntity），仿 advanced-solar-addon 的 `AdvancedSolarPanel.kt` 写法：
- `SpectralSolarPanelBlock` / `SpectralSolarPanelBlockEntity`（day=16384, night=8192, storage=100M, tier=6）
- `ProtonSolarPanelBlock` / `ProtonSolarPanelBlockEntity`（65536, 32768, 1B, t7）
- `SingularSolarPanelBlock` / `SingularSolarPanelBlockEntity`（262144, 131072, 10B, t8）
- `AdminSolarPanelBlock` / `AdminSolarPanelBlockEntity`（1048576, 1048576, 100B, t9）
- `PhotonicSolarPanelBlock` / `PhotonicSolarPanelBlockEntity`（4194304, 4194304, 1T, t10）
- `NeutronSolarPanelBlock` / `NeutronSolarPanelBlockEntity`（16777216, 16777216, 10T, t11）

全部继承 advanced-solar-addon 的 `SolarPanelBlockEntity`，仅传不同的 day/night/storage/tier 参数。
注册：`@ModBlock(name="spectral_solar_panel", registerItem=true, tab=CreativeTab.IC2_SOLAR, group="solar_panel")` + `@ModBlockEntity`。
BlockEntity 的 `getBlockName()` 返回各自名称（GUI 标题用）。

### 3.2 中子制造机 Neutron Fabricator（1 个）
`NeutronFabricatorBlock` / `NeutronFabricatorBlockEntity`，位于 `content/block/`。
- **模板**：ic2-fabric core 的 `MatterGeneratorBlockEntity`（物质制造机），它产出 UU 物质流体；中子制造机把产出流体改为 **Neutron 流体**，逻辑相同（耗电充满 → 产出 1mB 流体，支持废料 amplifier 加速、升级插槽、红石控制）。
- tier：参照 1.4.0 配置 `Configs.Neutronfabricator`（默认值见下），容量与耗电取一个合理的高数值（如 tier=8，容量 100,000,000 EU，每产出 1mB 消耗一满槽能量）。
- GUI/ScreenHandler：新建 `NeutronFabricatorScreenHandler`，参照 `MatterGeneratorScreenHandler`。
- 流体槽：注册 `@RegisterFluidStorage`，仅接受 Neutron 流体；支持空单元装桶。
- 注册：`@ModBlock(name="neutron_fabricator", tab=CreativeTab.IC2_MACHINES)`。

> 说明：1.4.0 的 `TileEntityMassFabricator` 直接继承 IC2 的 `TileEntityElectricMachine`；ic2-fabric 端对应 `MachineBlockEntity` + `MatterGeneratorBlockEntity` 模式，按后者重写，不照搬 Forge 代码。

---

## 4. 新增流体（1 种）

**Neutron 中子流体**，位于 `content/fluid/NeutronFluid.kt`。
- 模板：ic2-fabric core 的 `ModFluids`（参照 UU_MATTER 的 still/flowing 方块注册）。
- 注册 still + flowing + 桶（`NEUTRON_BUCKET`）。
- 材质：复用 1.4.0 的 `blocks/uu_matter1_still.png` / `uu_matter1_flow.png`（1.4.0 中子流体本就借用了 uu_matter 贴图）。
- 用于：中子制造机产出 → 桶装 → 压缩机合成 neutronshard。

---

## 5. 新增物品（19 种）

全部位于 `content/item/`，用 `@ModItem` 注册，每个带 `@RecipeProvider`。

### 5.1 核心（6）
| 物品 | 1.4.0 配方 | 新附属配方（datagen 重写） |
|---|---|---|
| `spectral_core` 光谱核心 | CBC/CAC/CBC：1×QuantumCore + 4×photoniy + 4×solarsplitter | 同 |
| `proton_core` 质子核心 | +形：4×proton + 2×enriched_sunnarium_alloy4 + 1×spectral_core | 同 |
| `singular_core` 奇点核心 | ABA/DCD/ABA：4×enderquantumcomponent + 1×photoniy_ingot + **2×光伏铱板** + 2×enriched_sunnarium_alloy | 同（强化铱板→光伏铱板） |
| `quant_core2` 量子核心II | ABA：2×enriched_sunnarium_alloy3 + 1×singular_core | 同 |
| `quant_core1` 量子核心I | ABA：2×enriched_sunnarium_alloy2 + 1×quant_core2 | 同 |
| `neutron_core` 中子核心 | +形：4×neutron + 1×quant_core2 | 同 |

### 5.2 富集阳光合金递进（3）
| 物品 | 配方 |
|---|---|
| `enriched_sunnarium_alloy2` | ABA：2×enriched_sunnarium_alloy + 1×singular_core |
| `enriched_sunnarium_alloy3` | ABA：2×enriched_sunnarium_alloy2 + 1×photoniy_ingot |
| `enriched_sunnarium_alloy4` | ABA：2×enriched_sunnarium_alloy + 1×spectral_core |

> 注：1.4.0 命名 enriched_sunnarium_alloy4 用 enriched_sunnarium_alloy（基础版）做原料，是循环引用外观，实际是 ESA → ESA2 → ESA3，ESA4 独立支线。新附属按上表，物品名沿用 1.4.0 的 2/3/4 编号保持辨识度。

### 5.3 光子系（2）
| 物品 | 配方 |
|---|---|
| `photoniy` 光子 | 分子重组仪：iridium_ore（ic2 铱矿石）→ photoniy，耗能 25,000,000（复用 advanced-solar-addon 的 MTRecipes 注册） |
| `photoniy_ingot` 光子锭 | 压缩机：9× photoniy → 1× photoniy_ingot |

### 5.4 核材料（4）
| 物品 | 配方 |
|---|---|
| `proton_shard` 质子碎片 | 压缩机：1× ic2 钚（`ic2_120:nuclear#plutonium`）→ 1× proton_shard |
| `proton` 质子 | 压缩机：18× proton_shard → 1× proton |
| `neutron_shard` 中子碎片 | 压缩机：1× Neutron 流体桶 → 1× neutron_shard |
| `neutron` 中子 | 压缩机：9× neutron_shard → 1× neutron |

### 5.5 分光器系（4）
| 物品 | 配方 |
|---|---|
| `red_component` 红色组件 | AAA/BBB/AAA：5× ic2 强化玻璃 + 3× 红石 |
| `blue_component` 蓝色组件 | 同上，B=青金石 |
| `green_component` 绿色组件 | A  ：1× IrradiantGlassPane（光辉玻璃板，复用 ASA） |
| `solar_splitter` 分光器 | ABC/ABC/ABC：3× red_component + 3× green_component + 3× blue_component |

### 5.6 量子组件（1）
| 物品 | 配方 |
|---|---|
| `ender_quantum_component` 末影量子组件 | ABA/BCB/ABA：**4× 光伏铱板** + 4× 末影之眼 + 1× 下界之星 |

---

## 6. 面板合成（6 条十字配方）

全部形态：上 B / 左B A 右B / 下 B（即 4 个上一级面板 + 1 个核心）。

| 产物 | B（4个） | A（1个核心） |
|---|---|---|
| Spectral Solar Panel | QuantumSolarPanelBlock（ASA） | spectral_core |
| Proton Solar Panel | SpectralSolarPanelBlock | proton_core |
| Singular Solar Panel | ProtonSolarPanelBlock | singular_core |
| Admin Solar Panel | SingularSolarPanelBlock | quant_core2 |
| Photonic Solar Panel | AdminSolarPanelBlock | quant_core1 |
| Neutron Solar Panel | PhotonicSolarPanelBlock | neutron_core |

---

## 7. 强化铱板 → 光伏铱板 替换（关键决策）

经核对 ic2-fabric 国际化文件：
- `item.ic2_120.iridium` → 中文名 **"强化铱板"**（即 IC2 经典的 Iridium Reinforced Plate，物品 ID `ic2_120:iridium`）。
- advanced-solar-addon 已有 **光伏铱板** `ic2_120_advanced_solar_addon:photovoltaic_iridium_plate`（`PhotovoltaicIridiumPlate`）。

**规则**：新附属中所有 1.4.0 原本使用 IC2 强化铱板（`ic2_120:iridium`）的位置，全部改用光伏铱板（`PhotovoltaicIridiumPlate`）。

**实际影响**（1.4.0 配方中用到 `IC2Items("crafting","iridium")` 且落入新附属范围的位置）：
- `ender_quantum_component`：4× 光伏铱板 ✓
- `singular_core`：2× 光伏铱板 ✓

其余新材料配方不涉及强化铱板。`ingotIridium`（oreDict，指铱锭）非强化铱板，不改。

---

## 8. 材质移植

从 `develop/industrialupgrade`（1.4.0 checkout）拷贝到新附属 `src/main/resources/assets/ic2_120_industrial_upgrade/textures/`。

### 8.1 面板方块贴图（6 套，每套 top/side/bottom）
1.4.0 路径：`src/main/resources/assets/super_solar_panels/textures/blocks/`
- `spectral/` → spectral_solar_panel_{top,side,bottom}.png
- `proton/` → proton_solar_panel_{...}
- `singulary/` → singular_solar_panel_{...}（注意 1.4.0 目录名是 singulary）
- admin → admin_solar_panel_{...}（1.4.0 admin 级贴图，若无则用 diffractive/光吸对应）
- `photonic/` → photonic_solar_panel_{...}
- `neutron/` → neutron_solar_panel_{...}

> 拷贝时统一重命名为 `{等级}_solar_panel_{top,side,bottom}.png`，与 advanced-solar-addon 的命名规范一致。

### 8.2 中子制造机贴图
1.4.0 路径同上，`matter_generator` 相关贴图（中子制造机在 1.4.0 称 nutronfabricator，借用 matter_generator 外观）。

### 8.3 中子流体贴图
`blocks/uu_matter1_still.png`、`blocks/uu_matter1_flow.png`。

### 8.4 物品贴图（19 种）
1.4.0 路径：`textures/items/crafting/`（CraftingTypes 对应）。
- 核心：spectralcore、protoncore、singularcore、quantcore1、quantcore2、neutroncore
- 合金：enrichedsunnariumalloy2/3/4
- 光子：photoniy、photoniy_ingot
- 核材料：proton、protonshard、neutron、neutronshard
- 分光器：solarsplitter、redcomponent、bluecomponent、greencomponent
- 量子组件：enderquantumcomponent

> 实施时用脚本批量核对 1.4.0 实际文件名（CraftingTypes 枚举名小写）并拷贝。

---

## 9. 模块结构

```
ic2-fabric/industrial-upgrade-addon/
├── build.gradle                          # 仿 advanced-solar-addon
├── src/main/
│   ├── kotlin/ic2_120_industrial_upgrade/
│   │   ├── IC2IndustrialUpgrade.kt       # ModInitializer, ClassScanner 注册
│   │   ├── content/
│   │   │   ├── block/
│   │   │   │   ├── SpectralSolarPanel.kt
│   │   │   │   ├── ProtonSolarPanel.kt
│   │   │   │   ├── SingularSolarPanel.kt
│   │   │   │   ├── AdminSolarPanel.kt
│   │   │   │   ├── PhotonicSolarPanel.kt
│   │   │   │   ├── NeutronSolarPanel.kt
│   │   │   │   ├── NeutronFabricator.kt
│   │   │   │   └── (复用 ASA 的 SolarPanelBlockEntity)
│   │   │   ├── item/
│   │   │   │   └── ModItems.kt           # 19 个 @ModItem 类
│   │   │   ├── fluid/
│   │   │   │   └── NeutronFluid.kt
│   │   │   ├── screen/
│   │   │   │   └── NeutronFabricatorScreenHandler.kt
│   │   │   ├── sync/
│   │   │   │   └── NeutronFabricatorSync.kt  (若需)
│   │   │   └── tab/                       # 创造栏（可选，复用 IC2_SOLAR）
│   │   └── (client)                      # 若需客户端渲染
│   └── resources/
│       ├── fabric.mod.json
│       ├── assets/ic2_120_industrial_upgrade/
│       │   ├── lang/zh_cn.json, en_us.json
│       │   ├── blockstates/*.json
│       │   ├── models/block/*.json, models/item/*.json
│       │   └── textures/{block,item,fluid}/*.png
│       └── data/ic2_120_industrial_upgrade/recipes/  (datagen 产出)
└── (settings.gradle 增加 include)
```

---

## 10. 注册与初始化

### 10.1 settings.gradle
增加 `include 'industrial-upgrade-addon'`。

### 10.2 IC2IndustrialUpgrade.kt（入口）
```kotlin
object IC2IndustrialUpgrade : ModInitializer {
    const val MOD_ID = "ic2_120_industrial_upgrade"
    fun id(path: String) = Identifier(MOD_ID, path)
    override fun onInitialize() {
        Ic2IndustrialUpgradeConfig.loadOrThrow()  // 可选配置
        ClassScanner.scanAndRegister(MOD_ID, listOf(
            "ic2_120_industrial_upgrade.content.block",
            "ic2_120_industrial_upgrade.content.item",
            "ic2_120_industrial_upgrade.content.screen",
            "ic2_120_industrial_upgrade.content.fluid"
        ))
        // 注册分子重组仪配方（photoniy 等）到 ASA 的 MTRecipes
        // 注册压缩机配方（proton/neutron 系）
    }
}
```

### 10.3 fabric.mod.json
entrypoints.main = `ic2_120_industrial_upgrade.IC2IndustrialUpgrade`（kotlin adapter）。
fabric-datagen 入口同 ASA 模式。

---

## 11. 配方注册方式汇总

| 配方类型 | 实现方式 |
|---|---|
| 工作台有序（面板、核心、组件等） | datagen `ShapedRecipeJsonBuilder`，写在各 Block/Item 类的 `@RecipeProvider` 伴生方法 |
| 压缩机（proton/neutron 系、photoniy_ingot） | 注册到 ic2-fabric core 的压缩机配方表（参照 core 的 CompressorRecipes 写法），或在入口统一注册 |
| 分子重组仪（photoniy） | 复用 ASA 的 `MTRecipes`，新附属初始化时 `MTRecipes.addRecipe(...)` 追加 |
| 流体桶装填 | Fabric API `FluidStorage` + 单元（参照 core CellsAndBuckets） |

---

## 12. 国际化（lang）

zh_cn.json / en_us.json，命名空间 `ic2_120_industrial_upgrade`：
- 6 面板：`block.ic2_120_industrial_upgrade.spectral_solar_panel` = "光谱太阳能发电机" 等
- 中子制造机：`block.ic2_120_industrial_upgrade.neutron_fabricator` = "中子制造机"
- 19 物品：`item.ic2_120_industrial_upgrade.spectral_core` = "光谱核心" 等
- 流体：`fluid.ic2_120_industrial_upgrade.neutron` = "中子"

中文名参照 1.4.0 俄/英文意译 + IC2 社区惯用译名。

---

## 13. AGENTS.md 同步

完成后更新 `develop/` 上层的 `MOD-REGISTRY.md`：
- 新增条目：industrial-upgrade-addon（自有，ic2-fabric 子模块）
- 标注：源码来自 industrialupgrade 1.4.0（仅材质），代码自有（基于 ic2-fabric + ASA 框架）

---

## 14. 验证清单（实施后）

- [ ] `./gradlew :industrial-upgrade-addon:build` 编译通过
- [ ] `runDatagen` 生成全部 recipe/blockstate/model JSON
- [ ] 游戏内 6 级面板可放置、可发电、GUI 正常、tier 正确
- [ ] 中子制造机耗电产 Neutron 流体，桶可取出
- [ ] 合成链从 Quantum 一路合到 Neutron 通畅
- [ ] 强化铱板未出现在任何新附属配方（仅光伏铱板）
- [ ] 19 种新材料全部有贴图、可合成、JEI 可查
- [ ] 与 advanced-solar-addon 同时加载无冲突

---

## 15. 风险与待定细节

1. **中子制造机耗电/容量数值**：1.4.0 配置默认值需从 `Configs.java` 抽取（`Neutronfabricator` / `Neutronfabricator1`）。实施时取默认值或定为 tier=8、容量 100M、每 mB 消耗一满槽。
2. **压缩机配方注册 API**：✅ 已解决。core 的 `CompressorRecipeSerializer` 对外可用（JSON `type=ic2_120:compressing`），新附属 datagen 直接产出该格式 JSON。
3. **admin 级贴图**：✅ 已解决。1.4.0 admin 级贴图目录为 `admsp_*`，已拷贝。
4. **photoniy 的 MT 配方能耗**：✅ 已实现。1.4.0 为 25,000,000 EU，新附属沿用；MTRecipes.addRecipe 已开放供附属调用。
5. **ClassScanner 跨模块引用**：✅ 已验证。新附属 import ASA 类用于注册配方，靠 fabric.mod.json depends 保证加载顺序。
6. **core 流体注册体系开放**：✅ 已完成。core 的 `ModFluids.registerFluidFor()` 与 `Ic2Fluid` companion maps 已改为 public，供附属复用；新附属中子流体不再独立实现 Still/Flowing 子类，改用 `registerFluidFor`。core 自身 `registerFluid` 行为保持不变（含创造栏注册与蒸馏水特殊处理）。
7. **电压等级（tier）在高等级截断**：⚠️ 已记录到 TODO.md。`EnergyTier.euPerTickFromTier` 内部 clamp tier≤9，导致 Photonic(tier10)/Neutron(tier11) 输出被限制在 tier9 水平（2097152 EU/t），与 dayPower 不匹配。暂不修改 core tier 体系（牵涉面广），待评估方案。

---

## 附：与 advanced-solar-addon 的边界

| 内容 | 归属 |
|---|---|
| Advanced / Hybrid / Ultimate / Quantum 面板 | advanced-solar-addon（已存在） |
| Spectral / Proton / Singular / Admin / Photonic / Neutron 面板 | **新附属** |
| SolarPanelBlockEntity 基类、GUI、Sync | advanced-solar-addon（复用） |
| MolecularTransformer | advanced-solar-addon（复用） |
| Sunnarium 系列、光伏铱板、QuantumCore 等 | advanced-solar-addon（复用） |
| 19 种新材料、Neutron 流体、中子制造机 | **新附属** |
