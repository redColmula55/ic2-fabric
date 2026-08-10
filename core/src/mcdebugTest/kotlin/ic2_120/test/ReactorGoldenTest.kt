package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.beFieldGreaterThan
import com.mcdebug.runner.getBeNumber
import com.mcdebug.runner.place
import com.mcdebug.runner.setBlocks
import com.mcdebug.runner.setSlot
import com.mcdebug.runner.waitTicks
import com.mcdebug.runner.waitUntil
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 核反应堆 golden 数据采集 + 基本断言。
 *
 * 原始 TS 版只输出 GOLDEN 行（供 JS 模拟器对照）；Kotlin 版保留 GOLDEN
 * 输出（println）并增加基本断言：EU 产出 > 0、堆温合理、槽位 use 值可读。
 * 布局 slot index = x*9+y。
 */
@McDebugTest
class ReactorGoldenTest {

    private val REACTOR = "ic2_120:nuclear_reactor"

    /** 放反应堆 + 相邻红石块（启动）→ 填槽 → 等 EU 产出 + 稳定 → 采集状态。 */
    private fun readReactor(ctx: TestContext, label: String, slotsToFill: List<Pair<Int, String>>, expectHeat: Boolean = true) {
        place(ctx, ctx.origin, REACTOR)
        setBlocks(ctx, listOf(ctx.pos(1, 0, 0) to "minecraft:redstone_block"))
        for ((slot, item) in slotsToFill) {
            setSlot(ctx, ctx.origin, slot, item, 1)
        }
        // 等到产出 EU（电模式）；最多 80 tick（4 cycle）
        waitUntil(ctx, beFieldGreaterThan(ctx.origin, "EnergyStored", 0), 80)
        // 再多等一个完整 cycle 让数值稳定
        waitTicks(ctx, 20)

        val heat = getBeNumber(ctx, ctx.origin, "HeatStored")
        val energy = getBeNumber(ctx, ctx.origin, "EnergyStored")

        // 读取每个非空槽的 use 值
        val slotUses = mutableMapOf<Int, String>()
        for ((slotIdx, _) in slotsToFill) {
            try {
                val stack = ctx.api.inv.getSlot(ctx.origin, slotIdx).asJsonObject.get("slot").asJsonObject
                val item = stack.get("item").takeIf { it != null && !it.isJsonNull }?.asString ?: "(empty)"
                val nbt = stack.get("nbt")?.takeIf { it != null && !it.isJsonNull }?.asJsonObject
                val use = nbt?.get("use")?.asLong ?: nbt?.get("Use")?.asLong ?: -1
                slotUses[slotIdx] = "$item/use=$use"
            } catch (_: Exception) {
                slotUses[slotIdx] = "(empty/burned)/use=-1"
            }
        }

        println("GOLDEN\t$label\theat=$heat\tenergy=$energy\tslots=$slotUses")

        // 基本断言：EU 产出是硬要求；堆温仅对"无散热布局"断言
        // （heat_vent/coolant_cell 会吸走堆热，堆温 0 是散热片正常工作）。
        assertTrue(energy > 0, "reactor should produce EU for layout $label")
        if (expectHeat) assertTrue(heat > 0, "reactor should generate heat for layout $label")
    }

    private fun rod(slot: Int) = slot to "ic2_120:uranium_fuel_rod"

    @Test
    fun goldenSingleUraniumRod(ctx: TestContext) = readReactor(ctx, "single_uranium_rod_slot0", listOf(rod(0)))

    @Test
    fun goldenTwoAdjacentUraniumRods(ctx: TestContext) =
        readReactor(ctx, "two_adjacent_uranium_rods", listOf(rod(0), rod(1)))

    @Test
    fun goldenFourStackedUraniumRods(ctx: TestContext) =
        readReactor(ctx, "four_stacked_uranium_rods", listOf(rod(0), rod(1), rod(2), rod(3)))

    @Test
    fun goldenUraniumRodPlusHeatVent(ctx: TestContext) =
        readReactor(ctx, "uranium_rod_plus_heat_vent", listOf(rod(0), 1 to "ic2_120:heat_vent"), expectHeat = false)

    @Test
    fun goldenDualUraniumRod(ctx: TestContext) =
        readReactor(ctx, "dual_uranium_rod", listOf(rod(0), 1 to "ic2_120:dual_uranium_fuel_rod"))

    @Test
    fun goldenQuadUraniumRod(ctx: TestContext) =
        readReactor(ctx, "quad_uranium_rod", listOf(rod(0), 1 to "ic2_120:quad_uranium_fuel_rod"))

    @Test
    fun goldenUraniumRodPlusNeutronReflector(ctx: TestContext) =
        readReactor(ctx, "uranium_rod_plus_neutron_reflector", listOf(rod(0), 1 to "ic2_120:neutron_reflector"))

    @Test
    fun goldenUraniumRodPlus10kCoolantCell(ctx: TestContext) =
        readReactor(ctx, "uranium_rod_plus_10k_coolant_cell", listOf(rod(0), 1 to "ic2_120:reactor_coolant_cell"), expectHeat = false)

    @Test
    fun goldenEmptySanity(ctx: TestContext) {
        place(ctx, ctx.origin, REACTOR)
        setBlocks(ctx, listOf(ctx.pos(1, 0, 0) to "minecraft:redstone_block"))
        waitTicks(ctx, 30)
        val energy = getBeNumber(ctx, ctx.origin, "EnergyStored")
        if (energy != 0.0) throw AssertionError("empty reactor must not produce EU, got $energy")
    }
}
