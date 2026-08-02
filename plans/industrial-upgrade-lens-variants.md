# 工业升级附属（industrial-upgrade-addon）日光镜系加成内容补全计划书

> 版本：v2.0 ｜ ✅ **已实施**（2026-08-01，编译 + datagen + 贴图校验通过）
> 基线：IndustrialUpgrade **1.4.0**（提交 `7d2cee224`，与 `industrial-upgrade-addon-migration.md` 相同基线）
> 目标：透镜（sunlinse/nightlinse/rainlinse）+ 全部 10 级面板的 Sun/Rain 变体落在 `industrial-upgrade-addon`；`advanced-solar-addon` **不新增任何内容，仅开放基类扩展点**

---

## 0. 背景与定位

原版工业升级 1.4.0 中，除 6 级主线面板（Spectral→Neutron）外，还有一套**透镜加成体系**：

- 3 种透镜物品：`sunlinse`（日光镜）、`nightlinse`（夜光镜）、`rainlinse`（雨镜）
- 每个等级的普通面板可通过透镜升级为 2 个变体：
  - **Sun 变体**（`{name}_solar_panelsun`）：仅晴天白天发电，功率 = 普通面板 day × 2
  - **Rain 变体**（`{name}_solar_panelrain`）：仅下雨/雷雨时发电，功率 = 普通面板 night

现 `industrial-upgrade-addon` 只移植了 6 级主线面板，**透镜物品、变体面板及全部相关合成均缺失**。本计划书补齐该体系。

**范围界定**：
- 补透镜 + Sun/Rain 变体体系（对应原版 `CraftingThings.sunlinse/nightlinse/rainlinse` 与 `*_solar_panelsun/*_solar_panelrain` 方块）。
- **全部内容在 `industrial-upgrade-addon`**：透镜物品、Sun/Rain 变体基类、以及**全部 10 级**（ASA 4 级 Advanced→Quantum + 本 addon 6 级 Spectral→Neutron）的变体方块（共 20 个），一律注册于 `ic2_120_industrial_upgrade` 命名空间。
- `advanced-solar-addon` **不新增任何物品/方块**，仅开放基类扩展点供 addon 继承复用：`SolarPanelBlockEntity` 的 `checkSky`/`hasSkyAccess` 转 protected open、`generationState` protected set、`GenerationState` 增加 `RAIN`、构造增加 `rainPower`（默认 0）；4 个面板 Block 类转 `open`。均为向后兼容改动。
- 不做：1.4.0 中的 3.x 系多元素透镜（aer/earth/nether/end 等，属工业升级 3.x 重构体系，非 1.4.0 内容）、变体面板之外的其他加成物品。

---

## 1. 原版 1.4.0 行为分析（研究结论）

### 1.1 透镜物品与用途（`CraftingThings` 枚举）

| 1.4.0 物品 ID | 英文名 | 中文建议 | 用途 |
|---|---|---|---|
| `sunlinse` | Sun Linse | 日光镜 | 合成 Sun 变体面板、合成 rainlinse |
| `nightlinse` | Night Linse | 夜光镜 | 合成 rainlinse 的原料 |
| `rainlinse` | Rain Linse | 雨镜 | 合成 Rain 变体面板 |

### 1.2 Sun/Rain 变体行为逻辑（TileEntitySunPanel / TileEntityRainPanel）

**Sun 变体**（`TileEntitySunPanel`）：
```
active = 天空可见 && 是白天 && !(可下雨 && (下雨 || 雷雨)) → DAY（否则 NONE）
发电：DAY 状态按 sunPower（= 普通面板 dayPower × 2）
```
即：**晴天白天**（阴雨/夜晚不发电），功率为普通面板白天功率的 2 倍。

