package frootloops.versus.mixin.items.throwing;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(ThrowablePotionItem.class)
public abstract class ThrowablePotionItemMixin extends PotionItem implements ProjectileItem {
    public ThrowablePotionItemMixin(Item.Settings settings) {
        super(settings);
    }

    @Override
    public void onStoppedUsing(ItemStack itemStack, World world, LivingEntity user, int remainingUseTicks) {
        if (user instanceof PlayerEntity playerEntity) {
            float pullProgress = Math.max(2.0F, (0.9F + (float)(this.getMaxUseTime(itemStack, user) - remainingUseTicks) / 32.0F));
            throwPotion(world, user, itemStack, 0.5f * pullProgress);
            world.playSound(null, playerEntity.getX(), playerEntity.getY(), playerEntity.getZ(), SoundEvents.ENTITY_WITCH_THROW, SoundCategory.PLAYERS, 1.0f, 1.0f / (world.getRandom().nextFloat() * 0.4f + 1.2f) + pullProgress * 0.5f);
            playerEntity.incrementStat(Stats.USED.getOrCreateStat(this));
            playerEntity.getItemCooldownManager().set(Items.SPLASH_POTION, 20);
            playerEntity.getItemCooldownManager().set(Items.LINGERING_POTION, 20);

        }
        else throwPotion(world, user, itemStack, 0.5f);
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
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);
        return TypedActionResult.consume(user.getStackInHand(hand));
    }

    private static void throwPotion(World world, LivingEntity user, ItemStack stack, float velocity) {
        if (!world.isClient) {
            PotionEntity potionEntity = new PotionEntity(world, user);
            potionEntity.setItem(stack);
            potionEntity.setVelocity(user, user.getPitch(), user.getYaw(), -20.0f, 0.5f * velocity, 1.0f);
            world.spawnEntity(potionEntity);
        }
    }
}
