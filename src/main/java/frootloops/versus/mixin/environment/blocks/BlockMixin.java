package frootloops.versus.mixin.environment.blocks;


import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Block.class)
public abstract class BlockMixin extends BlockBehaviour {
    public BlockMixin(Properties settings) {
        super(settings);
    }

    @Overwrite
    public float getExplosionResistance() {
        return this.explosionResistance * 0.4f;
    }

    @Override
    protected SoundType getSoundType(BlockState state) {
        if(this.soundType == SoundType.STONE && this.defaultMapColor() == MapColor.QUARTZ) {
            return SoundType.CALCITE;
        }
        return this.soundType;
    }

    @Overwrite
    public static void dropResources(BlockState state, Level world, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack tool) {
        if (world instanceof ServerLevel) {
            Block.getDrops(state, (ServerLevel)world, pos, blockEntity, entity, tool).forEach(stack -> dropStackTowardsPlayer(world, pos, stack, entity));
            state.spawnAfterBreak((ServerLevel)world, pos, tool, true);
        }
    }

    private static void dropStackTowardsPlayer(Level world, BlockPos pos, ItemStack stack, @Nullable Entity entity) {
        if (world.isClientSide() || stack.isEmpty() || !((ServerLevel)world).getGameRules().getBoolean(GameRules.BLOCK_DROPS)) return;

        double itemHeight = 0.125;
        double posX = (double)pos.getX() + 0.5 + Mth.nextDouble(world.getRandom(), -0.25, 0.25);
        double posY = (double)pos.getY() + 0.5 + Mth.nextDouble(world.getRandom(), -0.25, 0.25) - itemHeight;
        double posZ = (double)pos.getZ() + 0.5 + Mth.nextDouble(world.getRandom(), -0.25, 0.25);
        ItemEntity itemEntity = new ItemEntity(world, posX, posY, posZ, stack);
        itemEntity.setDefaultPickUpDelay();

        if(entity != null) {
            Vec3 distanceVect = itemEntity.position().vectorTo(entity.position()).scale(0.1);
            itemEntity.setDeltaMovement(itemEntity.getDeltaMovement().add(distanceVect.scale(0.6)));
            if(distanceVect.lengthSqr() < 1.0) itemEntity.setNoPickUpDelay();
        }
        world.addFreshEntity(itemEntity);
    }
}
