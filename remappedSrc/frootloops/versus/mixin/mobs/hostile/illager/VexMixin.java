package frootloops.versus.mixin.mobs.hostile.illager;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VexEntity.class)
public abstract class VexMixin extends HostileEntity {
    protected VexMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "initialize", at = @At("HEAD"))
    private void decreaseHealth(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData, CallbackInfoReturnable<EntityData> cir) {
        EntityAttributeInstance instanceHP = this.getAttributes().getCustomInstance(EntityAttributes.MAX_HEALTH);
        if (instanceHP != null) {
            instanceHP.setBaseValue(8.0D);
            this.setHealth(this.getMaxHealth());
        }
    }

    @Override
    public float getTargetingMargin() {
        return 0.5F;
    }
}