package rc55.mc.ic2cr.addon.mets.datagen;

import ic2_120.content.block.storage.EnergyStorageBlock;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.*;
import net.minecraft.item.Item;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import rc55.mc.ic2cr.addon.mets.MoreElectricTools;
import rc55.mc.ic2cr.addon.mets.block.MetsBlocks;
import rc55.mc.ic2cr.addon.mets.block.storage.MetsChargepadBlock;
import rc55.mc.ic2cr.addon.mets.block.storage.MetsEnergyStorageBlock;
import rc55.mc.ic2cr.addon.mets.fluid.MetsFluids;
import rc55.mc.ic2cr.addon.mets.item.MetsItems;
import rc55.mc.rfapi.data.gen.RFApiModelGenerationHelper;

public class MetsModelDataGen extends FabricModelProvider {
    public MetsModelDataGen(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator generator) {
        RFApiModelGenerationHelper.createFluidBlockModel(generator, Identifier.of(MoreElectricTools.MODID, "block/fluid/crude_oil_still"), MetsFluids.CRUDE_OIL);
        RFApiModelGenerationHelper.createFluidBlockModel(generator, Identifier.of(MoreElectricTools.MODID, "block/fluid/diesel_oil_still"), MetsFluids.DIESEL_OIL);
        generator.registerSimpleCubeAll(MetsBlocks.TITANIUM_ORE);
        generator.registerSimpleCubeAll(MetsBlocks.DEEPSLATE_TITANIUM_ORE);
        generator.registerSimpleCubeAll(MetsBlocks.NIOBIUM_ORE);
        generator.registerSimpleCubeAll(MetsBlocks.DEEPSLATE_NIOBIUM_ORE);
        generator.registerSimpleCubeAll(MetsBlocks.TITANIUM_BLOCK);

        registerEnergyStorage(generator, "lesu", MetsBlocks.LESU, MetsBlocks.LESU_CHARGEPAD);
        registerEnergyStorage(generator, "eesu", MetsBlocks.EESU, MetsBlocks.EESU_CHARGEPAD);
    }

    private static void registerEnergyStorage(
            BlockStateModelGenerator generator, String name, MetsEnergyStorageBlock storageBlock, MetsChargepadBlock chargepadBlock
    ) {
        final Identifier bottom = Identifier.of(MoreElectricTools.MODID, "block/%s/bottom".formatted(name));
        final Identifier chargepadTop = Identifier.of(MoreElectricTools.MODID, "block/%s/chargepad_top".formatted(name));
        final Identifier chargepadActive = Identifier.of(MoreElectricTools.MODID, "block/%s/chargepad_top_active".formatted(name));
        final Identifier front = Identifier.of(MoreElectricTools.MODID, "block/%s/front".formatted(name));
        final Identifier side = Identifier.of(MoreElectricTools.MODID, "block/%s/side".formatted(name));
        final Identifier back = Identifier.of(MoreElectricTools.MODID, "block/%s/back".formatted(name));

        TextureMap textureMap = new TextureMap().put(TextureKey.PARTICLE, side)
                .put(TextureKey.WEST, side).put(TextureKey.EAST, side)
                .put(TextureKey.DOWN, bottom).put(TextureKey.UP, bottom)
                .put(TextureKey.NORTH, front).put(TextureKey.SOUTH, back);

        final Identifier storageModel = Identifier.of(MoreElectricTools.MODID, "block/%s/storage".formatted(name));
        Models.CUBE.upload(storageModel, textureMap, generator.modelCollector);
        generator.blockStateCollector.accept(VariantsBlockStateSupplier.create(storageBlock).coordinate(energyStorageVariantMap(storageModel, storageModel)));
        generator.registerParentedItemModel(storageBlock, storageModel);

        final Identifier chargepadModel = Identifier.of(MoreElectricTools.MODID, "block/%s/chargepad".formatted(name));
        textureMap = textureMap.put(TextureKey.UP, chargepadTop);
        Models.CUBE.upload(chargepadModel, textureMap, generator.modelCollector);

        final Identifier chargepadActiveModel = Identifier.of(MoreElectricTools.MODID, "block/%s/chargepad_active".formatted(name));
        textureMap = textureMap.put(TextureKey.UP, chargepadActive);
        Models.CUBE.upload(chargepadActiveModel, textureMap, generator.modelCollector);
        generator.blockStateCollector.accept(VariantsBlockStateSupplier.create(chargepadBlock).coordinate(chargepadVariantMap(chargepadModel, chargepadActiveModel)));
        generator.registerParentedItemModel(chargepadBlock, chargepadModel);
    }

