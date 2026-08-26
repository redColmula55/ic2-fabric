package rc55.mc.ic2cr.addon.mets.fluid;

import net.minecraft.util.Identifier;
import rc55.mc.ic2cr.addon.mets.MoreElectricTools;
import rc55.mc.ic2cr.addon.mets.item.MetsItems;
import rc55.mc.rfapi.fluid.ExtendedFluid;
import rc55.mc.rfapi.fluid.FluidReference;
import rc55.mc.rfapi.fluid.FluidRegistry;
import rc55.mc.rfapi.fluid.FluidSettings;

import java.util.Objects;

public class MetsFluids {
    public static final FluidReference<ExtendedFluid> CRUDE_OIL = FluidRegistry.registerSimple(
            Objects.requireNonNull(Identifier.of(MoreElectricTools.MODID, "crude_oil")),
            FluidSettings.lavaLike().bucket(() -> MetsItems.CRUDE_OIL_BUCKET)
                    .color(FluidSettings.ColorSettings.builder().fixedColor(0xFFFFFF).fog(FluidSettings.ColorSettings.FogType.LAVA, 0x1E1E1E).itemColor(0x1E1E1E))
                    .luminance(0)
                    .setsFire(false)
                    .ticksRandomly(false)
                    .isInfinite(false)
                    .temperature(300)
    );

    public static final FluidReference<ExtendedFluid> DIESEL_OIL = FluidRegistry.registerSimple(
            Objects.requireNonNull(Identifier.of(MoreElectricTools.MODID, "diesel_oil")),
            FluidSettings.lavaLike().bucket(() -> MetsItems.DIESEL_OIL_BUCKET)
                    .color(FluidSettings.ColorSettings.builder().fixedColor(0xFFFFFF).fog(FluidSettings.ColorSettings.FogType.LAVA, 0x2F2F00).itemColor(0xD5C608))
                    .luminance(0)
                    .setsFire(false)
                    .isInfinite(false)
                    .temperature(300)
                    .ticksRandomly(false)
    );

    public static void init() {
    }
}
