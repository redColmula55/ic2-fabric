package rc55.mc.ic2cr.addon.mets.client.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import rc55.mc.ic2cr.addon.mets.screen.MetsScreenHandlerTypes;

@Environment(EnvType.CLIENT)
public class MetsHandledScreens {
    public static void init() {
        HandledScreens.register(MetsScreenHandlerTypes.LESU, MetsEnergyStorageScreen::new);
        HandledScreens.register(MetsScreenHandlerTypes.EESU, MetsEnergyStorageScreen::new);
        HandledScreens.register(MetsScreenHandlerTypes.LESU_CHARGEPAD, MetsEnergyStorageScreen::new);
        HandledScreens.register(MetsScreenHandlerTypes.EESU_CHARGEPAD, MetsEnergyStorageScreen::new);
    }
}