**Rain 变体**（`TileEntityRainPanel`）：
```
active = 天空可见 && 生物群系可下雨 && (下雨 || 雷雨) → RAIN（否则 NONE）
发电：RAIN 状态按 rainPower（= 普通面板 nightPower）
```
即：**下雨/雷雨天**发电，功率等于普通面板夜间功率（Admin 级起原版 night=day，故 rain=day）。

> 两变体均继承 BasePanelTE：storage/tier 与同等级普通面板一致，容量、充电槽、电网行为完全相同，仅"何时发电、发多少"不同。

### 1.3 原版相对倍率（关键：只复制倍率，不复用绝对数值）

| 面板 | 普通 day | 普通 night | sun (=day×2) | rain (=night) |
|---|---|---|---|---|
| Advanced | 4 | 4 | 8 | 4 |
| Hybrid | 32 | 8 | 64 | 8 |
| Ultimate | 256 | 64 | 512 | 64 |
| Quantum | 2048 | 1024 | 4096 | 1024 |
| Spectral | 8192 | 5000 | 16384 | 5000 |
| Proton | 32768 | 20000 | 65536 | 20000 |
| Singular | 131072 | 104857 | 262144 | 104857 |
| Admin | 554288 | 554288 | 1108576 | 554288 |
| Photonic | 2621440 | 2621440 | 5242880 | 2621440 |
| Neutron | 10485760 | 10485760 | 20971520 | 10485760 |

**结论（倍率规则）**：
- Sun 变体功率 = 普通面板 **dayPower × 2**
- Rain 变体功率 = 普通面板 **nightPower**
- storage、tier 与普通面板完全相同

> 数值绝对量一律映射到各自现有曲线（ASA 4 级按 ASA 既有数值、addon 6 级按 ×4 递推），见 §4 数值表。

### 1.4 原版配方（SPPRecipes.java）

**变体面板合成**（每级 2 条，共 20 条）：
```
"BA"  →  B = 对应普通面板，A = 透镜（sunlinse / rainlinse）
```
即 2×1 网格：上面放面板、下面放透镜，产出 1 个变体面板。

**rainlinse 合成**（唯一有配方的透镜）：
```
"USU" →  U = sunlinse ×2，S = nightlinse ×1
```

**⚠️ 原版缺陷**：`sunlinse`、`nightlinse` 在 1.4.0 **没有任何合成配方**（全仓库仅作为 rainlinse/变体面板的原料出现；3.x 重构版同样缺失），只能从创造模式获取。本计划书按 §5 设计补全配方。

### 1.5 原版贴图资源（可复用）

```
textures/items/                sunlinse.png / nightlinse.png / rainlinse.png
textures/blocks/               {各面板} topsun.png / toprain.png
                               例：spectral_topsun.png、spectral_toprain.png
                               （side/bottom 与普通面板相同，复用现有）
```

---

## 2. 缺口清单（现状 vs 目标）

| # | 内容 | 现状 | 目标 | 归属 |
|---|---|---|---|---|
| 1 | `sunlinse` 日光镜物品 + 贴图 + 合成 | 无 | 新增 | addon |
| 2 | `nightlinse` 夜光镜物品 + 贴图 + 合成 | 无 | 新增 | addon |
| 3 | `rainlinse` 雨镜物品 + 贴图 + 合成 | 无 | 新增 | addon |
| 4 | Sun/Rain 变体基类（SunPanelBlockEntity / RainPanelBlockEntity） | 无 | 新增 | addon |
| 5 | ASA 4 级 × Sun/Rain 变体方块（8 个）+ blockstate/model/lang/贴图 | 无 | 新增 | addon（命名空间 ic2_120_industrial_upgrade） |
| 6 | addon 6 级 × Sun/Rain 变体方块（12 个）+ blockstate/model/lang/贴图 | 无 | 新增 | addon |
| 7 | 20 条变体面板合成配方（8+12） | 无 | 新增 | addon |
| 8 | 面板 tick 的 Sun/Rain 生成逻辑 | `SolarPanelBlockEntity` 仅 NONE/NIGHT/DAY | 开放扩展点 | ASA（仅代码开放） |

