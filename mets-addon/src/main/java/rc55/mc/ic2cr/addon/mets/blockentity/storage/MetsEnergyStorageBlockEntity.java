package rc55.mc.ic2cr.addon.mets.blockentity.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import ic2_120.content.block.storage.EnergyStorageBlockEntity;
import ic2_120.content.block.storage.EnergyStorageConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import rc55.mc.ic2cr.addon.mets.blockentity.MetsBlockEntityTypes;
import team.reborn.energy.api.EnergyStorage;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class MetsEnergyStorageBlockEntity extends EnergyStorageBlockEntity {
    public MetsEnergyStorageBlockEntity(BlockEntityType<?> type, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull EnergyStorageConfig config) {
        super(type, pos, state, config);
    }

    public static MetsEnergyStorageBlockEntity createForBlock(BlockPos pos, BlockState state, EnergyStorageConfig config) {
        return creatorMap.getOrDefault(config, MetsEnergyStorageBlockEntity::createAsLESU).apply(pos, state);
    }

    public static MetsEnergyStorageBlockEntity createAsLESU(BlockPos pos, BlockState state) {
        return new MetsEnergyStorageBlockEntity(MetsBlockEntityTypes.LESU, pos, state, LESU);
    }

    public static MetsEnergyStorageBlockEntity createAsLESUChargepad(BlockPos pos, BlockState state) {
        return new MetsEnergyStorageBlockEntity(MetsBlockEntityTypes.LESU_CHARGEPAD, pos, state, LESU_CHARGEPAD);
    }

    public static MetsEnergyStorageBlockEntity createAsEESU(BlockPos pos, BlockState state) {
        return new MetsEnergyStorageBlockEntity(MetsBlockEntityTypes.EESU, pos, state, EESU);
    }

    public static MetsEnergyStorageBlockEntity createAsEESUChargepad(BlockPos pos, BlockState state) {
        return new MetsEnergyStorageBlockEntity(MetsBlockEntityTypes.EESU_CHARGEPAD, pos, state, EESU_CHARGEPAD);
    }

    public static void initEnergyStorage() {
        final List<BlockEntityType<MetsEnergyStorageBlockEntity>> types = ImmutableList.of(
                MetsBlockEntityTypes.LESU, MetsBlockEntityTypes.LESU_CHARGEPAD,
                MetsBlockEntityTypes.EESU, MetsBlockEntityTypes.EESU_CHARGEPAD
        );
        for (final var type : types) {
            EnergyStorage.SIDED.registerForBlockEntity(
                    (blockEntity, direction) -> blockEntity.getSync().getSideStorage(direction), type
            );
        }
    }

    public static final EnergyStorageConfig LESU = new EnergyStorageConfig(
            3, 1000000L, 2, true, false, true
    );
    public static final EnergyStorageConfig LESU_CHARGEPAD = new EnergyStorageConfig(
            3, 1000000L, 2, true, true, true
    );
    public static final EnergyStorageConfig EESU = new EnergyStorageConfig(
            5, 400000000L, 2, true, false, true
    );
    public static final EnergyStorageConfig EESU_CHARGEPAD = new EnergyStorageConfig(
            5, 400000000L, 2, true, true, true
    );

    private static final Map<EnergyStorageConfig, BiFunction<BlockPos, BlockState, MetsEnergyStorageBlockEntity>> creatorMap = Util.make(Maps.newHashMap(), map -> {
        map.put(LESU, MetsEnergyStorageBlockEntity::createAsLESU);
        map.put(LESU_CHARGEPAD, MetsEnergyStorageBlockEntity::createAsLESUChargepad);
        map.put(EESU, MetsEnergyStorageBlockEntity::createAsEESU);
        map.put(EESU_CHARGEPAD, MetsEnergyStorageBlockEntity::createAsEESUChargepad);
    });

    @Override
    public @NotNull String getContainerTranslationKey() {
        return this.getCachedState().getBlock().getTranslationKey();
    }
}
