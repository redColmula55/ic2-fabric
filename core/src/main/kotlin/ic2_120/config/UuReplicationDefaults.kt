package ic2_120.config

/**
 * UU 物质复制默认白名单
 *
 * 定价原则：
* - 有机物 >> 无机物（创造"生命"比创造"死物"更昂贵）
* - IC2 模组物品保持原值
 * - 原版物品按有机/无机重新平衡
 * - 矿物定价公式：基础产物(粗矿/宝石) = 基准；矿石 = 2 × 基准（1 矿石可打碎为 2 粗矿）；
 *   深板岩矿石 = 1.2 × 矿石 = 2.4 × 基准；锭 = 粗矿（1:1 冶炼）；块 = 9 × 锭/宝石；粒 = 锭 / 9
*/
object UuReplicationDefaults {

    val defaultWhitelist: Map<String, Int> = linkedMapOf(
        // ========== 基础无机建材 ==========
        "minecraft:cobblestone" to 10,
        "minecraft:stone" to 20,
        "minecraft:dirt" to 15,
        "minecraft:coarse_dirt" to 20,
        "minecraft:sand" to 15,
        "minecraft:red_sand" to 20,
        "minecraft:gravel" to 12,
        "minecraft:clay_ball" to 20,
        "minecraft:andesite" to 25,
        "minecraft:granite" to 25,
        "minecraft:diorite" to 25,
        "minecraft:polished_andesite" to 50,
        "minecraft:polished_granite" to 50,
        "minecraft:polished_diorite" to 50,

        // ========== 石材加工品 ==========
        "minecraft:sandstone" to 30,
        "minecraft:smooth_sandstone" to 50,
        "minecraft:cut_sandstone" to 50,
        "minecraft:chiseled_sandstone" to 80,
        "minecraft:red_sandstone" to 40,
        "minecraft:sandstone_stairs" to 60,
        "minecraft:sandstone_slab" to 20,

        "minecraft:stone_bricks" to 50,
        "minecraft:mossy_cobblestone" to 50,
        "minecraft:cracked_stone_bricks" to 60,
        "minecraft:stone_slab" to 40,
        "minecraft:stone_pressure_plate" to 60,
        // 旧版 UU 配方中当前没有同类自定义定价的物品
        "minecraft:stone_stairs" to 346466802,
        "minecraft:wooden_pressure_plate" to 192481557,
        // 旧版白羊毛属于当前已有的羊毛定价范围，沿用当前黑羊毛价格而非旧版配方价
        "minecraft:white_wool" to 200000,

        // ========== 陶瓦系列 ==========
        "minecraft:terracotta" to 80,
        "minecraft:white_terracotta" to 100,
        "minecraft:orange_terracotta" to 100,
        "minecraft:yellow_terracotta" to 100,
        "minecraft:light_gray_terracotta" to 100,
        "minecraft:brown_terracotta" to 100,
        "minecraft:red_terracotta" to 100,
        "minecraft:blue_terracotta" to 100,

        // ========== 新版本石头 ==========
        "minecraft:deepslate" to 30,
        "minecraft:polished_deepslate" to 60,
        "minecraft:deepslate_bricks" to 60,
        "minecraft:cracked_deepslate_bricks" to 70,
        "minecraft:deepslate_tiles" to 70,
        "minecraft:calcite" to 20,
        "minecraft:tuff" to 25,
        "minecraft:polished_tuff" to 50,
        "minecraft:mud" to 30,
        "minecraft:mud_bricks" to 60,
        "minecraft:packed_mud" to 50,

        // ========== 玻璃 ==========
        "minecraft:glass" to 100,

        // ========== 矿物系列 ==========

        // ---- 煤炭（基准 coal = 500）----
        "minecraft:coal" to 500,
        "minecraft:coal_ore" to 1000,
        "minecraft:deepslate_coal_ore" to 1200,
        "minecraft:coal_block" to 4500,

        // ---- 铁（基准 raw_iron = 540）----
        "minecraft:raw_iron" to 540,
        "minecraft:iron_ore" to 1080,
        "minecraft:deepslate_iron_ore" to 1296,
        "minecraft:iron_ingot" to 540,
        "minecraft:iron_nugget" to 60,
        "minecraft:iron_block" to 4860,
        "minecraft:raw_iron_block" to 4860,

        // ---- 铜（基准 raw_copper = 500）----
        "minecraft:raw_copper" to 500,
        "minecraft:copper_ore" to 1000,
        "minecraft:deepslate_copper_ore" to 1200,
        "minecraft:copper_ingot" to 500,
        "minecraft:copper_block" to 4500,
        "minecraft:raw_copper_block" to 4500,

        // ---- 金（基准 raw_gold = 2500）----
        "minecraft:raw_gold" to 2500,
        "minecraft:gold_ore" to 5000,
        "minecraft:deepslate_gold_ore" to 6000,
        "minecraft:gold_ingot" to 2500,
        "minecraft:gold_nugget" to 278,
        "minecraft:gold_block" to 22500,
        "minecraft:raw_gold_block" to 22500,

        // ---- 红石（基准 redstone = 1000）----
        "minecraft:redstone" to 1000,
        "minecraft:redstone_ore" to 2000,
        "minecraft:deepslate_redstone_ore" to 2400,
        "minecraft:redstone_block" to 9000,

        // ---- 青金石（基准 lapis = 5000）----
        "minecraft:lapis_lazuli" to 5000,
        "minecraft:lapis_ore" to 10000,
        "minecraft:deepslate_lapis_ore" to 12000,
        "minecraft:lapis_block" to 45000,

        // ---- 钻石（基准 diamond = 50000）----
        "minecraft:diamond" to 50000,
        "minecraft:diamond_ore" to 100000,
        "minecraft:deepslate_diamond_ore" to 120000,
        "minecraft:diamond_block" to 450000,

        // ---- 绿宝石（基准 emerald = 200000）----
        "minecraft:emerald" to 200000,
        "minecraft:emerald_ore" to 400000,
        "minecraft:deepslate_emerald_ore" to 480000,
        "minecraft:emerald_block" to 1800000,

        // ---- 下界石英（基准 quartz = 1000）----
        "minecraft:quartz" to 1000,
        "minecraft:nether_quartz_ore" to 2000,

        // ---- 下界合金（基准 netherite_scrap = 500000，锭 = 4 scrap + 4 gold）----
        "minecraft:ancient_debris" to 500000,
        "minecraft:netherite_scrap" to 500000,
        "minecraft:netherite_ingot" to 2010000,
        "minecraft:netherite_block" to 18090000,

        // ---- 紫水晶（基准 shard = 2000，块 = 4 shard）----
        "minecraft:amethyst_shard" to 2000,
        "minecraft:amethyst_block" to 8000,

        // ---- 其他矿物产物 ----
        "minecraft:flint" to 5000,
        "minecraft:snowball" to 2000,

        // IC2 矿石（保持原值，补充深板岩和粗矿变体）
        "ic2_120:tin_ore" to 1360,
        "ic2_120:deepslate_tin_ore" to 1632,
        "ic2_120:raw_tin" to 680,
        // 锡锭 = raw_tin 1:1 冶炼，无损耗（补遗漏：锭应与粗矿同价，参照铁/铜/金）
        "ic2_120:tin_ingot" to 680,
        "ic2_120:lead_ore" to 9182,
        "ic2_120:deepslate_lead_ore" to 11018,
        "ic2_120:raw_lead" to 4591,
        "ic2_120:uranium_ore" to 16070,
        "ic2_120:deepslate_uranium_ore" to 19284,
        "ic2_120:raw_uranium" to 8035,
        // 乏燃料棒：原燃料棒价值 − 已发电的 EU 价值（1 uB = 1000 EU，即 1 mB UU = 1M EU）。
        // 发电 EU 来自 processChamber：铀燃料棒 basePulses=1+cells/2，totalPulses×maxUse tick；MOX 同公式但 maxUse 减半。
        // 这些是反应堆燃烧后的副产物，无生成配方，必须显式定价。
        // 钚 / small_plutonium / MOX / RTG 靶丸的成本由这些乏燃料棒经热离心反推得到，不另设白名单。
        "ic2_120:depleted_uranium_fuel_rod" to 17875,      // 单铀棒 17895uB − 发电20000EU(20uB)
        "ic2_120:depleted_dual_uranium_fuel_rod" to 36276, // 双铀棒 36356uB − 发电80000EU(80uB)
        "ic2_120:depleted_quad_uranium_fuel_rod" to 74050, // 四铀棒 74290uB − 发电240000EU(240uB)
        "ic2_120:depleted_mox_fuel_rod" to 80062,          // 单MOX棒 80072uB − 发电10000EU(10uB)
        "ic2_120:depleted_dual_mox_fuel_rod" to 160660,    // 双MOX棒 160700uB − 发电40000EU(40uB)
        "ic2_120:depleted_quad_mox_fuel_rod" to 322391,    // 四MOX棒 322511uB − 发电120000EU(120uB)
        "ic2_120:iridium_ore_item" to 120000,
        "ic2_120:iridium_shard" to 13330,

        // IC2 钢与高级合金
        // 钢锭 = 10,000 uB（10 mB）；钢块 = 9 × 钢锭
        "ic2_120:steel_ingot" to 10000,
        "ic2_120:steel_block" to 90000,
        // 青铜锭：4 锭 = 3 铜 + 1 锡，单锭成本 = (3×copper_ingot 500 + tin_ingot 680) ÷ 4 = 545；
        // 作为成品单独复制时加 50% 加工损耗 → 545 × 1.5 = 817.5 → 取整 818
        "ic2_120:bronze_ingot" to 818,
        // 青铜块 = 9 × 青铜锭（纯 9 倍，块不再叠加损耗，参照 netherite/amethyst/coal 惯例）
        "ic2_120:bronze_block" to 7362,
        // 9 钢 + 9 青铜 + 9 锡 -> 2 混合金属锭 -> 1 高级合金；原料总价 × 1.5，取整
        "ic2_120:alloy" to 75769,

        // ========== 海洋物品 ==========
        "minecraft:prismarine" to 200000,
        "minecraft:prismarine_bricks" to 200000,
        "minecraft:dark_prismarine" to 300000,
        "minecraft:prismarine_crystals" to 300000,
        "minecraft:wet_sponge" to 500000,
        "minecraft:obsidian" to 100000,
        "minecraft:netherrack" to 2,
        "minecraft:glowstone_dust" to 8,

        // ========== 有机物-树木（昂贵）==========
        "minecraft:oak_log" to 50000,
        "minecraft:spruce_log" to 50000,
        "minecraft:birch_log" to 50000,
        "minecraft:jungle_log" to 50000,
        "minecraft:acacia_log" to 500000,
        "minecraft:dark_oak_log" to 50000,

        // IC2 橡胶树（保持不变）
        "ic2_120:rubber_sapling" to 3571823,
        "ic2_120:rubber_wood" to 930362,
        "ic2_120:rubber_log" to 930362,
        "ic2_120:resin" to 33314116,

        // 树苗（极贵 - 生命潜力）
        "minecraft:oak_sapling" to 500000,
        "minecraft:spruce_sapling" to 500000,
        "minecraft:birch_sapling" to 500000,
        "minecraft:jungle_sapling" to 500000,
        "minecraft:acacia_sapling" to 1275651,
        "minecraft:dark_oak_sapling" to 500000,

        // 树叶（贵）
        "minecraft:oak_leaves" to 30000,
        "minecraft:spruce_leaves" to 30000,
        "minecraft:birch_leaves" to 30000,
        "minecraft:jungle_leaves" to 30000,
        "minecraft:acacia_leaves" to 30000,
        "minecraft:dark_oak_leaves" to 30000,

        // 木制品（失去生命属性，便宜）
        "minecraft:oak_planks" to 100,
        "minecraft:spruce_planks" to 100,
        "minecraft:birch_planks" to 100,
        "minecraft:jungle_planks" to 100,
        "minecraft:acacia_planks" to 100,
        "minecraft:dark_oak_planks" to 100,

        "minecraft:oak_fence" to 150,
        "minecraft:oak_stairs" to 200,
        "minecraft:oak_door" to 200,
        "minecraft:oak_slab" to 60,
        "minecraft:oak_fence_gate" to 250,
        "minecraft:oak_trapdoor" to 250,

        // ========== 有机物-农作物（昂贵）==========
        "minecraft:wheat_seeds" to 200000,
        "minecraft:melon_seeds" to 200000,
        "minecraft:pumpkin_seeds" to 200000,
        "minecraft:beetroot_seeds" to 200000,
        "minecraft:cocoa_beans" to 200000,

        "minecraft:wheat" to 500000,
        "minecraft:carrot" to 500000,
        "minecraft:potato" to 500000,
        "minecraft:beetroot" to 500000,
        "minecraft:melon" to 500000,
        "minecraft:pumpkin" to 500000,

        "minecraft:cactus" to 300000,
        "minecraft:sugar_cane" to 500000,
        "minecraft:lily_pad" to 200000,
        "minecraft:vine" to 200000,

        // ========== 有机物-花卉（贵）==========
        "minecraft:dandelion" to 150000,
        "minecraft:poppy" to 150000,
        "minecraft:blue_orchid" to 150000,
        "minecraft:allium" to 150000,
        "minecraft:azure_bluet" to 150000,
        "minecraft:red_tulip" to 150000,
        "minecraft:orange_tulip" to 150000,
        "minecraft:white_tulip" to 150000,
        "minecraft:pink_tulip" to 150000,
        "minecraft:oxeye_daisy" to 150000,
        "minecraft:sunflower" to 150000,
        "minecraft:lilac" to 150000,
        "minecraft:rose_bush" to 150000,
        "minecraft:peony" to 150000,

        // ========== 有机物-食物（极贵）==========
        "minecraft:apple" to 800000,
        "minecraft:bread" to 1000000,
        "minecraft:cookie" to 500000,
        "minecraft:cake" to 2000000,
        "minecraft:golden_apple" to 5000000,
        "minecraft:enchanted_golden_apple" to 50000000,
        "minecraft:brown_mushroom" to 400000,
        "minecraft:red_mushroom" to 400000,

        // ========== 有机物-生物掉落 ==========
        "minecraft:bone" to 200000,
        "minecraft:rotten_flesh" to 200000,
        "minecraft:spider_eye" to 100000,
        "minecraft:string" to 50000,
        "minecraft:leather" to 300000,
        "minecraft:feather" to 300000,
        "minecraft:ender_pearl" to 500000,
        "minecraft:slime_ball" to 10000,
        "minecraft:slime_block" to 90000,
        "minecraft:gunpowder" to 50000,

        // ========== 羊毛 ==========
        "minecraft:black_wool" to 200000,

        // ========== 其他有机物 ==========
        "minecraft:stick" to 500000,
        "minecraft:torch" to 100000,

        // ========== 功能方块 ==========
        "minecraft:crafting_table" to 100,
        "minecraft:chest" to 200,
        "minecraft:furnace" to 150,
        "minecraft:brewing_stand" to 1000,
        "minecraft:bookshelf" to 500,
        "minecraft:ladder" to 80,
        "minecraft:rail" to 50000,

        // ========== 特殊物品 ==========
        "minecraft:book" to 500000,
        "minecraft:bucket" to 200,
        "minecraft:tnt" to 10000,
        "minecraft:name_tag" to 25000000,
        "minecraft:saddle" to 27000000,
        "minecraft:iron_horse_armor" to 100000,
        "minecraft:golden_horse_armor" to 250000,
        "minecraft:diamond_horse_armor" to 500000,
        "minecraft:music_disc_13" to 35000000,
        "minecraft:music_disc_cat" to 39000000
    )
}
