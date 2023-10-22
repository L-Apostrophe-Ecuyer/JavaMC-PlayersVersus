package frootloops.versus.mixin.hostile_mobs;

import frootloops.versus.util.hostile_mobs.PhantomMoveControlRevamp;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.FlyingEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PhantomEntity.class)
public abstract class PhantomMixin extends FlyingEntity {

    @Shadow
    BlockPos circlingCenter = BlockPos.ORIGIN;

    protected PhantomMixin(EntityType<? extends FlyingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        if(world.getRandom().nextBoolean()) return false;
        return super.canSpawn(world, spawnReason);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityTag) {
        this.moveControl = new PhantomMoveControlRevamp((PhantomEntity) ((Object)this));
        this.circlingCenter = this.getBlockPos().up(12);

        EntityAttributeInstance instance = this.getAttributes().getCustomInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (instance != null) {
            instance.setBaseValue(8.0D);
            this.setHealth(8.0f);
        }
        return super.initialize(world, difficulty, spawnReason, entityData, entityTag);
    }
}
