package frootloops.versus.mixin.mobs.hostile.illager;

import frootloops.versus.mod.hostile_mobs.ai.FleeAttackerAndHealGoal;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VindicatorEntity.class)
public abstract class VindicatorMixin extends IllagerEntity {
    protected VindicatorMixin(EntityType<? extends IllagerEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initialize", at = @At("HEAD"))
    private void increaseAttributes(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, NbtCompound entityTag, CallbackInfoReturnable<EntityData> cir) {
        EntityAttributeInstance instanceDmg = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (instanceDmg != null) instanceDmg.setBaseValue(3.0);

        EntityAttributeInstance instanceHP = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (instanceHP != null) {
            instanceHP.setBaseValue(30.0D);
            this.setHealth(this.getMaxHealth());
        }
    }

    @Inject(method = "initGoals", at = @At("HEAD"))
    private void vindicatorsCanHeal(CallbackInfo ci) {
        this.goalSelector.add(1, new FleeAttackerAndHealGoal<>(this, 1));
    }
}