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
import org.junit.jupiter.api.Test

/**
 * 核反应堆红石中继回归测试。
 *
 * 背景：反应堆 EU 模式此前只读中心方块自身的 isReceivingRedstonePower，
 * 红石块贴在反应仓上无效。修复后：中心反应堆或任一相邻反应仓收到红石
 * 信号即可激活反应堆（与 IC2 原版 TileEntityReactorChamberElectric 的
 * OR 语义一致）。
 */
@McDebugTest
class ReactorRedstoneTest {

    private val REACTOR = "ic2_120:nuclear_reactor"
    private val CHAMBER = "ic2_120:reactor_chamber"

    /** 正向：红石块只贴反应仓（距中心 2 格），反应堆应被激活并产出 EU。 */
    @Test
    fun redstoneRelayViaChamber(ctx: TestContext) {
        place(ctx, ctx.origin, REACTOR)
        place(ctx, ctx.pos(1, 0, 0), CHAMBER)
        // 红石块在仓东侧：与中心反应堆不相邻，只可能通过仓的中继生效
        setBlocks(ctx, listOf(ctx.pos(2, 0, 0) to "minecraft:redstone_block"))
        setSlot(ctx, ctx.origin, 0, "ic2_120:uranium_fuel_rod", 1)
        waitUntil(ctx, beFieldGreaterThan(ctx.origin, "EnergyStored", 0), 100)
    }

    /** 负向：有反应仓但无红石信号时，反应堆必须保持停机。 */
    @Test
    fun chamberWithoutSignalStaysOff(ctx: TestContext) {
        place(ctx, ctx.origin, REACTOR)
        place(ctx, ctx.pos(1, 0, 0), CHAMBER)
        setSlot(ctx, ctx.origin, 0, "ic2_120:uranium_fuel_rod", 1)
        waitTicks(ctx, 60)
        val energy = getBeNumber(ctx, ctx.origin, "EnergyStored")
        if (energy != 0.0) throw AssertionError("expected 0 energy without redstone signal, got $energy")
    }
}
