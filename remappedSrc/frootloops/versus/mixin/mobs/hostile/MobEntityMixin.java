package frootloops.versus.mixin.mobs.hostile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.MobVisibilityCache;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public abstract class MobEntityMixin extends LivingEntity {

    @Shadow @Nullable private LivingEntity target;
    @Shadow private MobVisibilityCache visibilityCache;

    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "startRiding", at = @At("HEAD"), cancellable = true)
    public void startRiding(Entity entity, boolean force, boolean emitEvent, CallbackInfoReturnable<Boolean> cir) {
        if(this.hurtTime > 0) cir.setReturnValue(force);
        else if(entity instanceof BoatEntity && target != null && visibilityCache.canSee(target)) cir.setReturnValue(false);
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (this.hasVehicle() && !(this.getVehicle() instanceof LivingEntity)) this.stopRiding();
        return super.damage(world, source, amount);
    }

    @Redirect(at=@At(value = "INVOKE", target="Lnet/minecraft/world/LocalDifficulty;getClampedLocalDifficulty()F"), method= "Lnet/minecraft/entity/mob/MobEntity;initEquipment(Lnet/minecraft/util/math/random/Random;Lnet/minecraft/world/LocalDifficulty;)V")
    private float harderFartherAndDeeper(LocalDifficulty localDifficulty) {
        float distanceMultiplier = ((float)(this.getBlockPos().getX() - this.getEntityWorld().getSpawnPoint().getPos().getX()))/512f + ((float)(this.getBlockPos().getZ() - this.getEntityWorld().getSpawnPoint().getPos().getZ()))/512f;
        float depthBonus = 160.0f/Math.abs((float)this.getEntityPos().y - 64f);
        return (localDifficulty.getClampedLocalDifficulty() + depthBonus) * distanceMultiplier;
    }

}
