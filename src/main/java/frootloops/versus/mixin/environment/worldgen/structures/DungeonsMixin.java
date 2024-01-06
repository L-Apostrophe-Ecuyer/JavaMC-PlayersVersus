package frootloops.versus.mixin.environment.worldgen.structures;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.DungeonFeature;
import net.minecraft.world.gen.feature.Feature;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Predicate;

@Mixin(DungeonFeature.class)
public abstract class DungeonsMixin extends Feature<DefaultFeatureConfig> {
    public DungeonsMixin(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    protected void setBlockStateIf(StructureWorldAccess world, BlockPos pos, BlockState state, Predicate<BlockState> predicate) {
        if (predicate.test(world.getBlockState(pos))) {
            Block block = state.getBlock();
            if (Blocks.COBBLESTONE.equals(block) ) {
                if(pos.getY() < 12 && world.getRandom().nextInt(12) > pos.getY()) state = Blocks.STONE_BRICKS.getDefaultState();
                else if(pos.getY() < 0 || (world.getBlockState(pos).isOf(Blocks.DEEPSLATE))) state = Blocks.DEEPSLATE_BRICKS.getDefaultState();
            } else if (Blocks.MOSSY_COBBLESTONE.equals(block) ) {
                if(pos.getY() < 12 && world.getRandom().nextInt(12) > pos.getY()) state = Blocks.MOSSY_STONE_BRICKS.getDefaultState();
                else if(pos.getY() < 0 || (world.getBlockState(pos).isOf(Blocks.DEEPSLATE))) state = Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState();
            }
            world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
        }
    }
}
