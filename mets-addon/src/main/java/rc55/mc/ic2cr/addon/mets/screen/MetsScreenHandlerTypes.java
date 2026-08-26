package rc55.mc.ic2cr.addon.mets.screen;

import ic2_120.content.screen.EnergyStorageScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import rc55.mc.ic2cr.addon.mets.MoreElectricTools;

public class MetsScreenHandlerTypes {
    public static final ScreenHandlerType<EnergyStorageScreenHandler> LESU = register("lesu", new ExtendedScreenHandlerType<>(
            EnergyStorageScreenHandler.Companion::fromBuffer
    ));

    public static final ScreenHandlerType<EnergyStorageScreenHandler> LESU_CHARGEPAD = register("chargepad_lesu", new ExtendedScreenHandlerType<>(
            EnergyStorageScreenHandler.Companion::fromBuffer
    ));

    public static final ScreenHandlerType<EnergyStorageScreenHandler> EESU = register("eesu", new ExtendedScreenHandlerType<>(
            EnergyStorageScreenHandler.Companion::fromBuffer
    ));

    public static final ScreenHandlerType<EnergyStorageScreenHandler> EESU_CHARGEPAD = register("chargepad_eesu", new ExtendedScreenHandlerType<>(
            EnergyStorageScreenHandler.Companion::fromBuffer
    ));

    private static <T extends ScreenHandler> ScreenHandlerType<T> register(String id, ScreenHandlerType<T> type) {
        return Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MoreElectricTools.MODID, id), type);
    }

    private static <T extends ScreenHandler> ScreenHandlerType<T> register(String id, ScreenHandlerType.Factory<T> factory) {
        return Registry.register(Registries.SCREEN_HANDLER, Identifier.of(MoreElectricTools.MODID, id), new ScreenHandlerType<>(factory, FeatureSet.of(FeatureFlags.VANILLA)));
    }

    public static void init() {
    }
}
