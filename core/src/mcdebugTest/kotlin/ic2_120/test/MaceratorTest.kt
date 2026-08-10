package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.assertBlockId
import com.mcdebug.runner.assertSlotCount
import com.mcdebug.runner.assertSlotEmpty
import com.mcdebug.runner.assertSlotHas
import com.mcdebug.runner.insertItem
import com.mcdebug.runner.invItemEquals
import com.mcdebug.runner.place
import com.mcdebug.runner.placeAsPlayer
import com.mcdebug.runner.setBeField
import com.mcdebug.runner.setBlocks
import com.mcdebug.runner.setSlot
import com.mcdebug.runner.traceBoxAround
import com.mcdebug.runner.waitTicks
import com.mcdebug.runner.waitUntil
import com.mcdebug.runner.withTrace
import org.junit.jupiter.api.Test

/**
 * 粉碎机 (Macerator) 测试。
 *
 * 粉碎机有两条供电路径需要分别覆盖：
 *   1. 通过**线缆**中转（中间隔一格绝缘铜缆），覆盖电缆连接 + 能量传输链路；
 *   2. 直接相邻 BatBox，覆盖面贴接供电。
 *
 * 槽位布局（与 `MaceratorBlockEntity` 保持一致）：
 *   slot 0  = 输入 (SLOT_INPUT)
 *   slot 1  = 输出 (SLOT_OUTPUT)
 *   slot 2  = 放电槽 (SLOT_DISCHARGING)
 *   slot 3+ = 升级槽 (SLOT_UPGRADE_0..3)
 */
@McDebugTest
class MaceratorTest {

    /**
     * 把"等待型"用例包在 trace 里：成功就静默通过；失败时 [withTrace] 会把
     * 逐 tick 的库存/BE NBT 变化附在错误消息上，便于定位是"没供电 / 没配方 /
     * 输出阻塞"哪一种。trace 范围只覆盖 origin 一格（机器本体），
     * intervalTicks=10 在 15s（300t）窗口里大约抓 30 帧。
     *
     * ⚠️ 不适用于"机器最终会消失"的用例（如过压爆炸）：trace 收尾快照时
     * 方块已变 air（无 BE）会抛 `no block entity`。爆炸类用例直接用 [waitUntil]。
     */
    private fun tracedWait(ctx: TestContext, predicate: String, timeoutTicks: Int) {
        withTrace(ctx, mapOf("box" to traceBoxAround(ctx.origin, 0), "intervalTicks" to 10)) {
            waitUntil(ctx, predicate, timeoutTicks)
        }
    }

    /**
     * 标准搭建：东二格 BatBox + 东一格绝缘铜缆（让玩家放置以正确生成 facing），
     * 然后放置粉碎机。线缆中转用于验证能量网络能跨方块传导。
     */
    private fun setupMacerator(ctx: TestContext) {
        val batbox = ctx.pos(2, 0, 0)
        val cable = ctx.pos(1, 0, 0)
        setBlocks(ctx, listOf(batbox to "ic2_120:batbox"), mapOf("facing" to "west"))
        // 电缆用 setBlocks 放置（不走 placeAsPlayer）：点击 IC2 机器（batbox）时
        // interactBlock 管线会先触发机器 onUse 打开 GUI → 放置被跳过；而点击
        // air（可替换方块）时 MC 会把方块放到 neighbor 上。setBlocks 直接写方块，
        // 连接状态由 neighborUpdate 自动处理（实测 west:true 正常）。
        setBlocks(ctx, listOf(cable to "ic2_120:insulated_copper_cable"))
        setBeField(ctx, batbox, "EnergyStored", 40000)
        place(ctx, ctx.origin, "ic2_120:macerator")
    }

    @Test
    fun `place`(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:macerator")
        assertBlockId(ctx, ctx.origin, "ic2_120:macerator")
    }

    /** 经典配方：1 圆石 → 1 砾石（经电缆供电）。 */
    @Test
    fun cobblestoneToGravelWithCable(ctx: TestContext) {
        setupMacerator(ctx)
        insertItem(ctx, ctx.origin, "minecraft:cobblestone", 1, 0)
        tracedWait(ctx, invItemEquals(ctx.origin, 1, "minecraft:gravel"), 15 * 20)
        assertSlotHas(ctx, ctx.origin, 1, "minecraft:gravel")
    }

    /** 1 煤炭块 → 9 煤粉（带数量断言）。 */
    @Test
    fun coalBlockTo9CoalDust(ctx: TestContext) {
        setupMacerator(ctx)
        insertItem(ctx, ctx.origin, "minecraft:coal_block", 1, 0)
        tracedWait(ctx, invItemEquals(ctx.origin, 1, "ic2_120:coal_dust"), 15 * 20)
        assertSlotCount(ctx, ctx.origin, 1, 9)
    }

    /** 8 西瓜片 → 1 生物质渣（带数量断言）。 */
    @Test
    fun melonSlicesToBioChaff(ctx: TestContext) {
        setupMacerator(ctx)
        insertItem(ctx, ctx.origin, "minecraft:melon_slice", 8, 0)
        tracedWait(ctx, invItemEquals(ctx.origin, 1, "ic2_120:bio_chaff"), 15 * 20)
        assertSlotCount(ctx, ctx.origin, 1, 1)
    }

