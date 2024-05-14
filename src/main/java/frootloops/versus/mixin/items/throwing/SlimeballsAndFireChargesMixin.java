package frootloops.versus.mixin.items.throwing;

import frootloops.versus.mod.items.throwing.SlimeballEntity;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class SlimeballsAndFireChargesMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    public void use(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        ItemStack itemStack = user.getStackInHand(hand);
        if(itemStack.isOf(Items.SLIME_BALL)) {
            throwSnowball(world, user, itemStack);
            cir.setReturnValue(TypedActionResult.success(itemStack, world.isClient()));
        }
        else if(itemStack.isOf(Items.FIRE_CHARGE)) {
            throwFireCharge(world, user, itemStack);
            cir.setReturnValue(TypedActionResult.success(itemStack, world.isClient()));
        }
    }

    private static void throwSnowball(World world, PlayerEntity user, ItemStack itemStack) {
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_EGG_THROW, SoundCategory.NEUTRAL, 0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!world.isClient) {
            SlimeballEntity slimeballEntity = new SlimeballEntity(world, user);
            slimeballEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 1.5f, 1.0f);
            world.spawnEntity(slimeballEntity);
        }
        user.incrementStat(Stats.USED.getOrCreateStat(Items.SLIME_BALL));
        if (!user.getAbilities().creativeMode) {
            itemStack.decrement(1);
            user.getItemCooldownManager().set(Items.SLIME_BALL, 8);
        }
    }


    private static void throwFireCharge(World world, PlayerEntity user, ItemStack itemStack) {
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.NEUTRAL, 0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!world.isClient) {
            double vx = -MathHelper.sin(user.getYaw() * ((float)Math.PI / 180)) * MathHelper.cos(user.getPitch() * ((float)Math.PI / 180));
            double vy = -MathHelper.sin((user.getPitch()) * ((float)Math.PI / 180));
            double vz = MathHelper.cos(user.getYaw() * ((float)Math.PI / 180)) * MathHelper.cos(user.getPitch() * ((float)Math.PI / 180));
            FireballEntity fireballEntity = new FireballEntity(world, user, new Vec3d(vx, vy, vz), 1);
            fireballEntity.setPosition(user.getEyePos().add(0.0, -0.05, 0.0));
            world.spawnEntity(fireballEntity);
        }
        user.incrementStat(Stats.USED.getOrCreateStat(Items.FIRE_CHARGE));
        if (!user.getAbilities().creativeMode) {
            itemStack.decrement(1);
            user.getItemCooldownManager().set(Items.FIRE_CHARGE, 20);
        }
    }

}