---

## 3. 新增内容总览（全部在 industrial-upgrade-addon）

- 物品（3）：`sunlinse`、`nightlinse`、`rainlinse`（ID 前缀 `ic2_120_industrial_upgrade`）
- 变体基类（2）：`SunPanelBlockEntity`、`RainPanelBlockEntity`
- 方块（20）：ASA 4 级（advanced/hybrid/ultimate/quantum）+ addon 6 级（spectral/proton/singular/admin/photonic/neutron）× {`*_solar_panelsun`, `*_solar_panelrain`}
- 配方（23）：3 条透镜 + 20 条变体面板
- lang：zh_cn.json / en_us.json 各 +23 条

---

## 4. 数值表（只复制相对倍率，绝对数值映射到各自现有曲线）

> 倍率规则（原版 1.4.0 结论）：**Sun = day×2，Rain = night**，storage/tier 与同等级普通面板完全一致。
> 下表绝对数值：ASA 4 级取 ASA 既有面板数值（day 每级 ×8）；addon 6 级取本 addon ×4 递推数值。

**ASA 4 级变体（8 个）**：

| 变体等级 | day | night | **sun** (=day×2) | **rain** (=night) | maxStorage | tier |
|---|---|---|---|---|---|---|
| Advanced | 8 | 1 | **16** | **1** | 32,000 | 1 |
| Hybrid | 64 | 8 | **128** | **8** | 100,000 | 2 |
| Ultimate | 512 | 64 | **1024** | **64** | 1,000,000 | 3 |
| Quantum | 4096 | 2048 | **8192** | **2048** | 10,000,000 | 5 |

**addon 6 级变体（12 个）**：

| 变体等级 | day | night | **sun** (=day×2) | **rain** (=night) | maxStorage | tier |
|---|---|---|---|---|---|---|
| Spectral | 16384 | 8192 | **32768** | **8192** | 100,000,000 | 6 |
| Proton | 65536 | 32768 | **131072** | **32768** | 1,000,000,000 | 7 |
| Singular | 262144 | 131072 | **524288** | **131072** | 10,000,000,000 | 8 |
| Admin | 1048576 | 1048576 | **2097152** | **1048576** | 100,000,000,000 | 9 |
| Photonic | 4194304 | 4194304 | **8388608** | **4194304** | 1,000,000,000,000 | 10 |
| Neutron | 16777216 | 16777216 | **33554432** | **16777216** | 10,000,000,000,000 | 11 |

---

## 5. 合成配方设计

### 5.1 透镜（3 条，全部在 addon）

| 产物 | 配方 | 说明 |
|---|---|---|
| `sunlinse` 日光镜 | 十字：` G / GSG / G `（G=萤石粉 glowstone_dust，S=强化玻璃 reinforced_glass） | **新设计**（原版无配方）：强化玻璃聚萤石之光 |
| `nightlinse` 夜光镜 | 十字：` C / CSC / C `（C=煤炭 coal，S=强化玻璃） | **新设计**（原版无配方）：强化玻璃聚煤之暗 |
| `rainlinse` 雨镜 | `USU`：2×sunlinse + 1×nightlinse | **沿用 1.4.0 原版配方** |

> 设计依据：配方材料全部取 ic2-fabric core / 原版已有物品（强化玻璃为 core 的 `ic2_120:reinforced_glass`，萤石粉/煤炭为原版），addon 引用 core 无依赖问题；与 1.4.0 配方风格（5 材料十字形）一致。如需更贵可改用 IC2 碳板/铱板，实施时按服主喜好定。

### 5.2 变体面板（20 条，全部沿用 1.4.0 的 `"BA"` 形式，全部在 addon）

统一形式 `"BA"`（2×1，面板在上、镜在下），`A`=透镜、`B`=对应普通面板。

