package frootloops.versus.mixin.items_and_effects.throwing;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.LingeringPotionEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SplashPotionEntity;
import net.minecraft.item.*;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.item.consume.UseAction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(ThrowablePotionItem.class)
public abstract class ThrowablePotionItemMixin extends PotionItem implements ProjectileItem {
    public ThrowablePotionItemMixin(Item.Settings settings) {
        super(settings);
    }

    @Override
    public boolean onStoppedUsing(ItemStack itemStack, World world, LivingEntity user, int remainingUseTicks) {
        if (user instanceof PlayerEntity playerEntity) {
            float pullProgress = Math.min(0.8F, (72000.0F - remainingUseTicks) / 64.0F);
            throwPotion(world, user, itemStack, 0.4f + pullProgress);
            playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
            playerEntity.getItemCooldownManager().set(itemStack, 20);
            itemStack.decrementUnlessCreative(1, user);
        }
        else throwPotion(world, user, itemStack, 0.5f);
        return false;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }

    private static void throwPotion(World world, LivingEntity user, ItemStack stack, float velocity) {
        if (!world.isClient) {
            PotionEntity potionEntity;
            if(stack.isOf(Items.SPLASH_POTION)) potionEntity = new SplashPotionEntity(world, user, stack);
            else if(stack.isOf(Items.LINGERING_POTION)) potionEntity = new LingeringPotionEntity(world, user, stack);
            else return;

            potionEntity.setItem(stack);
            potionEntity.setVelocity(user, user.getPitch(), user.getYaw(), -20.0f, velocity, 1.0f);
            world.spawnEntity(potionEntity);
        }
    }
}
