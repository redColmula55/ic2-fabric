package rc55.mc.ic2cr.addon.mets.datagen.loot;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import rc55.mc.ic2cr.addon.mets.block.MetsBlocks;
import rc55.mc.ic2cr.addon.mets.item.MetsItems;

public class MetsBlockLootTableDataGen extends FabricBlockLootTableProvider {
    public MetsBlockLootTableDataGen(FabricDataOutput dataOutput) {
        super(dataOutput);
    }

    @Override
    public void generate() {
        addDrop(MetsBlocks.NIOBIUM_ORE, oreDrops(MetsBlocks.NIOBIUM_ORE, MetsItems.RAW_NIOBIUM));
        addDrop(MetsBlocks.DEEPSLATE_NIOBIUM_ORE, oreDrops(MetsBlocks.DEEPSLATE_NIOBIUM_ORE, MetsItems.RAW_NIOBIUM));
        addDrop(MetsBlocks.TITANIUM_ORE, oreDrops(MetsBlocks.TITANIUM_ORE, MetsItems.RAW_TITANIUM));
        addDrop(MetsBlocks.DEEPSLATE_TITANIUM_ORE, oreDrops(MetsBlocks.DEEPSLATE_TITANIUM_ORE, MetsItems.RAW_TITANIUM));
        addDrop(MetsBlocks.TITANIUM_BLOCK);
    }
}
