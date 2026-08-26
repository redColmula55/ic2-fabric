package rc55.mc.ic2cr.addon.mets.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import rc55.mc.ic2cr.addon.mets.MoreElectricTools;
import rc55.mc.ic2cr.addon.mets.block.MetsBlocks;

public class MetsItemGroups {
    public static final RegistryKey<ItemGroup> METS_KEY = keyFor("mets");

    public static final ItemGroup METS_GROUP = register(METS_KEY, FabricItemGroup.builder()
            .icon(MetsItems.ADVANCED_IRIDIUM_SWORD::getFullPowerStack)
            .entries((displayContext, entries) -> {
                entries.add(MetsBlocks.NIOBIUM_ORE);
                entries.add(MetsBlocks.DEEPSLATE_NIOBIUM_ORE);
                entries.add(MetsItems.RAW_NIOBIUM);
                entries.add(MetsItems.CRUSHED_NIOBIUM_ORE);
                entries.add(MetsItems.NIOBIUM_DUST);
                entries.add(MetsBlocks.TITANIUM_ORE);
                entries.add(MetsBlocks.DEEPSLATE_TITANIUM_ORE);
                entries.add(MetsItems.RAW_TITANIUM);
                entries.add(MetsItems.CRUSHED_TITANIUM_ORE);
                entries.add(MetsItems.TITANIUM_DUST);
                entries.add(MetsItems.TITANIUM_INGOT);
                entries.add(MetsBlocks.TITANIUM_BLOCK);
                entries.add(MetsItems.TITANIUM_PLATE);
                entries.add(MetsItems.TITANIUM_CASING);
                entries.add(MetsItems.TITANIUM_SHAFT);
                entries.add(MetsItems.NIOBIUM_TITANIUM_DUST);
                entries.add(MetsItems.NIOBIUM_TITANIUM_INGOT);
                entries.add(MetsItems.NIOBIUM_TITANIUM_PLATE);
                entries.add(MetsItems.LIVING_CIRCUIT);
                entries.add(MetsItems.SUPER_CIRCUIT);
                entries.add(MetsItems.NANO_LIVING_METAL);
                entries.add(MetsItems.NEUTRON_PLATE);
                entries.add(MetsItems.THORIUM_DUST);
                entries.add(MetsItems.THORIUM_PILE);
                entries.add(MetsItems.SUPER_IRIDIUM_ALLOY);
                entries.add(MetsItems.SUPER_IRIDIUM_COMPRESS_PLATE);
                entries.add(MetsItems.LENS);
                entries.add(MetsItems.DIAMOND_LENS);
                entries.add(MetsItems.FIELD_GENERATOR);
                entries.add(MetsItems.PLANT_EXTRACT);
                entries.add(MetsItems.SUPERCONDUCTING_CABLE);

                entries.add(MetsBlocks.LESU);
                entries.add(MetsBlocks.LESU_CHARGEPAD);
                entries.add(MetsBlocks.EESU);
                entries.add(MetsBlocks.EESU_CHARGEPAD);

                entries.add(MetsItems.ADVANCED_IRIDIUM_SWORD);
                entries.add(MetsItems.ADVANCED_IRIDIUM_SWORD.getFullPowerStack());

                entries.add(MetsItems.CRUDE_OIL_BUCKET);
                entries.add(MetsItems.DIESEL_OIL_BUCKET);
            })
    );

    private static RegistryKey<ItemGroup> keyFor(String id) {
        return RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MoreElectricTools.MODID, id));
    }

    private static ItemGroup register(RegistryKey<ItemGroup> key, ItemGroup.Builder builder) {
        return Registry.register(Registries.ITEM_GROUP, key, builder.displayName(createTranslation(key)).build());
    }

    private static MutableText createTranslation(RegistryKey<ItemGroup> key) {
        return Text.translatable(key.getValue().toTranslationKey("itemGroup"));
    }

    static void init() {
    }
}
