package ic2_120_industrial_upgrade.content.block

import ic2_120.content.block.cables.BaseCableBlock
import ic2_120.content.block.cables.GlassFibreCableBlock
import ic2_120.content.item.Alloy
import ic2_120.content.item.BronzeIngot
import ic2_120.content.item.CarbonPlate
import ic2_120.content.item.DenseCopperPlate
import ic2_120.content.item.DenseGoldPlate
import ic2_120.content.item.DenseLeadPlate
import ic2_120.content.item.DenseSteelPlate
import ic2_120.content.item.DenseTinPlate
import ic2_120.content.item.MixedMetalIngot
import ic2_120.content.item.RubberItem
import ic2_120.content.item.SilverIngot
import ic2_120.content.item.SteelIngot
import ic2_120.content.item.energy.ITiered
import ic2_120.registry.id
import ic2_120.registry.instance
import ic2_120.registry.item
import ic2_120_industrial_upgrade.IC2IndustrialUpgrade
import net.minecraft.data.server.recipe.RecipeJsonProvider
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder
import net.minecraft.recipe.book.RecipeCategory
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.hasItem
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider.conditionsFromItem
import java.util.function.Consumer

// ── 工业升级高压玻璃 EU 导线（6 档）───────────────────────────────────────────
//
// 数值规则（与 core 衔接）：
//   - 基准：core 玻璃纤维导线 = tier 5 = 8192 EU/t
//   - 首档（光谱）= 玻璃纤维 × 4 = tier 6 = 32768 EU/t，之后每档 ×4
//   - tier = 6..11，capacity 由 EnergyTier.euPerTickFromTier(tier) 自动给出（32×4^(tier-1)）
//   - 损耗 loss 固定 0（无损传输），getEnergyLoss 返回 0L（单位毫 EU）
//
// 注册方式：本类不走 @ModBlock（附属 main entrypoint 晚于 core，无法被 core 的 CableBlockEntity
// 统一注册捕获）。改由 IUCableProvider 通过 core 的 "ic2_120:cables" entrypoint 调用
// ClassScanner.registerCableBlock 在 core.onInitialize 期间注册，使 core 能把它们合并进
// CableBlockEntity 的统一 BlockEntityType。
//
// 合成配方（级联，全部用 core 已有材料）保留 @RecipeProvider，由 datagen 在构建期生成。
//
// 命名等级链（已确认）：玻璃纤维(core) < 光谱 < 质子 < 奇异 < 神话 < 量子 < 光子

/** 光谱导线（tier 6，32768 EU/t，无损）。 */
class SpectralCableBlock : BaseCableBlock(), ITiered {
    override val tier: Int = 6
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 0L
}

/** 质子导线（tier 7，131072 EU/t，无损）。 */
class ProtonCableBlock : BaseCableBlock(), ITiered {
    override val tier: Int = 7
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 0L
}

/** 奇异导线（tier 8，524288 EU/t，无损）。 */
class SingularCableBlock : BaseCableBlock(), ITiered {
    override val tier: Int = 8
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 0L
}

/** 神话导线（tier 9，2097152 EU/t，无损）。 */
class MythicalCableBlock : BaseCableBlock(), ITiered {
    override val tier: Int = 9
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 0L
}

/** 量子导线（tier 10，8388608 EU/t，无损）。 */
class QuantumCableBlock : BaseCableBlock(), ITiered {
    override val tier: Int = 10
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 0L
}

/** 光子导线（tier 11，33554432 EU/t，无损）。 */
class PhotonicCableBlock : BaseCableBlock(), ITiered {
    override val tier: Int = 11
    override fun getTransferRate(): Long = nominalEuPerTick()
    override fun getEnergyLoss(): Long = 0L
}

/**
 * 导线合成配方（级联，全部用 core 已有材料，不引入新材料）。
 *
 * 集中在一个 object 的 @RecipeProvider 方法里生成全部 6 档合成表，
 * 由 core 的 ClassScanner 在附属 main entrypoint（datagen 阶段）扫描执行。
 */
