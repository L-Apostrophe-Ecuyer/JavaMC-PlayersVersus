package frootloops.versus.mixin.environment.worldgen.structures;


import com.mojang.serialization.Codec;
import frootloops.versus.mod.mobs.ModEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
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

    //private static final EntityType<?>[] TRIAL_MOB_SPAWNER_ENTITIES = new EntityType[]{EntityType.SKELETON, EntityType.SKELETON, EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.ZOMBIE, ModEntities.DEEPER_CREEPER, EntityType.CAVE_SPIDER, EntityType.WITHER_SKELETON};

    @Override
    protected void setBlockStateIf(StructureWorldAccess world, BlockPos pos, BlockState state, Predicate<BlockState> predicate) {
        if (predicate.test(world.getBlockState(pos))) {
            Block block = state.getBlock();
            if (Blocks.COBBLESTONE.equals(block) ) {
                if(pos.getY() < 0 || (world.getBlockState(pos).isOf(Blocks.DEEPSLATE))) state = Blocks.DEEPSLATE_BRICKS.getDefaultState();
                else if(pos.getY() < 12 && world.getRandom().nextInt(12) > pos.getY()) state = Blocks.STONE_BRICKS.getDefaultState();

            } else if (Blocks.MOSSY_COBBLESTONE.equals(block) ) {
                if(pos.getY() < 0 || (world.getBlockState(pos).isOf(Blocks.DEEPSLATE))) state = Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState();
                else if(pos.getY() < 12 && world.getRandom().nextInt(12) > pos.getY()) state = Blocks.MOSSY_STONE_BRICKS.getDefaultState();

            }
            world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
        }
    }
}
