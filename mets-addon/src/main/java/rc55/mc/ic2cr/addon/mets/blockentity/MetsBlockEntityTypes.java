package rc55.mc.ic2cr.addon.mets.blockentity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import rc55.mc.ic2cr.addon.mets.MoreElectricTools;
import rc55.mc.ic2cr.addon.mets.block.MetsBlocks;
import rc55.mc.ic2cr.addon.mets.blockentity.storage.MetsEnergyStorageBlockEntity;

public class MetsBlockEntityTypes {
    public static final BlockEntityType<MetsEnergyStorageBlockEntity> LESU = register("lesu",
            FabricBlockEntityTypeBuilder.create(MetsEnergyStorageBlockEntity::createAsLESU, MetsBlocks.LESU)
    );
    public static final BlockEntityType<MetsEnergyStorageBlockEntity> LESU_CHARGEPAD = register("lesu_chargepad",
            FabricBlockEntityTypeBuilder.create(MetsEnergyStorageBlockEntity::createAsLESUChargepad, MetsBlocks.LESU_CHARGEPAD)
    );
    public static final BlockEntityType<MetsEnergyStorageBlockEntity> EESU = register("eesu",
            FabricBlockEntityTypeBuilder.create(MetsEnergyStorageBlockEntity::createAsEESU, MetsBlocks.EESU)
    );
    public static final BlockEntityType<MetsEnergyStorageBlockEntity> EESU_CHARGEPAD = register("eesu_chargepad",
            FabricBlockEntityTypeBuilder.create(MetsEnergyStorageBlockEntity::createAsEESUChargepad, MetsBlocks.EESU_CHARGEPAD)
    );

    private static <T extends BlockEntity> BlockEntityType<T> register(String id, FabricBlockEntityTypeBuilder<T> builder) {
        return Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MoreElectricTools.MODID, id), builder.build());
    }

    public static void init() {
        MetsEnergyStorageBlockEntity.initEnergyStorage();
    }
}
