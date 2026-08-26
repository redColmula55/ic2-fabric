package rc55.mc.ic2cr.addon.mets.datagen;

import com.google.common.collect.ImmutableList;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.block.Block;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.book.RecipeCategory;
import rc55.mc.ic2cr.addon.mets.block.MetsBlocks;
import rc55.mc.ic2cr.addon.mets.item.MetsItems;

import java.util.List;
import java.util.function.Consumer;

public class MetsRecipeDataGen extends FabricRecipeProvider {
    public MetsRecipeDataGen(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate(Consumer<RecipeJsonProvider> consumer) {
        generateOreSmelting(consumer,
                MetsBlocks.TITANIUM_ORE, MetsBlocks.DEEPSLATE_TITANIUM_ORE, MetsItems.TITANIUM_DUST, MetsItems.RAW_TITANIUM,
                "titanium_ingot_smelt", MetsItems.TITANIUM_INGOT
        );

        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, MetsItems.NIOBIUM_TITANIUM_DUST, 2)
                .input(MetsItems.NIOBIUM_DUST).input(MetsItems.TITANIUM_DUST, 3)
                .criterion(hasItem(MetsItems.NIOBIUM_DUST), conditionsFromItem(MetsItems.NIOBIUM_DUST))
                .offerTo(consumer);
    }

    private static void generateOreSmelting(
            Consumer<RecipeJsonProvider> consumer, Block ore, Block deepslateOre, Item raw, Item dust, String name, Item result
    ) {
        final List<ItemConvertible> list = ImmutableList.of(ore, deepslateOre, raw, dust);
        offerSmelting(consumer, list, RecipeCategory.MISC, result, 0.7f, 200, name);
        offerBlasting(consumer, list, RecipeCategory.MISC, result, 0.7f, 100, name);
    }
}
