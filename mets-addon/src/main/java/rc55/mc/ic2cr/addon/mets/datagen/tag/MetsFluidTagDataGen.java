package rc55.mc.ic2cr.addon.mets.datagen.tag;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.registry.RegistryWrapper;
import rc55.mc.ic2cr.addon.mets.fluid.MetsFluids;
import rc55.mc.rfapi.data.gen.AbstractFluidTagProvider;
import rc55.mc.rfapi.fluid.FluidReference;

import java.util.concurrent.CompletableFuture;

public class MetsFluidTagDataGen extends AbstractFluidTagProvider {
    public MetsFluidTagDataGen(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, completableFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        this.createBaseTagsForReference(FluidReference::getBlockId, MetsFluids.CRUDE_OIL, MetsFluids.DIESEL_OIL);
    }
}