    /** 1 铁矿石 → 2 粉碎铁（验证富产系数）。 */
    @Test
    fun ironOreTo2CrushedIron(ctx: TestContext) {
        setupMacerator(ctx)
        insertItem(ctx, ctx.origin, "minecraft:iron_ore", 1, 0)
        tracedWait(ctx, invItemEquals(ctx.origin, 1, "ic2_120:crushed_iron"), 15 * 20)
        assertSlotCount(ctx, ctx.origin, 1, 2)
    }

    /** 无电闲置。 */
    @Test
    fun noPowerIdle(ctx: TestContext) {
        place(ctx, ctx.origin, "ic2_120:macerator")
        insertItem(ctx, ctx.origin, "minecraft:cobblestone", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:cobblestone")
        assertSlotEmpty(ctx, ctx.origin, 1)
    }

    /** 非法输入。 */
    @Test
    fun invalidInputDirt(ctx: TestContext) {
        setupMacerator(ctx)
        insertItem(ctx, ctx.origin, "minecraft:dirt", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:dirt")
        assertSlotEmpty(ctx, ctx.origin, 1)
    }

    /** 输出满 → 阻塞。 */
    @Test
    fun outputFullBlocksNext(ctx: TestContext) {
        setupMacerator(ctx)
        setSlot(ctx, ctx.origin, 1, "minecraft:gravel", 64)
        insertItem(ctx, ctx.origin, "minecraft:cobblestone", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:cobblestone")
    }

    /** 能量不足 1 轮 → 应当一整轮都不消耗输入（不能半成品消耗）。 */
    @Test
    fun energyStarveNoPartialConsume(ctx: TestContext) {
        val batbox = ctx.pos(2, 0, 0)
        val cable = ctx.pos(1, 0, 0)
        setBlocks(ctx, listOf(batbox to "ic2_120:batbox"), mapOf("facing" to "west"))
        setBlocks(ctx, listOf(cable to "ic2_120:insulated_copper_cable"))
        setBeField(ctx, batbox, "EnergyStored", 1)
        place(ctx, ctx.origin, "ic2_120:macerator")
        insertItem(ctx, ctx.origin, "minecraft:cobblestone", 1, 0)
        waitTicks(ctx, 200)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:cobblestone")
    }

    /** 直接相邻 BatBox 也能正常工作（覆盖面贴接供电链路）。 */
    @Test
    fun powerAdjacentBatbox(ctx: TestContext) {
        val batbox = ctx.batboxEast
        setBlocks(ctx, listOf(batbox to "ic2_120:batbox"), mapOf("facing" to "west"))
        setBeField(ctx, batbox, "EnergyStored", 40000)
        place(ctx, ctx.origin, "ic2_120:macerator")
        insertItem(ctx, ctx.origin, "minecraft:cobblestone", 1, 0)
        tracedWait(ctx, invItemEquals(ctx.origin, 1, "minecraft:gravel"), 15 * 20)
        assertSlotHas(ctx, ctx.origin, 1, "minecraft:gravel")
    }

    /**
     * 过压爆炸：把粉碎机接在满电的 MFSU（HV）下，应立刻自爆成空气。
     * 注意：机器放下即炸（mfsu 40M EU 过压倍率极高，place 返回时已变 air），
     * **不能 insertItem**（会撞上爆炸后的 air → no block entity）。
     * 不能用 tracedWait —— trace 收尾帧同样会撞 air。
     */
    @Test
    fun overvoltageHvExplode(ctx: TestContext) {
        val mfsu = ctx.batboxEast
        setBlocks(ctx, listOf(mfsu to "ic2_120:mfsu"), mapOf("facing" to "west"))
        setBeField(ctx, mfsu, "EnergyStored", 40_000_000)
        place(ctx, ctx.origin, "ic2_120:macerator")
        waitUntil(
            ctx,
            "block[${ctx.origin[0]},${ctx.origin[1]},${ctx.origin[2]}].id == \"minecraft:air\"",
            15 * 20,
        )
        assertBlockId(ctx, ctx.origin, "minecraft:air")
    }

    /**
     * 多对一配方缓存回归：先放 1 个（不足 8 个），让机器带着"数量不足"状态
     * 跑若干 tick，再补足到 8 个。若实现按"刚放入一个"缓存了 null 结果，
     * 补足后机器也不会开始加工——这是本用例要防止的回归。
     */
    @Test
    fun multiInputPartialThenFill(ctx: TestContext) {
        setupMacerator(ctx)
        // 先放 1 片西瓜（配方需 8 片），让机器空转 20 tick（足够触发多次配方判定）。
        insertItem(ctx, ctx.origin, "minecraft:melon_slice", 1, 0)
        waitTicks(ctx, 20)
        assertSlotHas(ctx, ctx.origin, 0, "minecraft:melon_slice")
        // 补足到 8 片后应当正常开始加工。
        insertItem(ctx, ctx.origin, "minecraft:melon_slice", 7, 0)
        tracedWait(ctx, invItemEquals(ctx.origin, 1, "ic2_120:bio_chaff"), 15 * 20)
        assertSlotCount(ctx, ctx.origin, 1, 1)
    }
}
