package frootloops.versus.mixin.items.elytra;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(FireworkRocketEntity.class)
public abstract class ElytraFireworksSpeedMixin extends ProjectileEntity  {
    public ElytraFireworksSpeedMixin(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow private int lifeTime;
    @Shadow @Nullable private LivingEntity shooter;

    @Override
    public void setOwner(@Nullable Entity entity) {
        super.setOwner(entity);
        if(entity != null && entity instanceof LivingEntity livingEntity && livingEntity.isFallFlying()) {

            // More of a difference between flight 1 and flight 3 (elytra only):
            // Flight 1: Average 16 -> Average 7
            // Flight 2: Average 26 -> Average 17
            // Flight 3: Average 36 -> Average 35 (not nerfed, but expensive!)
            float lifeTimeFl = (float) lifeTime;
            lifeTime = (int) (0.5 * lifeTimeFl * ((lifeTimeFl + 20) * (lifeTimeFl + 20) / 1600f));
        }
    }
}
