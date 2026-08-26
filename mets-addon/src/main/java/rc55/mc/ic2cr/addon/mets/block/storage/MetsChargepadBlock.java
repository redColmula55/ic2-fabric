package rc55.mc.ic2cr.addon.mets.block.storage;

import ic2_120.content.block.ChargepadBlock;
import ic2_120.content.block.storage.EnergyStorageConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rc55.mc.ic2cr.addon.mets.blockentity.storage.MetsEnergyStorageBlockEntity;

public class MetsChargepadBlock extends ChargepadBlock {
    public MetsChargepadBlock(@NotNull EnergyStorageConfig config, Settings settings) {
        super(config);
    }

    public static MetsChargepadBlock createAsLESU(Settings settings) {
        return new MetsChargepadBlock(MetsEnergyStorageBlockEntity.LESU_CHARGEPAD, settings);
    }

    public static MetsChargepadBlock createAsEESU(Settings settings) {
        return new MetsChargepadBlock(MetsEnergyStorageBlockEntity.EESU_CHARGEPAD, settings);
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return MetsEnergyStorageBlockEntity.createForBlock(pos, state, this.getConfig());
    }
}
