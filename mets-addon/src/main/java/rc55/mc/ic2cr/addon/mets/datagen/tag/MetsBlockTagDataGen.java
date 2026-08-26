package rc55.mc.ic2cr.addon.mets.datagen.tag;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import rc55.mc.ic2cr.addon.mets.block.MetsBlocks;

import java.util.concurrent.CompletableFuture;

public class MetsBlockTagDataGen extends FabricTagProvider.BlockTagProvider {
    public MetsBlockTagDataGen(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE).add(
                MetsBlocks.TITANIUM_BLOCK, MetsBlocks.NIOBIUM_ORE, MetsBlocks.DEEPSLATE_NIOBIUM_ORE,
                MetsBlocks.TITANIUM_ORE, MetsBlocks.DEEPSLATE_TITANIUM_ORE
        );
        getOrCreateTagBuilder(BlockTags.NEEDS_DIAMOND_TOOL).add(
                MetsBlocks.TITANIUM_BLOCK, MetsBlocks.NIOBIUM_ORE, MetsBlocks.DEEPSLATE_NIOBIUM_ORE,
                MetsBlocks.TITANIUM_ORE, MetsBlocks.DEEPSLATE_TITANIUM_ORE
        );
    }
}
