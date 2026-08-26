package rc55.mc.ic2cr.addon.mets;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rc55.mc.ic2cr.addon.mets.block.MetsBlocks;
import rc55.mc.ic2cr.addon.mets.blockentity.MetsBlockEntityTypes;
import rc55.mc.ic2cr.addon.mets.fluid.MetsFluids;
import rc55.mc.ic2cr.addon.mets.item.MetsItems;
import rc55.mc.ic2cr.addon.mets.screen.MetsScreenHandlerTypes;

public class MoreElectricTools implements ModInitializer {
    public static final String MODID = "ic2cr-mets-addon";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    static final ConfigHolder<MoreElectricToolsConfig> CONFIG_HOLDER = AutoConfig.register(MoreElectricToolsConfig.class, GsonConfigSerializer::new);

    @Override
    public void onInitialize() {
        MetsFluids.init();
        MetsBlocks.init();
        MetsItems.init();
        MetsBlockEntityTypes.init();
        MetsScreenHandlerTypes.init();
        LOGGER.info("METS init complete.");
    }
}
