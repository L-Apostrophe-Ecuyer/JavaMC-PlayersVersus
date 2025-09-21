package frootloops.versus.mixin.environment.worldgen.structures;


import com.mojang.serialization.Codec;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.mobs.ModEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.DungeonFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;


@Mixin(DungeonFeature.class)
public abstract class DungeonsMixin extends Feature<DefaultFeatureConfig> {
    public DungeonsMixin(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    private static EntityType<?>[] DEEP_MOB_SPAWNER_ENTITIES;
    private BlockPos spawnerPos = null;


    @Override
    protected void setBlockStateIf(StructureWorldAccess world, BlockPos pos, BlockState state, Predicate<BlockState> predicate) {
        if (predicate.test(world.getBlockState(pos))) {
            Block block = state.getBlock();
            if (Blocks.COBBLESTONE.equals(block) ) {
                if(pos.getY() <= -8 || (world.getBlockState(pos).isOf(Blocks.DEEPSLATE))) state = Blocks.TUFF_BRICKS.getDefaultState();
                else if(pos.getY() > -8 && pos.getY() < 40 && world.getRandom().nextInt(54) > pos.getY() + 8) state = Blocks.STONE_BRICKS.getDefaultState();
                else if(world.getBlockState(pos).isOf(Blocks.TUFF)) state = Blocks.MOSSY_STONE_BRICKS.getDefaultState();

            } else if (Blocks.MOSSY_COBBLESTONE.equals(block) ) {
                if(pos.getY() <= -8 || (world.getBlockState(pos).isOf(Blocks.DEEPSLATE))) state = Blocks.DEEPSLATE_BRICKS.getDefaultState();
                else if(pos.getY() > -8 && pos.getY() < 40 && world.getRandom().nextInt(54) > pos.getY() + 8) state = Blocks.MOSSY_STONE_BRICKS.getDefaultState();
                else if(world.getBlockState(pos).isOf(Blocks.TUFF)) state = Blocks.TUFF_BRICKS.getDefaultState();
            }
            else if (Blocks.SPAWNER.equals(block) && pos.getY() < -8) spawnerPos = pos;
            world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
        }
        else if(state == Blocks.SPAWNER.getDefaultState()) spawnerPos = null;
    }

    @Inject(method = "generate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/StructureWorldAccess;getBlockEntity(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/entity/BlockEntity;"), cancellable = true)
    public void generate(FeatureContext<DefaultFeatureConfig> context, CallbackInfoReturnable<Boolean> info) {
        if(spawnerPos == null) return;
        Random random = context.getRandom();
        StructureWorldAccess structureWorldAccess = context.getWorld();
        if (structureWorldAccess.getBlockEntity(spawnerPos) instanceof MobSpawnerBlockEntity mobSpawnerBlockEntity) {

            if(DEEP_MOB_SPAWNER_ENTITIES == null) DEEP_MOB_SPAWNER_ENTITIES =  new EntityType[]{EntityType.SKELETON, EntityType.SKELETON, EntityType.ZOMBIE, ModEntities.WITHERED_ZOMBIE, EntityType.CAVE_SPIDER, EntityType.WITHER_SKELETON};
            EntityType<?> entityType = DEEP_MOB_SPAWNER_ENTITIES[random.nextInt(DEEP_MOB_SPAWNER_ENTITIES.length)];
            mobSpawnerBlockEntity.setEntityType(entityType, random);

        } else VersusMod.MOD_LOGGER.error("Failed to fetch mob spawner entity at ({}, {}, {})", spawnerPos.getX(), spawnerPos.getY(), spawnerPos.getZ());
        spawnerPos = null;
        info.cancel();
    }
}
