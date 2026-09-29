package frootloops.versus.mixin.environment.worldgen.structures;


import com.mojang.serialization.Codec;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.mobs.ModEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.MonsterRoomFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;


@Mixin(MonsterRoomFeature.class)
public abstract class DungeonsMixin extends Feature<NoneFeatureConfiguration> {
    public DungeonsMixin(Codec<NoneFeatureConfiguration> configCodec) {
        super(configCodec);
    }

    private static EntityType<?>[] DEEP_MOB_SPAWNER_ENTITIES;
    private BlockPos spawnerPos = null;


    @Override
    protected void safeSetBlock(WorldGenLevel world, BlockPos pos, BlockState state, Predicate<BlockState> predicate) {
        if (predicate.test(world.getBlockState(pos))) {
            Block block = state.getBlock();
            if (Blocks.COBBLESTONE.equals(block) ) {
                if(pos.getY() <= -8 || (world.getBlockState(pos).is(Blocks.DEEPSLATE))) state = Blocks.TUFF_BRICKS.defaultBlockState();
                else if(pos.getY() > -8 && pos.getY() < 40 && world.getRandom().nextInt(54) > pos.getY() + 8) state = Blocks.STONE_BRICKS.defaultBlockState();
                else if(world.getBlockState(pos).is(Blocks.TUFF)) state = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();

            } else if (Blocks.MOSSY_COBBLESTONE.equals(block) ) {
                if(pos.getY() <= -8 || (world.getBlockState(pos).is(Blocks.DEEPSLATE))) state = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
                else if(pos.getY() > -8 && pos.getY() < 40 && world.getRandom().nextInt(54) > pos.getY() + 8) state = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
                else if(world.getBlockState(pos).is(Blocks.TUFF)) state = Blocks.TUFF_BRICKS.defaultBlockState();
            }
            else if (Blocks.SPAWNER.equals(block) && pos.getY() < -8) spawnerPos = pos;
            world.setBlock(pos, state, Block.UPDATE_CLIENTS);
        }
        else if(state == Blocks.SPAWNER.defaultBlockState()) spawnerPos = null;
    }

    @Inject(method = "place(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"), cancellable = true)
    public void generate(FeaturePlaceContext<NoneFeatureConfiguration> context, CallbackInfoReturnable<Boolean> info) {
        if(spawnerPos == null) return;
        RandomSource random = context.random();
        WorldGenLevel structureWorldAccess = context.level();
        if (structureWorldAccess.getBlockEntity(spawnerPos) instanceof SpawnerBlockEntity mobSpawnerBlockEntity) {

            if(DEEP_MOB_SPAWNER_ENTITIES == null) DEEP_MOB_SPAWNER_ENTITIES =  new EntityType[]{EntityType.SKELETON, EntityType.SKELETON, EntityType.ZOMBIE, ModEntities.WITHERED_ZOMBIE, EntityType.CAVE_SPIDER, EntityType.WITHER_SKELETON};
            EntityType<?> entityType = DEEP_MOB_SPAWNER_ENTITIES[random.nextInt(DEEP_MOB_SPAWNER_ENTITIES.length)];
            mobSpawnerBlockEntity.setEntityId(entityType, random);

        } else VersusMod.MOD_LOGGER.error("Failed to fetch mob spawner entity at ({}, {}, {})", spawnerPos.getX(), spawnerPos.getY(), spawnerPos.getZ());
        spawnerPos = null;
        info.cancel();
    }
}
