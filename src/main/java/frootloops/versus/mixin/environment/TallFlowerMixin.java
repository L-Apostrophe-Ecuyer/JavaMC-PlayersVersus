package frootloops.versus.mixin.environment;

import frootloops.versus.VersusMod;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TallFlowerBlock.class)
public abstract class TallFlowerMixin extends PlantBlock implements Fertilizable {

    protected TallFlowerMixin(Settings settings) {
        super(settings);
    }


    @Override
    protected void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if(state.isOf(Blocks.ROSE_BUSH)) {
            if (!(entity instanceof PlayerEntity) && !(entity instanceof HostileEntity)) return;

            entity.slowMovement(state, new Vec3d(0.9f, 0.75, 0.9f));
            if (!(world.isClient || (entity.lastRenderX == entity.getX() && entity.lastRenderZ == entity.getZ()))) {
                double d = Math.abs(entity.getX() - entity.lastRenderX);
                double e = Math.abs(entity.getZ() - entity.lastRenderZ);
                if (d >= (double) 0.03f || e >= (double) 0.03f) {
                    entity.damage(world.getDamageSources().sweetBerryBush(), 1.0f);
                }
            }
        }
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        if(state.isOf(Blocks.ROSE_BUSH)) TallFlowerBlock.dropStack((World)world, pos, new ItemStack(Items.POPPY));
        else TallFlowerBlock.dropStack((World)world, pos, new ItemStack(this));
    }
}
