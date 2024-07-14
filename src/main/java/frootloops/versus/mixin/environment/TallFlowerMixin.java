package frootloops.versus.mixin.environment;

import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ShortPlantBlock.class)
public abstract class TallFlowerMixin extends PlantBlock implements Fertilizable {

    protected TallFlowerMixin(Settings settings) {
        super(settings);
    }


    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if(state.isOf(Blocks.ROSE_BUSH)) {
            if (!(entity instanceof PlayerEntity) || !(entity instanceof HostileEntity)) {
                return;
            }
            entity.slowMovement(state, new Vec3d(0.9f, 0.75, 0.9f));
            if (!(world.isClient || (entity.lastRenderX == entity.getX() && entity.lastRenderZ == entity.getZ()))) {
                double d = Math.abs(entity.getX() - entity.lastRenderX);
                double e = Math.abs(entity.getZ() - entity.lastRenderZ);
                if (d >= (double) 0.01f || e >= (double) 0.01f) {
                    entity.damage(world.getDamageSources().sweetBerryBush(), 1.0f);
                }
            }
        }
    }
}
