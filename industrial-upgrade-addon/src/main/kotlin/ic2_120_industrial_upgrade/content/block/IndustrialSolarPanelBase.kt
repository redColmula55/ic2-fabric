package ic2_120_industrial_upgrade.content.block

import ic2_120.content.block.MachineBlock
import ic2_120_advanced_solar_addon.content.block.SolarPanelBlockEntity
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.screen.NamedScreenHandlerFactory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/** 所有面板共用的 ACTIVE 属性，名称与 advanced-solar-addon 保持一致。 */
val SOLAR_ACTIVE: BooleanProperty = BooleanProperty.of("active")

/**
 * 工业升级附属所有太阳能发电机的抽象方块基类。
 * 交互/放置/状态属性逻辑统一，行为完全复用 ASA 的 SolarPanelBlockEntity。
 */
abstract class IndustrialSolarPanelBlock : MachineBlock() {
    override fun createScreenHandlerFactory(state: BlockState, world: World, pos: BlockPos): NamedScreenHandlerFactory? =
        world.getBlockEntity(pos) as? NamedScreenHandlerFactory

    @Deprecated("Override without Hand parameter", ReplaceWith("onUse(state, world, pos, player, hit)"))
    override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
        if (!world.isClient) createScreenHandlerFactory(state, world, pos)?.let { player.openHandledScreen(it) }
        return ActionResult.SUCCESS
    }

    override fun appendProperties(builder: StateManager.Builder<net.minecraft.block.Block, BlockState>) {
        super.appendProperties(builder)
        builder.add(SOLAR_ACTIVE)
    }

    override fun getPlacementState(ctx: ItemPlacementContext): BlockState? =
        super.getPlacementState(ctx)?.with(SOLAR_ACTIVE, false)
}
