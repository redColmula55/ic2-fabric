package ic2_120_industrial_upgrade.content.fluid

import ic2_120.content.fluid.ModFluids
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import net.minecraft.block.Block
import net.minecraft.fluid.FlowableFluid
import net.minecraft.item.Item

/**
 * 中子流体（Neutron）：由中子制造机产出，桶装后经压缩机合成中子碎片。
 *
 * 复用 core 的 [ModFluids.registerFluidFor] 开放 API 注册，不再独立实现 Still/Flowing 子类。
 * 1.4.0 中子流体本就借用 uu_matter 贴图，这里仍使用相同贴图名（已从 1.4.0 拷贝为 neutron_still/flow）。
 */
object NeutronFluid {

    /** 中子流体（Still） */
    val NEUTRON_STILL: FlowableFluid by lazy {
        ModFluids.Ic2Fluid.stillFluidMap["neutron"] as? FlowableFluid
            ?: error("Neutron fluid not registered yet")
    }

    /** 中子流体（Flowing） */
    val NEUTRON_FLOWING: FlowableFluid by lazy {
        ModFluids.Ic2Fluid.flowingFluidMap["neutron"] as? FlowableFluid
            ?: error("Flowing neutron fluid not registered yet")
    }

    /** 中子流体方块 */
    lateinit var NEUTRON_BLOCK: Block
        private set

    /** 中子流体桶 */
    lateinit var NEUTRON_BUCKET: Item
        private set

    fun register() {
        val result = ModFluids.registerFluidFor(
            modId = IC2IndustrialUpgrade.MOD_ID,
            name = "neutron",
            stillTex = "neutron_still",
            flowTex = "neutron_flow",
            tintArgb = 0xFF8B7FD4.toInt() // 淡紫色，呼应中子主题
        )
        NEUTRON_BLOCK = result.block
        NEUTRON_BUCKET = result.bucket
    }
}
