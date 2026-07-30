package ic2_120_industrial_upgrade.content.sync

import ic2_120.content.TickLimitedSidedEnergyContainer
import ic2_120.content.energy.EnergyTier
import ic2_120.content.syncs.SyncSchema
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants
import net.minecraft.util.math.Direction

/**
 * 中子制造机同步数据。参照 core 的 MatterGeneratorSync。
 *
 * 字段：
 * - amount/capacity：来自父类 TickLimitedSidedEnergyContainer（能量存储）
 * - fluidAmount/fluidCapacity：中子流体槽当前量/容量（droplets）
 * - progress：当前 1mB 生成周期内的能量进度百分比（0..100）
 */
class NeutronFabricatorSync(
    schema: SyncSchema,
    capacity: Long,
    tier: Int,
    private val getFacing: () -> Direction,
    currentTickProvider: () -> Long?
) : TickLimitedSidedEnergyContainer(
    baseCapacity = capacity,
    maxInsertPerTick = EnergyTier.euPerTickFromTier(tier),
    maxExtractPerTick = EnergyTier.euPerTickFromTier(tier),
    currentTickProvider = currentTickProvider
) {
    companion object {
        const val NBT_ENERGY = "Energy"
    }

    /** 中子流体槽当前量（droplets） */
    var fluidAmount by schema.int("FluidAmount", default = 0)
    /** 中子流体槽容量（droplets），与 NeutronFabricatorBlockEntity.TANK_CAPACITY_DROPLETS 一致（10 桶） */
    var fluidCapacity by schema.int("FluidCapacity", default = (FluidConstants.BUCKET * 10L).toInt())
    /** 进度百分比 0..100 */
    var progress by schema.int("Progress", default = 0)
}
