package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.getBeNumber
import com.mcdebug.runner.setBeField
import com.mcdebug.runner.waitTicks
import org.junit.jupiter.api.Test

/**
 * 变压器全矩阵诊断：facing(4) × mode(2) × 输入侧有电(2)。
 *
 * 物理布局固定：west = BatBox（低压 1 级），east = CESU（高压 3 级）。
 * 正面（facing 方向）是高级面（2 级），其他五面是低级面（1 级）。
 *
 * 期望：
 * - STEP_UP + facing=east（正面朝东对着 CESU）+ 低压侧有电：BatBox 32→变压器积累→正面 128 输出→CESU 涨
 * - STEP_DOWN + facing=east + CESU 有电：CESU 128→正面输入→非正面 32×4 输出→BatBox 涨
 * - 其余组合要么方向反、要么正面悬空、要么没电 → 目标电池不变
 */
@McDebugTest
class TransformerMatrixTest {

    private fun powerSide(ctx: TestContext, pos: List<Int>, tier: Int, powered: Boolean) {
        if (powered) setBeField(ctx, pos, "EnergyStored", if (tier == 1) 40_000.0 else 300_000.0)
    }

    private fun dump(ctx: TestContext, name: String, facing: String, mode: Int, powered: Boolean) {
        val bat = getBeNumber(ctx, ctx.pos(-1, 0, 0), "EnergyStored").toLong()
        val cesu = getBeNumber(ctx, ctx.pos(1, 0, 0), "EnergyStored").toLong()
        val tr = getBeNumber(ctx, ctx.origin, "EnergyStored").toLong()
        println("MATRIX\t$name\tfacing=$facing\tmode=$mode\tpowered=$powered\tbat=$bat\tcesu=$cesu\ttransformer=$tr")
    }

    private fun runCase(
        ctx: TestContext,
        name: String,
        facing: String,
        mode: Int, // 0=STEP_DOWN 1=STEP_UP
        powered: Boolean,
    ): Pair<Double, Double> {
        ctx.api.world.clearBox(
            mapOf(
                "from" to listOf(ctx.origin[0] - 3, ctx.origin[1] - 1, ctx.origin[2] - 3),
                "to" to listOf(ctx.origin[0] + 3, ctx.origin[1] + 1, ctx.origin[2] + 3),
            ),
            maxBlocks = 256,
        )
        // 电池只从 facing 面输出（getSideMaxExtract: side==facing），其余五面接收。
        // 升压：BatBox 输出朝变压器(east)、CESU 从变压器接收(west 面 → facing≠west=east)；
        // 降压：CESU 输出朝变压器(west)、BatBox 从变压器接收(east 面 → facing≠east=west)。
        val batFacing = if (mode == 1) "east" else "west"
        val cesuFacing = if (mode == 1) "east" else "west"
        ctx.api.world.setBlock(ctx.pos(-1, 0, 0), "ic2_120:batbox", mapOf("facing" to batFacing))
        ctx.api.world.setBlock(ctx.pos(1, 0, 0), "ic2_120:cesu", mapOf("facing" to cesuFacing))
        ctx.api.world.setBlock(ctx.origin, "ic2_120:lv_transformer", mapOf("facing" to facing))

        setBeField(ctx, ctx.origin, "Mode", mode.toDouble())
        // 给电源侧预充（模拟"有电"），否则两个电池都是空的
        if (mode == 1) powerSide(ctx, ctx.pos(-1, 0, 0), 1, powered) // 升压：低压侧(BatBox)供电
        else powerSide(ctx, ctx.pos(1, 0, 0), 3, powered) // 降压：高压侧(CESU)供电

        val bat0 = getBeNumber(ctx, ctx.pos(-1, 0, 0), "EnergyStored")
        val cesu0 = getBeNumber(ctx, ctx.pos(1, 0, 0), "EnergyStored")

        waitTicks(ctx, 40) // 2 秒自然 tick，全部方块走真实 ticker

        val bat1 = getBeNumber(ctx, ctx.pos(-1, 0, 0), "EnergyStored")
        val cesu1 = getBeNumber(ctx, ctx.pos(1, 0, 0), "EnergyStored")
        dump(ctx, name, facing, mode, powered)

        return (bat1 - bat0) to (cesu1 - cesu0)
    }

