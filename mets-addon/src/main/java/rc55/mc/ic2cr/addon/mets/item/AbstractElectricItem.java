package rc55.mc.ic2cr.addon.mets.item;

import java.util.List;

import ic2_120.content.item.energy.IElectricTool;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AbstractElectricItem extends Item implements IElectricTool {
	private long maxStorageEU = 0, transferSpeed = 0;
	private int powerLevel = 0;
	
    public AbstractElectricItem(long maxEU, long tSpeed, int tier, Settings settings) {
        super(settings.maxCount(1));
		maxStorageEU = maxEU;
		transferSpeed = tSpeed;
		powerLevel = tier;
	}

	@Override
	public long getMaxCapacity() {
		return maxStorageEU;
	}

	@Override
	public int getTier()
	{
		return powerLevel;
	}

//	@Override
//	public double getTransferLimit(ItemStack stack) {
//		return transferSpeed;
//	}

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        this.appendEnergyTooltip(stack, tooltip);
    }

    public Item getChargedItem(ItemStack itemStack) {
        return this;
    }
    
    public Item getEmptyItem(ItemStack itemStack) {
        return this;
    }
    
	protected void setLastRightClick(ItemStack stack, long value) {
		stack.getOrCreateNbt().putLong("LastRightClick", value);
    }

	protected long getLastRightClick(ItemStack stack) {
		long value = 0;
		try {
			value = stack.getOrCreateNbt().getLong("LastRightClick");
		} catch (Exception ignored) {}
		return value;
	}

    @Override
    public long getEnergy(@NotNull ItemStack stack) {
        return IElectricTool.Companion.getEnergy(stack);
    }

    @Override
    public void setEnergy(@NotNull ItemStack stack, long energy) {
        IElectricTool.Companion.setEnergy(stack, energy, this.maxStorageEU);
    }

    public float getElectricItemAttenuationRatio(ItemStack stack) {
        // TODO: 附魔
        return 1f;//EfficientEnergyCost.getAttenuationRatio(EnchantmentHelper.getEnchantmentLevel(EnchantmentManager.efficientEu, stack));
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return this.getEnergy(stack) < this.getMaxEnergy();
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return this.getEnergyBarStep(stack);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        return this.getEnergyBarColor(stack);
    }

    public ItemStack getFullPowerStack() {
        ItemStack stack = new ItemStack(this);
        this.setEnergy(stack, this.maxStorageEU);
        return stack;
    }
}
