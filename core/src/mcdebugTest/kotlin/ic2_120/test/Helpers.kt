package ic2_120.test

import com.mcdebug.cli.Pos
import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.place
import com.mcdebug.runner.setBeField
import com.mcdebug.runner.setBlocks

/** 待测机器 origin 的东侧一格 — [setupAdjacentBatbox] 默认放置 BatBox 的位置。 */
val TestContext.batboxEast: Pos get() = pos(1, 0, 0)

/**
 * 搭建最常见的"被测机器 + BatBox 供电"布局：
 *   1. 在 origin 东侧一格放一个面朝西的 BatBox（LV 输出侧贴向机器）；
 *   2. 预充 40 000 EU，足够所有调用本 helper 的机器跑完一次完整配方；
 *   3. 在 origin 放置目标机器。
 *
 * 机器 id 与 BatBox 位置都可参数化：若被测机器的能量接口不在西面，可传入
 * 自定义坐标并自行调整 `facing`。
 */
fun setupAdjacentBatbox(ctx: TestContext, machine: String, batbox: Pos = ctx.batboxEast) {
    setBlocks(ctx, listOf(batbox to "ic2_120:batbox"), mapOf("facing" to "west"))
    setBeField(ctx, batbox, "EnergyStored", 40000)
    place(ctx, ctx.origin, machine)
}

/**
 * 把 32 位热量值写入 BE 拆分的高低 16 位字段。
 *
 * 热量系统用两个 16 位字段 (`Heat_Low` / `Heat_High`) 拼成 32 位累加器，
 * 是为了把 BE 序列化出来的 NBT 体积拆小。需要把热量预设到阈值以上的用例
 * 调用本方法一次后，调用方一般会再加 +200，保证阈值安全越过。
 */
fun setHeat(ctx: TestContext, value: Int) {
    setBeField(ctx, ctx.origin, "Heat_Low", value and 0xffff)
    setBeField(ctx, ctx.origin, "Heat_High", (value ushr 16) and 0xffff)
}