| 产物 | B | A |
|---|---|---|
| advanced_solar_panelsun / _panelrain | advanced_solar_panel（ASA） | sunlinse / rainlinse |
| hybrid_solar_panelsun / _panelrain | hybrid_solar_panel（ASA） | sunlinse / rainlinse |
| ultimate_solar_panelsun / _panelrain | ultimate_solar_panel（ASA） | sunlinse / rainlinse |
| quantum_solar_panelsun / _panelrain | quantum_solar_panel（ASA） | sunlinse / rainlinse |
| spectral_solar_panelsun / _panelrain | spectral_solar_panel | sunlinse / rainlinse |
| proton_solar_panelsun / _panelrain | proton_solar_panel | sunlinse / rainlinse |
| singular_solar_panelsun / _panelrain | singular_solar_panel | sunlinse / rainlinse |
| admin_solar_panelsun / _panelrain | admin_solar_panel | sunlinse / rainlinse |
| photonic_solar_panelsun / _panelrain | photonic_solar_panel | sunlinse / rainlinse |
| neutron_solar_panelsun / _panelrain | neutron_solar_panel | sunlinse / rainlinse |

> 产物全部注册于 `ic2_120_industrial_upgrade`；前 4 行原料面板来自 ASA（`ic2_120_advanced_solar_addon:*_solar_panel`），透镜为本 addon 物品。

### 5.3 不可逆性与合成边界（设计决策：方案 A，忠实原版）

已确认按**方案 A**（忠实 1.4.0）实施：

- **单向合成**：只存在"普通面板 + 透镜 → 变体面板"，**不提供反向配方**（变体 → 面板 + 透镜）。
- **透镜一次性消耗**：合成后透镜不返还；扳手拆下方块得到变体面板本体，仍不可还原。
- **变体不参与任何后续合成**：升级链配方（同等级面板 ×4 + 核心）只接受**普通面板**作为原料；`*_solar_panelsun` / `*_solar_panelrain` 仅作为产物存在，**不作为任何配方的输入**。
- 玩家需要普通面板时只能直接合成普通面板，不可从变体逆向得到。

理由：忠实 1.4.0 行为；合成矩阵最小化，升级链清晰；无新增反向/等价配方逻辑。

---

## 6. 技术实现方案

### 6.1 ASA 基类扩展点（**ASA 唯一改动**，向后兼容）

`advanced-solar-addon` 的 `SolarPanelBlockEntity` 开放以下扩展点（均为非破坏性）：

1. `GenerationState` 枚举增加 `RAIN`（`NONE, NIGHT, DAY, RAIN`）
2. `checkSky()`：`private fun` → `protected open fun`；`hasSkyAccess()` 同转 `protected`
3. `generationState`：`var ... private set` → `var ... protected set`
4. 构造参数追加 `rainPower: Int = 0`；父类 tick 的 `when(generationState)` 补 `RAIN -> sync.generateEnergy(rainPower)`
5. 4 个面板 Block 类（Advanced/Hybrid/Ultimate/QuantumSolarPanelBlock）转 `open`（供 addon 变体继承）

> ASA 不新增任何物品/方块/资源。既有 4 级 ASA 面板行为零变化。

### 6.2 新增变体基类（**addon 内**，`content/block/`）

```
SunPanelBlockEntity（抽象，addon）
    └─ 继承 ASA 的 SolarPanelBlockEntity
    └─ 构造参数追加 sunPower；重写 checkSky()：
        天空可见 && 白天 && !(可下雨 && 下雨/雷雨) → DAY，否则 NONE
    └─ 传给父类 dayPower = sunPower（DAY 分支即按 sunPower 发电），nightPower = 0
RainPanelBlockEntity（抽象，addon）
    └─ 继承 ASA 的 SolarPanelBlockEntity
    └─ 构造参数追加 rainPower；重写 checkSky()：
        天空可见 && 可下雨 && (下雨 || 雷雨) → RAIN，否则 NONE
    └─ 传给父类 nightPower = rainPower 且 rainPower = rainPower（RAIN 分支按 rainPower 发电）
```

