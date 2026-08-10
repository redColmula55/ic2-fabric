package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.assertBlockId
import com.mcdebug.runner.getBeNumber
import com.mcdebug.runner.setBeField
import com.mcdebug.runner.setBlocks
import com.mcdebug.runner.waitTicks
import org.junit.jupiter.api.Test

/**
 * 变压器 (Transformer) 测试。
 * 布局：west=BatBox(LV) — origin=变压器 — east=CESU(MV)。
 * Mode：1=升压(STEP_UP), 0=降压(STEP_DOWN)。
 * 验证 100 tick 内能量从输入端流向输出端。
 */
@McDebugTest
class TransformerTest {

    private fun setupTransformer(ctx: TestContext, mode: Int, batboxEnergy: Int, cesuEnergy: Int) {
        val west = ctx.pos(-1, 0, 0)
        val east = ctx.pos(1, 0, 0)
        // 电池只从 facing 面输出（getSideMaxExtract: side==facing），其余五面接收。
        // 升压：BatBox 输出朝变压器(east)；CESU 从 west 面接收 → facing≠west=east。
        // 降压：CESU 输出朝变压器(west)；BatBox 从 east 面接收 → facing≠east=west。
        val batFacing = if (mode == 1) "east" else "west"
        val cesuFacing = if (mode == 1) "east" else "west"
        ctx.api.world.setBlock(west, "ic2_120:batbox", mapOf("facing" to batFacing))
        ctx.api.world.setBlock(ctx.origin, "ic2_120:lv_transformer", mapOf("facing" to "east"))
        ctx.api.world.setBlock(east, "ic2_120:cesu", mapOf("facing" to cesuFacing))
        setBeField(ctx, west, "EnergyStored", batboxEnergy)
        setBeField(ctx, east, "EnergyStored", cesuEnergy)
        setBeField(ctx, ctx.origin, "Mode", mode)
    }

    /**
     * 升压：BatBox(LV) → 变压器 → CESU(MV)，等 100 tick 后 CESU 应有能量流入。
     *
     * 曾误判为"重构回归"（当时 CESU facing=west：升压时 CESU 需从 west 面接收，
     * 而 facing=输出面 → west==facing → insert=0，能量卡在变压器缓冲）。真相：
     * 电池按 facing 单向输出，其余五面接收——用 TransformerMatrixTest 全矩阵
     * 验证后确认变压器本身无 bug，此测试按正确朝向恢复。
     */
    @Test
    fun stepUpLvToMv(ctx: TestContext) {
        setupTransformer(ctx, mode = 1, batboxEnergy = 10000, cesuEnergy = 0)
        val east = ctx.pos(1, 0, 0)
        val start = getBeNumber(ctx, east, "EnergyStored")
        waitTicks(ctx, 100)
        val end = getBeNumber(ctx, east, "EnergyStored")
        if (end <= start) throw AssertionError("expected CESU to gain energy, before=$start after=$end")
    }

    /** 降压：CESU(MV) → 变压器 → BatBox(LV)，等 100 tick 后 BatBox 应有能量流入。 */
    @Test
    fun stepDownMvToLv(ctx: TestContext) {
        setupTransformer(ctx, mode = 0, batboxEnergy = 0, cesuEnergy = 10000)
        val west = ctx.pos(-1, 0, 0)
        val start = getBeNumber(ctx, west, "EnergyStored")
        waitTicks(ctx, 100)
        val end = getBeNumber(ctx, west, "EnergyStored")
        if (end <= start) throw AssertionError("expected BatBox to gain energy, before=$start after=$end")
    }

    /** 三种变压器都能放置。 */
    @Test
    fun placeLvMvHvEv(ctx: TestContext) {
        val x = ctx.origin[0]
        for ((i, id) in listOf("ic2_120:lv_transformer", "ic2_120:mv_transformer", "ic2_120:hv_transformer", "ic2_120:ev_transformer").withIndex()) {
            com.mcdebug.runner.place(ctx, listOf(x + i, ctx.origin[1], ctx.origin[2]), id)
            assertBlockId(ctx, listOf(x + i, ctx.origin[1], ctx.origin[2]), id)
        }
    }
}
