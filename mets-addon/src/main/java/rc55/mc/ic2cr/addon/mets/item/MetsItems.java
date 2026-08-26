package rc55.mc.ic2cr.addon.mets.item;

import net.minecraft.block.Block;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import rc55.mc.ic2cr.addon.mets.MoreElectricTools;
import rc55.mc.ic2cr.addon.mets.fluid.MetsFluids;
import rc55.mc.ic2cr.addon.mets.item.weapon.AdvancedIridiumSwordItem;
import rc55.mc.rfapi.item.BucketItemRegistry;

import java.util.function.BiFunction;
import java.util.function.Function;

public class MetsItems {
    // ===== 材料 ======
    // 铌
    public static final Item RAW_NIOBIUM = register("raw_niobium");
    public static final Item CRUSHED_NIOBIUM_ORE = register("crushed_niobium");
    public static final Item NIOBIUM_DUST = register("niobium_dust");
    // 钛
    public static final Item RAW_TITANIUM = register("raw_titanium");
    public static final Item CRUSHED_TITANIUM_ORE = register("crushed_titanium");
    public static final Item TITANIUM_DUST = register("titanium_dust");
    public static final Item TITANIUM_INGOT = register("titanium_ingot");
    public static final Item TITANIUM_PLATE = register("titanium_plate");
    public static final Item TITANIUM_CASING = register("titanium_casing");
    public static final Item TITANIUM_SHAFT = register("titanium_shaft");
    // 铌钛合金
    public static final Item NIOBIUM_TITANIUM_DUST = register("niobium_titanium_dust");
    public static final Item NIOBIUM_TITANIUM_INGOT = register("niobium_titanium_ingot");
    public static final Item NIOBIUM_TITANIUM_PLATE = register("niobium_titanium_plate");
    // 钍
    public static final Item THORIUM_DUST = register("thorium_dust");
    public static final Item THORIUM_PILE = register("thorium_pile");
    // 铱
    public static final Item SUPER_IRIDIUM_ALLOY = register("super_iridium_alloy");
    public static final Item SUPER_IRIDIUM_COMPRESS_PLATE = register("super_iridium_compress_plate");
    // 其他
    public static final Item NANO_LIVING_METAL = register("nano_living_metal");
    public static final Item NEUTRON_PLATE = register("neutron_plate");
    public static final Item SUPER_CIRCUIT = register("super_circuit");
    public static final Item LIVING_CIRCUIT = register("living_circuit");
    public static final Item LENS = register("lens");
    public static final Item DIAMOND_LENS = register("diamond_lens");
    public static final Item PLANT_EXTRACT = register("plant_extract");
    public static final Item FIELD_GENERATOR = register("field_generator");
    public static final Item SUPERCONDUCTING_CABLE = register("superconducting_cable");

    // ===== 武器 =====
    public static final AdvancedIridiumSwordItem ADVANCED_IRIDIUM_SWORD = register("advanced_iridium_sword", AdvancedIridiumSwordItem::new, new Item.Settings());

    // ===== 杂项 =====
    public static final BucketItem CRUDE_OIL_BUCKET = BucketItemRegistry.registerVanilla(
            Identifier.of(MoreElectricTools.MODID, "crude_oil_bucket"), MetsFluids.CRUDE_OIL, new Item.Settings().maxCount(16)
    );

    public static final BucketItem DIESEL_OIL_BUCKET = BucketItemRegistry.registerVanilla(
            Identifier.of(MoreElectricTools.MODID, "diesel_oil_bucket"), MetsFluids.DIESEL_OIL, new Item.Settings().maxCount(16)
    );

    private static Item register(String id) {
        return register(id, Item::new, new Item.Settings());
    }

    private static <T extends Item, S extends Item.Settings> T register(String id, Function<S, T> itemFactory, S settings) {
        return Registry.register(Registries.ITEM, Identifier.of(MoreElectricTools.MODID, id), itemFactory.apply(settings));
    }

    public static <B extends Block, I extends Item, S extends Item.Settings> I registerBlockItem(
            B block, BiFunction<B, S, I> itemFactory, S settings
    ) {
        return Registry.register(Registries.ITEM, Registries.BLOCK.getId(block), itemFactory.apply(block, settings));
    }

    public static void init() {
        MetsItemGroups.init();
    }
}
