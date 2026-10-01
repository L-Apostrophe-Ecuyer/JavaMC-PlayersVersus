package frootloops.versus.mixin.items_and_effects.throwing;

import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(ThrowablePotionItem.class)
public abstract class ThrowablePotionItemMixin extends PotionItem implements ProjectileItem {
    public ThrowablePotionItemMixin(Item.Properties settings) {
        super(settings);
    }

    @Override
    public boolean releaseUsing(ItemStack itemStack, Level world, LivingEntity user, int remainingUseTicks) {
        if (user instanceof Player playerEntity) {
            float pullProgress = Math.min(0.8F, (72000.0F - remainingUseTicks) / 64.0F);
            throwPotion(world, user, itemStack, 0.4f + pullProgress);
            playerEntity.awardStat(Stats.ITEM_USED.get(this));
            playerEntity.getCooldowns().addCooldown(itemStack, 20);
            itemStack.consume(1, user);
        }
        else throwPotion(world, user, itemStack, 0.5f);
        return false;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        user.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    private static void throwPotion(Level world, LivingEntity user, ItemStack stack, float velocity) {
        if (!world.isClientSide()) {
            AbstractThrownPotion potionEntity;
            if(stack.is(Items.SPLASH_POTION)) potionEntity = new ThrownSplashPotion(world, user, stack);
            else if(stack.is(Items.LINGERING_POTION)) potionEntity = new ThrownLingeringPotion(world, user, stack);
            else return;

            potionEntity.setItem(stack);
            potionEntity.shootFromRotation(user, user.getXRot(), user.getYRot(), -20.0f, velocity, 1.0f);
            world.addFreshEntity(potionEntity);
        }
    }
}
