package rc55.mc.ic2cr.addon.mets;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import rc55.mc.ic2cr.addon.mets.datagen.MetsModelDataGen;
import rc55.mc.ic2cr.addon.mets.datagen.MetsRecipeDataGen;
import rc55.mc.ic2cr.addon.mets.datagen.loot.MetsBlockLootTableDataGen;
import rc55.mc.ic2cr.addon.mets.datagen.tag.MetsBlockTagDataGen;
import rc55.mc.ic2cr.addon.mets.datagen.tag.MetsFluidTagDataGen;

public class MoreElectricToolsDataGen implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        final FabricDataGenerator.Pack generator = fabricDataGenerator.createPack();
        // ===== 材质 =====
        // 模型
        generator.addProvider(MetsModelDataGen::new);
        // ===== 数据 =====
        // tag
        generator.addProvider(MetsFluidTagDataGen::new);
        generator.addProvider(MetsBlockTagDataGen::new);
        // 配方
        generator.addProvider(MetsRecipeDataGen::new);
        // 战利品表
        generator.addProvider(MetsBlockLootTableDataGen::new);
    }
}
