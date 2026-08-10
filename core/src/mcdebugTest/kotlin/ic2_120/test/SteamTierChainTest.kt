package ic2_120.test

import com.mcdebug.runner.McDebugTest
import com.mcdebug.runner.TestContext
import com.mcdebug.runner.getBeNumber
import com.mcdebug.runner.place
import com.mcdebug.runner.setBeField
import com.mcdebug.runner.setBlocks
import com.mcdebug.runner.setSlot
import com.mcdebug.runner.waitTicks
import org.junit.jupiter.api.Test

/**
 * 蒸汽梯次利用链条测试。
 *
 * 锅炉(过热蒸汽) → 轮机#1(4KU/mB) → 普通蒸汽 → 轮机#2(2KU/mB) → 冷凝机 → 蒸馏水回流。
 *
 * 取巧：锅炉 SystemHeatMilli 直接拉到 400000（过热阈值 374°C），跳过电加热机升温。
 * 依赖自然 tick（蒸汽传递/流体调度/动能转换），用固定等待 + 采样验证。
 */
@McDebugTest
class SteamTierChainTest {

    /** origin 方向偏移的语义化布局（对齐 TS 版 layout）。 */
    private fun layout(ctx: TestContext): Map<String, List<Int>> {
        val o = ctx.origin
        return mapOf(
            "boiler" to o,
            "turbine1" to ctx.pos(1, 0, 0),
            "turbine2" to ctx.pos(2, 0, 0),
            "condenser" to ctx.pos(3, 0, 0),
            "heatGen1" to ctx.pos(-1, 0, 0),
            "heatGen1Power" to ctx.pos(-2, 0, 0),
            "heatGen1Redstone" to ctx.pos(-1, 1, 0),
            "heatGen2" to ctx.pos(0, 0, -1),
            "heatGen2Power" to ctx.pos(-1, 0, -2),
            "heatGen2Redstone" to ctx.pos(0, 1, -1),
            "condenserPower" to ctx.pos(3, 0, -1),
            "kineticGen1" to ctx.pos(1, 0, -1),
            "kineticGen2" to ctx.pos(2, 0, -1),
            "mfsu" to ctx.pos(1, 0, -2),
            "cable" to ctx.pos(2, 0, -2),
            "pumpAtt" to ctx.pos(3, 0, 1),
            "pumpAtt2" to ctx.pos(2, 0, 1),
            "pipe0" to ctx.pos(3, 0, 2),
            "pipe1" to ctx.pos(2, 0, 2),
            "pipe2" to ctx.pos(1, 0, 2),
            "pipe3" to ctx.pos(0, 0, 2),
            "endPipe" to ctx.pos(0, 0, 1),
        )
    }

    /** 搭建完整梯次链（对齐 TS 版 setupTierChain）。 */
    private fun setupTierChain(ctx: TestContext) {
        val p = layout(ctx)
        val srv = ctx.api.server

        // 1. 热源：电加热机 + 创造发电机 + 红石块
        setBlocks(
            ctx,
            listOf(
                p["heatGen1Power"]!! to "ic2_120:creative_generator",
                p["heatGen1Redstone"]!! to "minecraft:redstone_block",
                p["heatGen2Power"]!! to "ic2_120:creative_generator",
                p["heatGen2Redstone"]!! to "minecraft:redstone_block",
            ),
        )
        srv.runCommand("setblock ${p["heatGen1"]!![0]} ${p["heatGen1"]!![1]} ${p["heatGen1"]!![2]} ic2_120:electric_heat_generator[facing=east]")
        srv.runCommand("setblock ${p["heatGen2"]!![0]} ${p["heatGen2"]!![1]} ${p["heatGen2"]!![2]} ic2_120:electric_heat_generator[facing=south]")
        for (hg in listOf(p["heatGen1"]!!, p["heatGen2"]!!)) {
            for (s in 0..9) setSlot(ctx, hg, s, "ic2_120:coil", 1)
            setBeField(ctx, hg, "EnergyStored", 10000)
        }

        // 2. 锅炉（温度直拉 400°C + 满蒸馏水）
        srv.runCommand("setblock ${p["boiler"]!![0]} ${p["boiler"]!![1]} ${p["boiler"]!![2]} ic2_120:steam_generator[facing=west]")
        setBeField(ctx, p["boiler"]!!, "SystemHeatMilli", 400_000)
        setBeField(ctx, p["boiler"]!!, "Pressure", 221)
        setBeField(ctx, p["boiler"]!!, "InputMB", 1)
        setBeField(ctx, p["boiler"]!!, "WaterTank.amount", 810_000)
        setBeField(ctx, p["boiler"]!!, "WaterTank.variant.fluid", "ic2_120:distilled_water")

        // 3. 两台轮机 + 涡轮
        place(ctx, p["turbine1"]!!, "ic2_120:steam_kinetic_generator")
        place(ctx, p["turbine2"]!!, "ic2_120:steam_kinetic_generator")
        setSlot(ctx, p["turbine1"]!!, 0, "ic2_120:steam_turbine", 1)
        setSlot(ctx, p["turbine2"]!!, 0, "ic2_120:steam_turbine", 1)

        // 4. 冷凝机 + 散热口 + 供电
        place(ctx, p["condenser"]!!, "ic2_120:condenser")
        for (s in 0..3) setSlot(ctx, p["condenser"]!!, s, "ic2_120:heat_vent", 1)
        setBeField(ctx, p["condenser"]!!, "EnergyStored", 100_000)
        setBlocks(ctx, listOf(p["condenserPower"]!! to "ic2_120:creative_generator"))

        // 5. 动能→电能：动能发电机(facing=south) 并联 MFSU
        srv.runCommand("setblock ${p["kineticGen1"]!![0]} ${p["kineticGen1"]!![1]} ${p["kineticGen1"]!![2]} ic2_120:kinetic_generator[facing=south]")
        srv.runCommand("setblock ${p["kineticGen2"]!![0]} ${p["kineticGen2"]!![1]} ${p["kineticGen2"]!![2]} ic2_120:kinetic_generator[facing=south]")
        setBlocks(ctx, listOf(p["mfsu"]!! to "ic2_120:mfsu"))
        srv.runCommand("setblock ${p["cable"]!![0]} ${p["cable"]!![1]} ${p["cable"]!![2]} ic2_120:glass_fibre_cable")

        // 6. 蒸馏水回流：泵附件 + 管道
        srv.runCommand("setblock ${p["pumpAtt"]!![0]} ${p["pumpAtt"]!![1]} ${p["pumpAtt"]!![2]} ic2_120:bronze_pump_attachment")
        srv.runCommand("setblock ${p["pumpAtt2"]!![0]} ${p["pumpAtt2"]!![1]} ${p["pumpAtt2"]!![2]} ic2_120:bronze_pump_attachment")
        for (pipe in listOf("pipe0", "pipe1", "pipe2", "pipe3", "endPipe")) {
            srv.runCommand("setblock ${p[pipe]!![0]} ${p[pipe]!![1]} ${p[pipe]!![2]} ic2_120:bronze_pipe_tiny")
        }

        waitTicks(ctx, 160) // 8 秒：电加热机启动 + 蒸汽传递 + 冷凝
    }