实现细节：
- 天空可见/可下雨判断复用父类 `hasSkyAccess()`（§6.1 已开放）。
- 末地/其他维度：Sun 变体末地恒 DAY（与原版一致）、非主世界 NONE；Rain 变体仅主世界生效。
- GUI：复用父类 `SolarPanelScreenHandler`，仅标题经 `getBlockName()` 差异化；`sync.generationState=3`（RAIN）沿用现有样式，不新增专用图标。

### 6.3 变体方块（20 个，全部在 addon 的 `content/block/`）

每级一对类（Block + BlockEntity）。**ASA 4 级变体继承 ASA 的 Block 类**（§6.1 已 open），**addon 6 级变体继承 `IndustrialSolarPanelBlock`**：

```kotlin
// 示例：ASA 级变体（addon 命名空间）
@ModBlock(name = "advanced_solar_panelsun", registerItem = true,
          tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class AdvancedSolarPanelSunBlock : AdvancedSolarPanelBlock() { ... }

@ModBlockEntity(block = AdvancedSolarPanelSunBlock::class)
class AdvancedSolarPanelSunBlockEntity(pos, state) : SunPanelBlockEntity(
    type, pos, state,
    dayPower = 8, nightPower = 1, maxStorage = 32_000L, tier = 1,
    sunPower = 16, activeProperty = AdvancedSolarPanelBlock.ACTIVE) {
    override fun getBlockName() = "advanced_solar_panelsun"
    override fun getDisplayName() = Text.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}

// 示例：addon 级变体
@ModBlock(name = "spectral_solar_panelsun", registerItem = true,
          tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class SpectralSolarPanelSunBlock : IndustrialSolarPanelBlock() { ... }

@ModBlockEntity(block = SpectralSolarPanelSunBlock::class)
class SpectralSolarPanelSunBlockEntity(pos, state) : SunPanelBlockEntity(
    type, pos, state,
    dayPower = 16384, nightPower = 8192, maxStorage = 100_000_000L, tier = 6,
    sunPower = 32768, activeProperty = SOLAR_ACTIVE) {
    override fun getBlockName() = "spectral_solar_panelsun"
    override fun getDisplayName() = Text.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
```

> addon `fabric.mod.json` 的 depends 已含 `ic2_120_advanced_solar_addon`，保证基类/Block 加载顺序。

### 6.4 物品与配方注册（全部在 addon）

- 3 个透镜：addon 的 `content/item/Lenses.kt`，`@ModItem(name="sunlinse", tab=CreativeTab.INDUSTRIAL_UPGRADE, group="material")` + 伴生 `@RecipeProvider generateRecipes`（datagen `ShapedRecipeJsonBuilder`）。物品 ID：`ic2_120_industrial_upgrade:sunlinse` 等。
- 变体配方：写在各 Block 伴生 `@RecipeProvider`（20 条）。ASA 4 级的原料面板为 `ic2_120_advanced_solar_addon:*_solar_panel`。
- 物品模型：`models/item/sunlinse.json` 等（layer0 指向贴图）；方块模型：top 贴图替换为 `*_top_sun` / `*_top_rain`，side/bottom——ASA 4 级变体复用 ASA 面板贴图（跨 mod 引用 `ic2_120_advanced_solar_addon:block/*_solar_side` 等），addon 6 级变体复用本模块普通面板贴图。

### 6.5 贴图移植（从 1.4.0 checkout 拷贝，全部到 addon）

```
develop/industrialupgrade/src/main/resources/assets/super_solar_panels/
  透镜（→ textures/item/）：sunlinse.png / nightlinse.png / rainlinse.png
  topsun/toprain（→ textures/block/，20 张）：
    ASA 4 级：advanced/hybrid/ultimate/quantum × topsun/toprain
    addon 6 级：spectral/psp(singulary)/admsp/phsp/nsp × topsun/toprain
```

