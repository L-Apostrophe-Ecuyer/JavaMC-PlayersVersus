package frootloops.versus.mixin.mobs.hostile.nether;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.world.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WitherSkeletonEntity.class)
public class WitherSkeletonMixin extends HostileEntity {
    protected WitherSkeletonMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        if(spawnReason == SpawnReason.NATURAL && this.getBlockPos().getY() > -32 && !this.getWorld().getBiome(this.getBlockPos()).isIn(BiomeTags.ANCIENT_CITY_HAS_STRUCTURE)) return false;
        else return super.canSpawn(world, spawnReason);
    }
}
