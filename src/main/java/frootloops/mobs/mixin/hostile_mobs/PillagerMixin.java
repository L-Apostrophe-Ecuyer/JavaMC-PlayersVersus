package frootloops.mobs.mixin.hostile_mobs;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(PillagerEntity.class)
public abstract class PillagerMixin extends IllagerEntity {
    protected PillagerMixin(EntityType<? extends IllagerEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initialize", at = @At("HEAD"))
    private void increaseAttributes(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, NbtCompound entityTag, CallbackInfoReturnable<EntityData> cir) {
        EntityAttributeInstance instance = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (instance != null) instance.setBaseValue(0.45);

        EntityAttributeInstance instanceHP = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (instanceHP != null) {
            instanceHP.setBaseValue(26.0D);
            this.setHealth(this.getMaxHealth());
        }
    }

    @Inject(method = "initGoals", at = @At("HEAD"))
    private void fleePlayer(CallbackInfo ci) {
        this.goalSelector.add(1, new FleeEntityGoal<>(this, PlayerEntity.class, 3, 1.1, 1.2,
                (livingEntity) -> true));
    }

    @ModifyArg(method = "enchantMainHandItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/random/Random;nextInt(I)I"))
    private int changeProbabilityMoreEnchant(int range) {
        return range / 3;
    }
}