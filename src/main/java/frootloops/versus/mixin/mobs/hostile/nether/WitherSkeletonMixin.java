package frootloops.versus.mixin.mobs.hostile.nether;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherSkeletonEntity.class)
public class WitherSkeletonMixin extends HostileEntity {
    protected WitherSkeletonMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initialize", at = @At("TAIL"))
    private void decreaseHealth(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, CallbackInfoReturnable<EntityData> cir) {
        EntityAttributeInstance instanceHP = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (instanceHP != null) {
            instanceHP.setBaseValue(40.0D);
            this.setHealth(this.getMaxHealth());
        }

        EntityAttributeInstance instanceKnockbackRes = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (instanceKnockbackRes != null) instanceKnockbackRes.setBaseValue(0.5D);

        EntityAttributeInstance instanceKnockbackAtk = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_ATTACK_KNOCKBACK);
        if (instanceKnockbackAtk != null) instanceKnockbackAtk.setBaseValue(1.25D);
    }
}
