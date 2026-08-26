package rc55.mc.ic2cr.addon.mets.item.block;

import ic2_120.content.block.storage.EnergyStorageBlock;
import org.jetbrains.annotations.NotNull;
import rc55.mc.ic2cr.addon.mets.block.storage.MetsChargepadBlock;
import rc55.mc.ic2cr.addon.mets.block.storage.MetsEnergyStorageBlock;

public class MetsEnergyStorageBlockItem extends EnergyStorageBlock.EnergyStorageBlockItem {
    public MetsEnergyStorageBlockItem(@NotNull MetsEnergyStorageBlock block, @NotNull Settings settings) {
        super(block, settings, block.getConfig());
    }

    public MetsEnergyStorageBlockItem(@NotNull MetsChargepadBlock block, @NotNull Settings settings) {
        super(block, settings, block.getConfig());
    }

    @Override
    protected @NotNull String getTranslationKeyFull() {
        return this.getTranslationKey() + ".full";
    }
}