object CableRecipes {
    /**
     * 生成 6 档导线的级联合成表。由 [ic2_120_industrial_upgrade.content.recipes.ModRecipeProvider]
     * 显式调用（导线不走 @ModBlock，其 companion 不参与 ClassScanner 的 @RecipeProvider 扫描）。
     */
    fun generateRecipes(exporter: Consumer<RecipeJsonProvider>) {
        // 1 光谱：core 玻璃纤维 + 混合金属锭 + 橡胶
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SpectralCableBlock::class.item(), 1)
            .pattern(" A ")
            .pattern("BBB")
            .pattern(" C ")
            .input('A', GlassFibreCableBlock::class.item())
            .input('B', MixedMetalIngot::class.instance())
            .input('C', RubberItem::class.instance())
            .criterion(hasItem(GlassFibreCableBlock::class.item()), conditionsFromItem(GlassFibreCableBlock::class.item()))
            .offerTo(exporter, IC2IndustrialUpgrade.id("spectral_cable"))

        // 2 质子：光谱导线 + 致密锡板 + 银锭
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ProtonCableBlock::class.item(), 1)
            .pattern(" A ")
            .pattern("BCB")
            .pattern(" A ")
            .input('A', SpectralCableBlock::class.item())
            .input('B', DenseTinPlate::class.instance())
            .input('C', SilverIngot::class.instance())
            .criterion(hasItem(SpectralCableBlock::class.item()), conditionsFromItem(SpectralCableBlock::class.item()))
            .offerTo(exporter, IC2IndustrialUpgrade.id("proton_cable"))

        // 3 奇异：质子导线 + 致密锡板 + 高级合金
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, SingularCableBlock::class.item(), 1)
            .pattern(" A ")
            .pattern("BCB")
            .pattern(" A ")
            .input('A', ProtonCableBlock::class.item())
            .input('B', DenseTinPlate::class.instance())
            .input('C', Alloy::class.instance())
            .criterion(hasItem(ProtonCableBlock::class.item()), conditionsFromItem(ProtonCableBlock::class.item()))
            .offerTo(exporter, IC2IndustrialUpgrade.id("singular_cable"))

        // 4 神话：奇异导线 + 致密铅板 + 高级合金 + 致密金板
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, MythicalCableBlock::class.item(), 1)
            .pattern("DAD")
            .pattern("BCB")
            .pattern("DAD")
            .input('A', SingularCableBlock::class.item())
            .input('B', DenseLeadPlate::class.instance())
            .input('C', Alloy::class.instance())
            .input('D', DenseGoldPlate::class.instance())
            .criterion(hasItem(SingularCableBlock::class.item()), conditionsFromItem(SingularCableBlock::class.item()))
            .offerTo(exporter, IC2IndustrialUpgrade.id("mythical_cable"))

        // 5 量子：神话导线 + 钢锭 + 碳板 + 青铜锭
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, QuantumCableBlock::class.item(), 1)
            .pattern("DAD")
            .pattern("BCB")
            .pattern("DAD")
            .input('A', MythicalCableBlock::class.item())
            .input('B', SteelIngot::class.instance())
            .input('C', CarbonPlate::class.instance())
            .input('D', BronzeIngot::class.instance())
            .criterion(hasItem(MythicalCableBlock::class.item()), conditionsFromItem(MythicalCableBlock::class.item()))
            .offerTo(exporter, IC2IndustrialUpgrade.id("quantum_cable"))

        // 6 光子：量子导线 + 致密铜板 + 致密钢板
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, PhotonicCableBlock::class.item(), 1)
            .pattern(" A ")
            .pattern("BCB")
            .pattern(" A ")
            .input('A', QuantumCableBlock::class.item())
            .input('B', DenseCopperPlate::class.instance())
            .input('C', DenseSteelPlate::class.instance())
            .criterion(hasItem(QuantumCableBlock::class.item()), conditionsFromItem(QuantumCableBlock::class.item()))
            .offerTo(exporter, IC2IndustrialUpgrade.id("photonic_cable"))
    }
}