    private static BlockStateVariantMap energyStorageVariantMap(Identifier modelId, Identifier activeModelId) {
        return BlockStateVariantMap.create(Properties.FACING, EnergyStorageBlock.Companion.getACTIVE())
                .register(Direction.DOWN, false, BlockStateVariant.create().put(VariantSettings.MODEL, modelId)
                        .put(VariantSettings.X, VariantSettings.Rotation.R90))
                .register(Direction.UP, false, BlockStateVariant.create().put(VariantSettings.MODEL, modelId)
                        .put(VariantSettings.X, VariantSettings.Rotation.R270))
                .register(Direction.NORTH, false, BlockStateVariant.create().put(VariantSettings.MODEL, modelId))
                .register(Direction.EAST, false, BlockStateVariant.create().put(VariantSettings.MODEL, modelId)
                        .put(VariantSettings.Y, VariantSettings.Rotation.R90))
                .register(Direction.SOUTH, false, BlockStateVariant.create().put(VariantSettings.MODEL, modelId)
                        .put(VariantSettings.Y, VariantSettings.Rotation.R180))
                .register(Direction.WEST, false, BlockStateVariant.create().put(VariantSettings.MODEL, modelId)
                        .put(VariantSettings.Y, VariantSettings.Rotation.R270))
                .register(Direction.DOWN, true, BlockStateVariant.create().put(VariantSettings.MODEL, activeModelId)
                        .put(VariantSettings.X, VariantSettings.Rotation.R90))
                .register(Direction.UP, true, BlockStateVariant.create().put(VariantSettings.MODEL, activeModelId)
                        .put(VariantSettings.X, VariantSettings.Rotation.R270))
                .register(Direction.NORTH, true, BlockStateVariant.create().put(VariantSettings.MODEL, activeModelId))
                .register(Direction.EAST, true, BlockStateVariant.create().put(VariantSettings.MODEL, activeModelId)
                        .put(VariantSettings.Y, VariantSettings.Rotation.R90))
                .register(Direction.SOUTH, true, BlockStateVariant.create().put(VariantSettings.MODEL, activeModelId)
                        .put(VariantSettings.Y, VariantSettings.Rotation.R180))
                .register(Direction.WEST, true, BlockStateVariant.create().put(VariantSettings.MODEL, activeModelId)
                        .put(VariantSettings.Y, VariantSettings.Rotation.R270));
    }

    private static BlockStateVariantMap chargepadVariantMap(Identifier modelId, Identifier activeModelId) {
        return BlockStateVariantMap.create(Properties.HORIZONTAL_FACING, EnergyStorageBlock.Companion.getACTIVE())
                .register(Direction.NORTH, false, BlockStateVariant.create().put(VariantSettings.MODEL, modelId))
                .register(Direction.EAST, false, BlockStateVariant.create().put(VariantSettings.MODEL, modelId)
                        .put(VariantSettings.Y, VariantSettings.Rotation.R90))
                .register(Direction.SOUTH, false, BlockStateVariant.create().put(VariantSettings.MODEL, modelId)
                        .put(VariantSettings.Y, VariantSettings.Rotation.R180))
                .register(Direction.WEST, false, BlockStateVariant.create().put(VariantSettings.MODEL, modelId)
                        .put(VariantSettings.Y, VariantSettings.Rotation.R270))
                .register(Direction.NORTH, true, BlockStateVariant.create().put(VariantSettings.MODEL, activeModelId))
                .register(Direction.EAST, true, BlockStateVariant.create().put(VariantSettings.MODEL, activeModelId)
                        .put(VariantSettings.Y, VariantSettings.Rotation.R90))
                .register(Direction.SOUTH, true, BlockStateVariant.create().put(VariantSettings.MODEL, activeModelId)
                        .put(VariantSettings.Y, VariantSettings.Rotation.R180))
                .register(Direction.WEST, true, BlockStateVariant.create().put(VariantSettings.MODEL, activeModelId)
                        .put(VariantSettings.Y, VariantSettings.Rotation.R270));
    }

    @Override
    public void generateItemModels(ItemModelGenerator generator) {
        RFApiModelGenerationHelper.createVanillaBucketItemModel(generator, MetsItems.CRUDE_OIL_BUCKET, MetsItems.DIESEL_OIL_BUCKET);

        generator.register(MetsItems.ADVANCED_IRIDIUM_SWORD, Models.HANDHELD);
        
        regSimpleGenerated(generator, MetsItems.CRUSHED_NIOBIUM_ORE, MetsItems.NIOBIUM_DUST, MetsItems.CRUSHED_TITANIUM_ORE,
                MetsItems.TITANIUM_DUST, MetsItems.TITANIUM_INGOT, MetsItems.TITANIUM_PLATE, MetsItems.TITANIUM_CASING,
                MetsItems.TITANIUM_SHAFT, MetsItems.NIOBIUM_TITANIUM_DUST, MetsItems.NIOBIUM_TITANIUM_INGOT,
                MetsItems.NIOBIUM_TITANIUM_PLATE, MetsItems.LIVING_CIRCUIT, MetsItems.SUPER_CIRCUIT, MetsItems.NANO_LIVING_METAL,
                MetsItems.NEUTRON_PLATE, MetsItems.THORIUM_DUST, MetsItems.THORIUM_PILE, MetsItems.SUPER_IRIDIUM_ALLOY,
                MetsItems.SUPER_IRIDIUM_COMPRESS_PLATE, MetsItems.LENS, MetsItems.DIAMOND_LENS, MetsItems.FIELD_GENERATOR,
                MetsItems.PLANT_EXTRACT, MetsItems.SUPERCONDUCTING_CABLE, MetsItems.RAW_NIOBIUM, MetsItems.RAW_TITANIUM
        );
    }
    
    private static void regSimpleGenerated(ItemModelGenerator generator, Item... items) {
        for (final Item item : items) {
            generator.register(item, Models.GENERATED);
        }
    }
}
