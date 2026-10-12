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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.sounds.BlockSoundSet;
import net.minecraft.world.level.block.sounds.BlockSoundSets;

@Mixin(Block.class)
public abstract class BlockMixin extends BlockBehaviour {
    public BlockMixin(Properties settings) {
        super(settings);
    }

    @Overwrite
    public float getExplosionResistance() {
        return this.explosionResistance * 0.4f;
    }

    // Public, not protected: the mixin adds it to Block itself, which BlockStateBase names when it calls it; a
    // protected method there can't be called from BlockStateBase's package (block.state), which 1.21.10's
    // intermediary and Yarn packages hid, and 26.x's unobfuscated ones don't (IllegalAccessError).
    @Override
    public Optional<ResourceKey<BlockSoundSet>> getSounds(BlockState state) {
        if(this.blockSoundSet.filter(BlockSoundSets.STONE::equals).isPresent() && this.defaultMapColor() == MapColor.QUARTZ) {
            return Optional.of(BlockSoundSets.CALCITE);
        }
        return this.blockSoundSet;
    }

    @Overwrite
    public static void dropResources(BlockState state, Level world, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack tool) {
        if (world instanceof ServerLevel serverWorld) {
            Block.getDrops(state, serverWorld, pos, blockEntity, entity, tool).forEach(stack -> dropStackTowardsPlayer(world, pos, stack, entity));
            state.spawnAfterBreak(serverWorld, pos, tool, true, entity);
        }
    }

    private static void dropStackTowardsPlayer(Level world, BlockPos pos, ItemStack stack, @Nullable Entity entity) {
        if (world.isClientSide() || stack.isEmpty() || !((ServerLevel)world).getGameRules().get(GameRules.BLOCK_DROPS)) return;

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
