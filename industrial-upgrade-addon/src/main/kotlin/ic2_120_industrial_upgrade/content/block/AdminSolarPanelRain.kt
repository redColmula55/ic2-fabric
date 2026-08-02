package ic2_120_industrial_upgrade.content.block

import ic2_120.registry.CreativeTab
import ic2_120.registry.annotation.ModBlock
import ic2_120.registry.annotation.ModBlockEntity
import ic2_120.registry.annotation.RecipeProvider
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120.registry.type
import ic2_120_industrial_upgrade.content.item.RainLinse
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import net.minecraft.text.Text
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityTicker
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.function.Consumer

// i18n: block.ic2_120_industrial_upgrade.admin_solar_panelrain
// zh_cn: 光吸雨能太阳能发电机  en_us: Admin Rain Panel
@ModBlock(name = "admin_solar_panelrain", registerItem = true, tab = CreativeTab.INDUSTRIAL_UPGRADE, group = "solar_panel")
class AdminSolarPanelRainBlock : IndustrialSolarPanelBlock() {
    override fun createBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        AdminSolarPanelRainBlockEntity(pos, state)

    override fun <T : BlockEntity> getTicker(
        world: World,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        if (world.isClient) null
        else checkType(type, AdminSolarPanelRainBlockEntity::class.type()) { w, p, s, be -> be.tick(w, p, s) }

    companion object {
        @RecipeProvider
        fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
            // "BA"：光吸太阳能发电机 + RainLinse（ASA）
            ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, AdminSolarPanelRainBlock::class.item(), 1)
                .pattern("B")
                .pattern("A")
                .input('B', AdminSolarPanelBlock::class.item())
                .input('A', RainLinse::class.instance())
                .criterion(hasItem(AdminSolarPanelBlock::class.item()), conditionsFromItem(AdminSolarPanelBlock::class.item()))
                .offerTo(exporter, IC2IndustrialUpgrade.id("admin_solar_panelrain"))
        }
    }
}

@ModBlockEntity(block = AdminSolarPanelRainBlock::class)
class AdminSolarPanelRainBlockEntity(pos: BlockPos, state: BlockState) : RainPanelBlockEntity(
    AdminSolarPanelRainBlockEntity::class.type(), pos, state,
    dayPower = 1048576, nightPower = 1048576, maxStorage = 100000000000L, tier = 9,
    rainPower = 1048576, activeProperty = SOLAR_ACTIVE
) {
    override fun getBlockName(): String = "admin_solar_panelrain"
    override fun getDisplayName(): Text = Text.translatable("block.${IC2IndustrialUpgrade.MOD_ID}.${getBlockName()}")
}
