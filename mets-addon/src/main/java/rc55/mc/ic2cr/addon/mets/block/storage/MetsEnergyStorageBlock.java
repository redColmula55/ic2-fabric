package rc55.mc.ic2cr.addon.mets.block.storage;

import ic2_120.content.block.storage.EnergyStorageBlock;
import ic2_120.content.block.storage.EnergyStorageConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rc55.mc.ic2cr.addon.mets.blockentity.storage.MetsEnergyStorageBlockEntity;

public class MetsEnergyStorageBlock extends EnergyStorageBlock {
    public MetsEnergyStorageBlock(@NotNull EnergyStorageConfig config, Settings settings) {
        super(config);
    }

    public static MetsEnergyStorageBlock createAsLESU(Settings settings) {
        return new MetsEnergyStorageBlock(MetsEnergyStorageBlockEntity.LESU, settings);
    }

    public static MetsEnergyStorageBlock createAsEESU(Settings settings) {
        return new MetsEnergyStorageBlock(MetsEnergyStorageBlockEntity.EESU, settings);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return MetsEnergyStorageBlockEntity.createForBlock(pos, state, this.getConfig());
    }
}
