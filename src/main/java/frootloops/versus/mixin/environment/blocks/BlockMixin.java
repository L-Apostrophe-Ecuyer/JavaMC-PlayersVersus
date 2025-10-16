package frootloops.versus.mixin.environment.blocks;


import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Block.class)
public abstract class BlockMixin extends AbstractBlock {
    public BlockMixin(Settings settings) {
        super(settings);
    }

    @Overwrite
    public float getBlastResistance() {
        return this.resistance * 0.4f;
    }

    @Override
    protected BlockSoundGroup getSoundGroup(BlockState state) {
        if(this.soundGroup == BlockSoundGroup.STONE && this.getDefaultMapColor() == MapColor.OFF_WHITE) {
            return BlockSoundGroup.CALCITE;
        }
        return this.soundGroup;
    }

    @Overwrite
    public static void dropStacks(BlockState state, World world, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack tool) {
        if (world instanceof ServerWorld) {
            Block.getDroppedStacks(state, (ServerWorld)world, pos, blockEntity, entity, tool).forEach(stack -> dropStackTowardsPlayer(world, pos, stack, entity));
            state.onStacksDropped((ServerWorld)world, pos, tool, true);
        }
    }

    private static void dropStackTowardsPlayer(World world, BlockPos pos, ItemStack stack, @Nullable Entity entity) {
        if (world.isClient() || stack.isEmpty() || !((ServerWorld)world).getGameRules().getBoolean(GameRules.DO_TILE_DROPS)) return;

        double itemHeight = 0.125;
        double posX = (double)pos.getX() + 0.5 + MathHelper.nextDouble(world.random, -0.25, 0.25);
        double posY = (double)pos.getY() + 0.5 + MathHelper.nextDouble(world.random, -0.25, 0.25) - itemHeight;
        double posZ = (double)pos.getZ() + 0.5 + MathHelper.nextDouble(world.random, -0.25, 0.25);
        ItemEntity itemEntity = new ItemEntity(world, posX, posY, posZ, stack);
        itemEntity.setToDefaultPickupDelay();

        if(entity != null) {
            Vec3d distanceVect = itemEntity.getEntityPos().relativize(entity.getEntityPos()).multiply(0.1);
            itemEntity.setVelocity(itemEntity.getVelocity().add(distanceVect.multiply(0.6)));
            if(distanceVect.lengthSquared() < 1.0) itemEntity.resetPickupDelay();
        }
        world.spawnEntity(itemEntity);
    }
}