    /** 升压：BatBox(west, 32) → 变压器 → 正面 → CESU(east, 128)。期望 facing=east 时 CESU 涨。 */
    @Test
    fun stepUpMatrix(ctx: TestContext) {
        var ok = 0
        for (facing in listOf("east", "west", "north", "south")) {
            for (powered in listOf(true, false)) {
                val name = "stepUp_${facing}_${if (powered) "powered" else "empty"}"
                val (batDelta, cesuDelta) = runCase(ctx, name, facing, 1, powered)
                if (facing == "east" && powered) {
                    if (cesuDelta > 0 && batDelta < 0) {
                        ok++
                    } else throw AssertionError("$name: 期望 CESU 涨(BatBox 供电)，实际 bat=$batDelta cesu=$cesuDelta")
                } else {
                    // 非正确组合：CESU 绝不能涨（正面悬空时 BatBox 流入变压器缓冲是合法行为，
                    // 但能量无法到达 CESU）
                    if (cesuDelta != 0.0) throw AssertionError("$name: 期望 CESU 不涨，实际 bat=$batDelta cesu=$cesuDelta")
                }
            }
        }
        println("stepUpMatrix: $ok/1 correct-direction cases flowed")
    }

    /** 降压：CESU(east, 128) → 变压器正面 → 非正面 → BatBox(west, 32)。期望 facing=east 时 BatBox 涨。 */
    @Test
    fun stepDownMatrix(ctx: TestContext) {
        var ok = 0
        for (facing in listOf("east", "west", "north", "south")) {
            for (powered in listOf(true, false)) {
                val name = "stepDown_${facing}_${if (powered) "powered" else "empty"}"
                val (batDelta, cesuDelta) = runCase(ctx, name, facing, 0, powered)
                if (facing == "east" && powered) {
                    if (batDelta > 0 && cesuDelta < 0) {
                        ok++
                    } else throw AssertionError("$name: 期望 BatBox 涨(CESU 供电)，实际 bat=$batDelta cesu=$cesuDelta")
                } else {
                    val target = if (facing == "east" && !powered) batDelta else cesuDelta
                    if (target != 0.0) throw AssertionError("$name: 期望目标电池不动，实际 bat=$batDelta cesu=$cesuDelta")
                }
            }
        }
        println("stepDownMatrix: $ok/1 correct-direction cases flowed")
    }

    /** 对照：不带 facing 的 setblock 默认朝向（模拟之前手动测试）→ 高压面悬空，能量不流。 */
    @Test
    fun defaultFacingDoesNotFlow(ctx: TestContext) {
        ctx.api.world.clearBox(
            mapOf(
                "from" to listOf(ctx.origin[0] - 3, ctx.origin[1] - 1, ctx.origin[2] - 3),
                "to" to listOf(ctx.origin[0] + 3, ctx.origin[1] + 1, ctx.origin[2] + 3),
            ),
            maxBlocks = 256,
        )
        ctx.api.world.setBlock(ctx.pos(-1, 0, 0), "ic2_120:batbox", mapOf("facing" to "east"))
        ctx.api.world.setBlock(ctx.pos(1, 0, 0), "ic2_120:cesu", mapOf("facing" to "east"))
        ctx.api.server.runCommand("setblock ${ctx.origin[0]} ${ctx.origin[1]} ${ctx.origin[2]} ic2_120:lv_transformer")
        setBeField(ctx, ctx.origin, "Mode", 1.0)
        setBeField(ctx, ctx.pos(-1, 0, 0), "EnergyStored", 3200.0)

        val cesu0 = getBeNumber(ctx, ctx.pos(1, 0, 0), "EnergyStored")
        waitTicks(ctx, 40)
        val cesu1 = getBeNumber(ctx, ctx.pos(1, 0, 0), "EnergyStored")
        val facing = ctx.api.world.getBlock(ctx.origin).asJsonObject.get("state").asJsonObject.get("props").asJsonObject.get("facing").asString
        println("defaultFacing test: transformer facing=$facing, cesu delta=${cesu1 - cesu0}")
        if (cesu1 > cesu0) throw AssertionError("默认朝向(=$facing)下 CESU 竟然涨了——默认朝向不是悬空？")
    }
}