> 1.4.0 文件名前缀对照：advanced→`advanced solar`、hybrid→`hybrid solar`、ultimate→`ultimate solar`、quantum→`quantum solar`、spectral→`spectral`、proton→`psp`、singular→`singulary`、admin→`admsp`、photonic→`phsp`、neutron→`nsp`（实施时脚本核对实际文件，含空格的旧文件名需重命名）。
> 拷贝后统一命名为 `{等级}_solar_panel_top_sun.png` / `_top_rain.png`。

### 6.6 国际化（全部在 addon，前缀 `ic2_120_industrial_upgrade`，zh/en 各 +23 条）

```
item.ic2_120_industrial_upgrade.sunlinse       = 日光镜 (Sun Linse)
item.ic2_120_industrial_upgrade.nightlinse     = 夜光镜 (Night Linse)
item.ic2_120_industrial_upgrade.rainlinse      = 雨镜 (Rain Linse)
block.ic2_120_industrial_upgrade.advanced_solar_panelsun  = 高级日光太阳能发电机 (Advanced Sun Panel)
block.ic2_120_industrial_upgrade.advanced_solar_panelrain = 高级雨能太阳能发电机 (Advanced Rain Panel)
block.ic2_120_industrial_upgrade.spectral_solar_panelsun  = 光谱日光太阳能发电机 (Spectral Sun Panel)
block.ic2_120_industrial_upgrade.spectral_solar_panelrain = 光谱雨能太阳能发电机 (Spectral Rain Panel)
...（其余同构）
```

---

## 7. 模块结构与文件清单

**advanced-solar-addon（仅代码开放，无新增文件）**：
```
advanced-solar-addon/src/main/kotlin/ic2_120_advanced_solar_addon/content/block/
├── SolarPanelBlockEntity.kt      # 改：GenerationState.RAIN + checkSky/hasSkyAccess 开放 + generationState protected set + rainPower 参数 + tick RAIN 分支
├── AdvancedSolarPanel.kt         # 改：class → open class
├── HybridSolarPanel.kt           # 改：class → open class
├── UltimateSolarPanel.kt         # 改：class → open class
└── QuantumSolarPanel.kt          # 改：class → open class
```

**industrial-upgrade-addon（全部新增）**：
```
industrial-upgrade-addon/src/main/
├── kotlin/ic2_120_industrial_upgrade/content/
│   ├── block/
│   │   ├── SunPanelBlockEntity.kt                    # 新增：Sun 变体抽象基类
│   │   ├── RainPanelBlockEntity.kt                   # 新增：Rain 变体抽象基类
│   │   ├── AdvancedSolarPanelSun.kt / Rain.kt        # 新增：ASA 4 级变体（继承 ASA Block）
│   │   ├── HybridSolarPanelSun.kt / Rain.kt          # 新增
│   │   ├── UltimateSolarPanelSun.kt / Rain.kt        # 新增
│   │   ├── QuantumSolarPanelSun.kt / Rain.kt         # 新增
│   │   ├── SpectralSolarPanelSun.kt / Rain.kt        # 新增：addon 6 级变体
│   │   ├── ProtonSolarPanelSun.kt / Rain.kt          # 新增
│   │   ├── SingularSolarPanelSun.kt / Rain.kt        # 新增
│   │   ├── AdminSolarPanelSun.kt / Rain.kt           # 新增
│   │   ├── PhotonicSolarPanelSun.kt / Rain.kt        # 新增
│   │   └── NeutronSolarPanelSun.kt / Rain.kt         # 新增
│   └── item/
│       └── Lenses.kt                                 # 新增：sunlinse/nightlinse/rainlinse
└── resources/assets/ic2_120_industrial_upgrade/
    ├── lang/zh_cn.json, en_us.json                   # +23 条
    ├── blockstates/*_solar_panelsun.json, *_solar_panelrain.json   # 20 个
    ├── models/block/*_solar_panelsun.json, *_solar_panelrain.json  # 20 个
    ├── models/item/*_solar_panelsun.json, *_solar_panelrain.json   # 20 个
    ├── models/item/sunlinse.json, nightlinse.json, rainlinse.json  # 3 个
    └── textures/
        ├── item/sunlinse.png, nightlinse.png, rainlinse.png        # 3 个
        └── block/*_solar_panel_top_sun.png, *_solar_panel_top_rain.png  # 20 个
```

