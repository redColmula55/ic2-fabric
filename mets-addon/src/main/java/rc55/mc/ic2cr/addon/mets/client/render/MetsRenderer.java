package rc55.mc.ic2cr.addon.mets.client.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;
import rc55.mc.ic2cr.addon.mets.MoreElectricTools;
import rc55.mc.ic2cr.addon.mets.fluid.MetsFluids;
import rc55.mc.rfapi.client.FluidRenderRegistry;
import rc55.mc.rfapi.fluid.FluidReference;

import java.util.Objects;

@Environment(EnvType.CLIENT)
public class MetsRenderer {
    public static void init() {
        initFluidRenderers();
    }

    private static void initFluidRenderers() {
        regFluidRenderer(MetsFluids.CRUDE_OIL, "crude_oil");
        regFluidRenderer(MetsFluids.DIESEL_OIL, "diesel_oil");
    }

    private static void regFluidRenderer(FluidReference<?> fluid, String texId) {
        FluidRenderRegistry.register(
                fluid,
                Objects.requireNonNull(Identifier.of(MoreElectricTools.MODID, "block/fluid/%s_still".formatted(texId))),
                Objects.requireNonNull(Identifier.of(MoreElectricTools.MODID, "block/fluid/%s_flow".formatted(texId))),
                null
        );
    }
}
