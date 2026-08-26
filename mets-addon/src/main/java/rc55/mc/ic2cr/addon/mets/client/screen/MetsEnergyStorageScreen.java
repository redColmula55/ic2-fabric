package rc55.mc.ic2cr.addon.mets.client.screen;

import ic2_120.client.screen.EnergyStorageScreen;
import ic2_120.content.block.storage.EnergyStorageConfig;
import ic2_120.content.screen.EnergyStorageScreenHandler;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import rc55.mc.ic2cr.addon.mets.blockentity.storage.MetsEnergyStorageBlockEntity;

public class MetsEnergyStorageScreen extends EnergyStorageScreen {
    public MetsEnergyStorageScreen(@NotNull EnergyStorageScreenHandler handler, @NotNull PlayerInventory playerInventory, @NotNull Text title) {
        super(handler, playerInventory, title);
    }

    @Override
    protected long resolveCapacity() {
        return this.handler.getContext().get((world, pos) -> {
            final BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof MetsEnergyStorageBlockEntity) {
                return  ((MetsEnergyStorageBlockEntity) blockEntity).getConfig().getCapacity();
            }
            return EnergyStorageConfig.Companion.getBATBOX().getCapacity();
        }, EnergyStorageConfig.Companion.getBATBOX().getCapacity());
    }
}
