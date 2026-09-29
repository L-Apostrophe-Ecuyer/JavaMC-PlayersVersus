package frootloops.versus.mixin.items_and_effects.throwing;

import frootloops.versus.mod.items_and_effects.throwing.SlimeballEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class SlimeballsAndFireChargesMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    public void use(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack itemStack = user.getItemInHand(hand);
        if(itemStack.is(Items.SLIME_BALL)) {
            throwSnowball(world, user, itemStack);
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
        else if(itemStack.is(Items.FIRE_CHARGE)) {
            throwFireCharge(world, user, itemStack);
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    private static void throwSnowball(Level world, Player user, ItemStack itemStack) {
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.EGG_THROW, SoundSource.NEUTRAL, 0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!world.isClientSide()) {
            SlimeballEntity slimeballEntity = new SlimeballEntity(world, user, itemStack);
            slimeballEntity.shootFromRotation(user, user.getXRot(), user.getYRot(), 0.0f, 1.5f, 1.0f);
            world.addFreshEntity(slimeballEntity);
        }
        user.awardStat(Stats.ITEM_USED.get(Items.SLIME_BALL));
        if (!user.getAbilities().instabuild) {
            itemStack.shrink(1);
            user.getCooldowns().addCooldown(itemStack, 8);
        }
    }


    private static void throwFireCharge(Level world, Player user, ItemStack itemStack) {
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 0.5f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!world.isClientSide()) {
            double vx = -Mth.sin(user.getYRot() * ((float)Math.PI / 180)) * Mth.cos(user.getXRot() * ((float)Math.PI / 180));
            double vy = -Mth.sin((user.getXRot()) * ((float)Math.PI / 180));
            double vz = Mth.cos(user.getYRot() * ((float)Math.PI / 180)) * Mth.cos(user.getXRot() * ((float)Math.PI / 180));
            LargeFireball fireballEntity = new LargeFireball(world, user, new Vec3(vx, vy, vz), 1);
            fireballEntity.setPos(user.getEyePosition().add(0.0, -0.05, 0.0));
            world.addFreshEntity(fireballEntity);
        }
        user.awardStat(Stats.ITEM_USED.get(Items.FIRE_CHARGE));
        if (!user.getAbilities().instabuild) {
            itemStack.shrink(1);
            user.getCooldowns().addCooldown(itemStack, 20);
        }
    }

}
