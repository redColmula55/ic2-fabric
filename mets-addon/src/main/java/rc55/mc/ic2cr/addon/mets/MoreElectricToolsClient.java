package rc55.mc.ic2cr.addon.mets;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import rc55.mc.ic2cr.addon.mets.client.render.MetsRenderer;
import rc55.mc.ic2cr.addon.mets.client.screen.MetsHandledScreens;

@Environment(EnvType.CLIENT)
public class MoreElectricToolsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MetsRenderer.init();
        MetsHandledScreens.init();
    }
}