---

## 8. 边界

| 内容 | 归属 | 说明 |
|---|---|---|
| 透镜物品（3 种）+ Sun/Rain 变体基类 | industrial-upgrade-addon | 命名空间 `ic2_120_industrial_upgrade` |
| 全部 10 级 × Sun/Rain 变体（20 个方块） | industrial-upgrade-addon | 含 ASA 4 级（继承 ASA Block 类）与 addon 6 级 |
| ASA 基类扩展点（checkSky/generationState/RAIN/rainPower + Block open） | advanced-solar-addon（仅代码） | 无新增物品/方块/资源 |
| 3.x 多元素透镜（aer/earth/nether/end/moon） | 不做 | 属 3.x 重构体系，非 1.4.0 基线 |

---

## 9. 文档同步

已完成更新 `MOD-REGISTRY.md`：
- **advanced-solar-addon 条目**：标注仅开放基类扩展点（无新增内容），向后兼容。
- **industrial-upgrade-addon 条目**：标注透镜 + 全部 10 级 Sun/Rain 变体（20 方块、23 配方）。
- 本计划书归档于 `plans/`。

---

## 10. 验证清单（实施后）

- [x] `./gradlew :advanced-solar-addon:build :industrial-upgrade-addon:build` 编译通过
- [x] datagen 产出 23 条配方 JSON（addon：3 透镜 + 20 变体）；ASA 无变体/透镜产物（回归 19 条）
- [x] 3 种透镜（addon）可合成、贴图正常
- [x] 20 个变体方块（addon）blockstate/model/item-model/贴图完整（贴图引用校验 0 缺失）
- [ ] 游戏内：Sun 变体（10 级）晴天白天按 day×2 发电；夜晚、下雨、雷雨不发电
- [ ] 游戏内：Rain 变体（10 级）雨天/雷雨按 night 发电；晴天、夜晚不发电
- [ ] storage/tier 与同等级普通面板一致
- [ ] 20 条变体配方：面板 + 透镜 → 变体，产出 1 个
- [ ] **无反向配方**：扳手拆下变体得到变体本体；`*_solar_panelsun` / `*_solar_panelrain` 不出现在任何配方原料中
- [ ] ASA 既有 4 级普通面板行为不变（回归）；addon 既有 6 级普通面板行为不变（回归）
- [x] zh_cn/en_us 完整（addon +23 条），无缺 key

---

## 11. 风险与待定

1. **ASA 基类改动回归**：`checkSky()` 转 open 后既有 ASA 4 级面板逻辑不变（仅可见性/可覆盖性变化），风险低；仍需游戏内回归验证。
2. **父类 tick 的 RAIN 分支**：父类构造 `rainPower` 默认 0，不影响既有面板；Rain 变体 GUI 生成状态沿用普通面板样式（`sync.generationState=3`），不新增专用图标。
3. **透镜配方数值**：原版无配方可抄，§5.1 为设计建议；如需调整造价（如改用 IC2 碳板/铱板）由 datagen 一处修改。
4. **贴图命名**：1.4.0 含空格文件名已在拷贝时统一重命名。
5. **Sun 变体在末地**：沿用父类逻辑（末地→DAY 全天发电，等同普通面板），与原版 1.4.0 的"Sun 面板在末地恒 DAY"行为一致，无需特判。
