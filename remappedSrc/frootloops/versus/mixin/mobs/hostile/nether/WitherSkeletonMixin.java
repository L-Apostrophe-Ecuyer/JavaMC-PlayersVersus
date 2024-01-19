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

@Mixin(WitherSkeletonEntity.class)
public class WitherSkeletonMixin extends HostileEntity {
    protected WitherSkeletonMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityTag) {
        EntityAttributeInstance instanceHealth = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (instanceHealth != null) {
            instanceHealth.setBaseValue(40.0D);
            this.setHealth(this.getMaxHealth());
        }
        EntityAttributeInstance instanceKnockback = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
        if (instanceKnockback != null) instanceKnockback.setBaseValue(0.5D);

        return super.initialize(world, difficulty, spawnReason, entityData, entityTag);
    }

    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        if(spawnReason == SpawnReason.NATURAL && this.getBlockPos().getY() > -32 && !this.method_48926().getBiome(this.getBlockPos()).isIn(BiomeTags.ANCIENT_CITY_HAS_STRUCTURE)) return false;
        else return super.canSpawn(world, spawnReason);
    }
}
