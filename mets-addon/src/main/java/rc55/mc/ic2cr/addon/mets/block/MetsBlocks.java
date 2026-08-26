package rc55.mc.ic2cr.addon.mets.block;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import rc55.mc.ic2cr.addon.mets.MoreElectricTools;
import rc55.mc.ic2cr.addon.mets.block.storage.MetsChargepadBlock;
import rc55.mc.ic2cr.addon.mets.block.storage.MetsEnergyStorageBlock;
import rc55.mc.ic2cr.addon.mets.item.MetsItems;
import rc55.mc.ic2cr.addon.mets.item.block.MetsEnergyStorageBlockItem;

import java.util.function.BiFunction;
import java.util.function.Function;

public class MetsBlocks {
    public static final Block NIOBIUM_ORE = register("niobium_ore", AbstractBlock.Settings.copy(Blocks.IRON_ORE));
    public static final Block DEEPSLATE_NIOBIUM_ORE = register("deepslate_niobium_ore", AbstractBlock.Settings.copy(Blocks.DEEPSLATE_IRON_ORE));
    public static final Block TITANIUM_ORE = register("titanium_ore", AbstractBlock.Settings.copy(Blocks.IRON_ORE));
    public static final Block DEEPSLATE_TITANIUM_ORE = register("deepslate_titanium_ore", AbstractBlock.Settings.copy(Blocks.DEEPSLATE_IRON_ORE));
    public static final Block TITANIUM_BLOCK = register("titanium_block", AbstractBlock.Settings.copy(Blocks.IRON_BLOCK));

    public static final MetsEnergyStorageBlock LESU = register("lesu",
            MetsEnergyStorageBlock::createAsLESU,
            null,
            MetsEnergyStorageBlockItem::new,
            new Item.Settings()
    );

    public static final MetsChargepadBlock LESU_CHARGEPAD = register(
            "chargepad_lesu",
            MetsChargepadBlock::createAsLESU,
            null,
            MetsEnergyStorageBlockItem::new,
            new Item.Settings()
    );

    public static final MetsEnergyStorageBlock EESU = register("eesu",
            MetsEnergyStorageBlock::createAsEESU,
            null,
            MetsEnergyStorageBlockItem::new,
            new Item.Settings()
    );

    public static final MetsChargepadBlock EESU_CHARGEPAD = register(
            "chargepad_eesu",
            MetsChargepadBlock::createAsEESU,
            null,
            MetsEnergyStorageBlockItem::new,
            new Item.Settings()
    );

    private static <S extends AbstractBlock.Settings> Block register(
            String id,
            S settings
    ) {
        return register(id, Block::new, settings, true);
    }

    private static <T extends Block, S extends AbstractBlock.Settings> T register(
            String id,
            Function<S, T> blockFactory,
            S settings
    ) {
        return register(id, blockFactory, settings, true);
    }

    private static <T extends Block, S extends AbstractBlock.Settings> T register(
            String id,
            Function<S, T> blockFactory,
            S settings,
            boolean withItem
    ) {
        return withItem ? register(id, blockFactory, settings, BlockItem::new, new Item.Settings()) : register(id, blockFactory, settings, null, null);
    }

    private static <T extends Block, S extends AbstractBlock.Settings> T register(
            String id,
            Function<S, T> blockFactory,
            S settings,
            @Nullable BiFunction<T, ? super Item.Settings, ? extends Item> blockItemFactory,
            @Nullable Item.Settings itemSettings
    ) {
        T block = Registry.register(Registries.BLOCK, Identifier.of(MoreElectricTools.MODID, id), blockFactory.apply(settings));
        if (blockItemFactory != null && itemSettings != null) {
            MetsItems.registerBlockItem(block, blockItemFactory::apply, itemSettings);
        }
        return block;
    }

    public static void init() {
    }
}
