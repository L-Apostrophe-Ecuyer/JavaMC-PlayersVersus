package frootloops.versus.mixin.hostile_mobs;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WitherSkeletonEntity.class)
public class WitherSkeletonMixin extends HostileEntity {
    protected WitherSkeletonMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityTag) {
        EntityAttributeInstance instance = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (instance != null) {
            instance.setBaseValue(30.0D);
            this.setHealth(this.getMaxHealth());
        }
        return super.initialize(world, difficulty, spawnReason, entityData, entityTag);
    }
}