    /** 完整梯次链：过热蒸汽 → 轮机#1 → 轮机#2 → 冷凝 → 回流，EU 速率 ≈ 150 EU/t。 */
    @Test
    fun fullChain(ctx: TestContext) {
        val p = layout(ctx)
        setupTierChain(ctx)

        // 验证 1：锅炉在过热区
        val temp = getBeNumber(ctx, p["boiler"]!!, "SystemHeatMilli")
        if (temp < 374_000) throw AssertionError("锅炉未达过热区: SystemHeatMilli=$temp, 需要 >= 374000")

        // 验证 2：MFSU EU 上升速率 ≈ 150 EU/t（100mB/t × 6KU/mB ÷ 4KU/EU）
        val eu0 = getBeNumber(ctx, p["mfsu"]!!, "EnergyStored")
        waitTicks(ctx, 100)
        val eu1 = getBeNumber(ctx, p["mfsu"]!!, "EnergyStored")
        val euRate = (eu1 - eu0) / 100.0
        if (eu1 <= eu0) {
            val gen1Eu = getBeNumber(ctx, p["kineticGen1"]!!, "EnergyStored")
            val gen2Eu = getBeNumber(ctx, p["kineticGen2"]!!, "EnergyStored")
            val boilerSteam = getBeNumber(ctx, p["boiler"]!!, "SteamTank.amount")
            val boilerTemp = getBeNumber(ctx, p["boiler"]!!, "SystemHeatMilli")
            val boilerWater = getBeNumber(ctx, p["boiler"]!!, "WaterTank.amount")
            val t1Steam = getBeNumber(ctx, p["turbine1"]!!, "SteamTank.amount")
            throw AssertionError(
                "MFSU EU 未上升 (before=$eu0, after=$eu1). " +
                    "诊断: gen1.eu=$gen1Eu, gen2.eu=$gen2Eu, boiler.temp=$boilerTemp, " +
                    "boiler.steam=$boilerSteam, boiler.water=$boilerWater, turbine1.steam=$t1Steam",
            )
        }
        if (euRate < 105 || euRate > 195) {
            throw AssertionError("EU 速率异常: ${euRate} EU/t，期望 ~150 EU/t (105~195). eu0=$eu0, eu1=$eu1")
        }
    }

    /** 蒸馏水闭环：20 秒内锅炉水量损耗 ≤ 2%。 */
    @Test
    fun distilledWaterLoop(ctx: TestContext) {
        val p = layout(ctx)
        setupTierChain(ctx)

        waitTicks(ctx, 200) // 等链条进入稳态
        val waterBefore = getBeNumber(ctx, p["boiler"]!!, "WaterTank.amount")
        waitTicks(ctx, 400)
        val waterAfter = getBeNumber(ctx, p["boiler"]!!, "WaterTank.amount")
        val loss = waterBefore - waterAfter
        val maxAllowedLoss = kotlin.math.floor(waterBefore * 0.02)
        if (loss > maxAllowedLoss) {
            throw AssertionError(
                "蒸馏水损耗过大: before=$waterBefore, after=$waterAfter, loss=$loss droplets, " +
                    "允许最大 $maxAllowedLoss。可能存在蒸汽积压爆炸或回流堵塞。",
            )
        }
    }
}
