package rc55.mc.ic2cr.addon.mets.item.weapon;

import com.google.common.base.Predicates;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import rc55.mc.ic2cr.addon.mets.MoreElectricToolsConfig;
import rc55.mc.ic2cr.addon.mets.item.AbstractElectricItem;

public class AdvancedIridiumSwordItem extends AbstractElectricItem {
	private final static long maxStorageEU = 10000000, transferSpeed = 2048;
	private final static double sweepingDistance = 12d;

	public AdvancedIridiumSwordItem(Settings settings) {
		super(maxStorageEU, transferSpeed, 4, settings);
	}

    private float getAttackDamage(ItemStack stack) {
    	boolean isHyperState = getHyperState(stack);
    	float damage = MoreElectricToolsConfig.getInstance().AdvancedIridiumSwordBaseAttackDamage;
		if (isHyperState) {
			int level = EnchantmentHelper.getLevel(Enchantments.SHARPNESS, stack);
			damage *= (level == 0) ? 1.5f : (level + 1);
		}
        return damage;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // won't work while attacker != player
        if(!(attacker instanceof PlayerEntity) || attacker.getEntityWorld().isClient)
            return true;

        boolean isHyperState = getHyperState(stack);
        float ratio = getElectricItemAttenuationRatio(stack);
        double attackCost = (MoreElectricToolsConfig.getInstance().AdvancedIridiumSwordBaseCost * (isHyperState ? 1.5d : 1.0d)) * ratio;
        if (getEnergy(stack) > attackCost) {//ElectricItem.manager.canUse(stack, attackCost)
            //ElectricItem.manager.discharge(stack, attackCost, 4, true, false, false);
            setEnergy(stack, getEnergy(stack) - 4);
            float attackDamage = this.getAttackDamage(stack);
            //sweeping edge
            if (isHyperState && attacker instanceof PlayerEntity player) {
                World currentWorld = player.getEntityWorld();
                for (LivingEntity living : currentWorld.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(1.0D, 0.25D, 1.0D), Predicates.alwaysTrue())) {
                    if (living != player && living != target && player.distanceTo(living) < sweepingDistance) {
                        living.takeKnockback(0.4F, MathHelper.sin(player.headYaw * 0.017f), -MathHelper.cos(player.headYaw * 0.017f));
                        living.damage(player.getDamageSources().playerAttack(player), attackDamage / 2.0f);
                    }
                }
                player.playSound(SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, player.getSoundCategory(), 1f, 1f);
            }

            target.damage(attacker.getDamageSources().mobAttack(attacker), attackDamage);
        }
        return true;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack currentSword = user.getStackInHand(hand);

        long lastRightClick = getLastRightClick(currentSword);
        long currentTime = System.currentTimeMillis();
        boolean isHyperState = getHyperState(currentSword);

        if (currentTime - lastRightClick > 100) {
            isHyperState = !isHyperState;
            lastRightClick = currentTime;

            setHyperState(currentSword, isHyperState);
            setLastRightClick(currentSword, lastRightClick);
        }

        return TypedActionResult.success(currentSword);
    }

    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        return state.isIn(BlockTags.SWORD_EFFICIENT) ? 18f : super.getMiningSpeedMultiplier(stack, state);
    }

    private void setHyperState(ItemStack stack, boolean state) {
        stack.getOrCreateNbt().putBoolean("isHyperState", state);
    }

	private boolean getHyperState(ItemStack stack) {
		return stack.getOrCreateNbt().getBoolean("isHyperState");
	}

	private void setHyperValue(ItemStack stack, float value) {
        stack.getOrCreateNbt().putFloat("HyperValue", value);
    }

	private float getHyperValue(ItemStack stack) {
		return stack.getOrCreateNbt().getFloat("HyperValue");
	}
}
