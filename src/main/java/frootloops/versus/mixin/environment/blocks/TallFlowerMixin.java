package frootloops.versus.mixin.environment.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.TallFlowerBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TallFlowerBlock.class)
public abstract class TallFlowerMixin extends VegetationBlock implements BonemealableBlock {

    protected TallFlowerMixin(Properties settings) {
        super(settings);
    }


    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, InsideBlockEffectApplier handler, boolean bl) {
        if(state.is(Blocks.ROSE_BUSH)) {
            if (!(entity instanceof Player) && !(entity instanceof Monster)) return;
            entity.makeStuckInBlock(state, new Vec3(0.9f, 0.75, 0.9f));
            if (!(world.isClientSide() || (entity.xOld == entity.getX() && entity.zOld == entity.getZ()))) {
                double d = Math.abs(entity.getX() - entity.xOld);
                double e = Math.abs(entity.getZ() - entity.zOld);
                if (d >= (double) 0.03f || e >= (double) 0.03f) {
                    entity.hurtServer((ServerLevel) world, world.damageSources().sweetBerryBush(), 1.0f);
                }
            }
        }
    }

    @Override
    public void performBonemeal(ServerLevel world, RandomSource random, BlockPos pos, BlockState state) {
        if(state.is(Blocks.ROSE_BUSH)) TallFlowerBlock.popResource((Level)world, pos, new ItemStack(Items.POPPY));
        else TallFlowerBlock.popResource((Level)world, pos, new ItemStack(this));
    }
}
