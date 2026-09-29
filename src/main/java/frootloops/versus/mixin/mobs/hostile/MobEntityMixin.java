package frootloops.versus.mixin.mobs.hostile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobEntityMixin extends LivingEntity {

    @Shadow @Nullable private LivingEntity target;
    @Shadow private Sensing sensing;

    protected MobEntityMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "startRiding", at = @At("HEAD"), cancellable = true)
    public void startRiding(Entity entity, boolean force, boolean emitEvent, CallbackInfoReturnable<Boolean> cir) {
        if(this.hurtTime > 0) cir.setReturnValue(force);
        else if(entity instanceof Boat && target != null && sensing.hasLineOfSight(target)) cir.setReturnValue(false);
    }

    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        if (this.isPassenger() && !(this.getVehicle() instanceof LivingEntity)) this.stopRiding();
        return super.hurtServer(world, source, amount);
    }

    @Redirect(at=@At(value = "INVOKE", target="Lnet/minecraft/world/DifficultyInstance;getSpecialMultiplier()F"), method= "populateDefaultEquipmentSlots(Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/DifficultyInstance;)V")
    private float harderFartherAndDeeper(DifficultyInstance localDifficulty) {
        float distanceMultiplier = ((float)(this.blockPosition().getX() - this.level().getRespawnData().pos().getX()))/512f + ((float)(this.blockPosition().getZ() - this.level().getRespawnData().pos().getZ()))/512f;
        float depthBonus = 160.0f/Math.abs((float)this.position().y - 64f);
        return (localDifficulty.getSpecialMultiplier() + depthBonus) * distanceMultiplier;
    }

}
