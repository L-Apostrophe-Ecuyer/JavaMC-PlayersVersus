package frootloops.versus.mixin.environment.worldgen.structures.debugging_only;

import com.google.common.collect.Lists;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.structures.StructurePoolGeneratorDebuger;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.JigsawBlock;
import net.minecraft.block.enums.Orientation;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.pool.*;
import net.minecraft.structure.pool.alias.StructurePoolAliasLookup;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(StructurePoolBasedGenerator.class)
public class StructurePoolBasedGeneratorMixin {

    private static final boolean SHOW_DEBUG_MESSAGES = true;

    @Inject(method = "Lnet/minecraft/structure/pool/StructurePoolBasedGenerator;generate(Lnet/minecraft/world/gen/structure/StructureType$Context;Lnet/minecraft/util/registry/RegistryEntry;ILnet/minecraft/structure/pool/StructurePoolBasedGenerator$PieceFactory;Lnet/minecraft/util/math/BlockPos;ZLjava/util/Optional;I)Ljava/util/Optional;",
            at = @At("HEAD"), cancellable = false)
    private static void startGeneratingJigsaw(
            Structure.Context context,
            RegistryEntry<StructurePool> structurePool,
            Optional<Identifier> id,
            int size,
            BlockPos pos,
            boolean useExpansionHack,
            Optional<Heightmap.Type> projectStartToHeightmap,
            int maxDistanceFromCenter,
            StructurePoolAliasLookup aliasLookup,
            CallbackInfoReturnable<Optional<Structure.StructurePosition>> cir) {

        if(SHOW_DEBUG_MESSAGES) {
            VersusMod.MOD_LOGGER.warn("STARTED GENERATING JIGSAW: " + id.toString() + " OF SIZE " + structurePool.value().getElementCount());
        }

        DynamicRegistryManager dynamicRegistryManager = context.dynamicRegistryManager();
        Registry<StructurePool> registry = dynamicRegistryManager.get(RegistryKeys.TEMPLATE_POOL);
        StructurePool structurePool2 = structurePool.getKey().flatMap(registryKey -> registry.getOrEmpty(aliasLookup.lookup((RegistryKey<StructurePool>)registryKey))).orElse(structurePool.value());


    }

    @Overwrite
    private static Optional<BlockPos> findStartingJigsawPos(StructurePoolElement pool, Identifier id, BlockPos pos, BlockRotation rotation, StructureTemplateManager structureManager, ChunkRandom random) {
        List<StructureTemplate.StructureBlockInfo> list = pool.getStructureBlockInfos(structureManager, pos, rotation, random);
        Optional<BlockPos> optional = Optional.empty();

        if(list.isEmpty()) {

            list = pool.getStructureBlockInfos(structureManager, pos, BlockRotation.NONE, random);
            if(SHOW_DEBUG_MESSAGES) VersusMod.MOD_LOGGER.warn("LOOKING FOR JIGSAW STARTING POS: " + list.size() + " Jigsaws in structure with rotation " + BlockRotation.NONE);

            list = pool.getStructureBlockInfos(structureManager, pos, BlockRotation.CLOCKWISE_90, random);
            if(SHOW_DEBUG_MESSAGES) VersusMod.MOD_LOGGER.warn("LOOKING FOR JIGSAW STARTING POS: " + list.size() + " Jigsaws in structure with rotation " + BlockRotation.CLOCKWISE_90);

            list = pool.getStructureBlockInfos(structureManager, pos, BlockRotation.CLOCKWISE_180, random);
            if(SHOW_DEBUG_MESSAGES) VersusMod.MOD_LOGGER.warn("LOOKING FOR JIGSAW STARTING POS: " + list.size() + " Jigsaws in structure with rotation " + BlockRotation.CLOCKWISE_180);

            list = pool.getStructureBlockInfos(structureManager, pos, BlockRotation.COUNTERCLOCKWISE_90, random);
            if(SHOW_DEBUG_MESSAGES)  VersusMod.MOD_LOGGER.warn("LOOKING FOR JIGSAW STARTING POS: " + list.size() + " Jigsaws in structure with rotation " + BlockRotation.COUNTERCLOCKWISE_90);

            if(SHOW_DEBUG_MESSAGES) VersusMod.MOD_LOGGER.warn("LOOKING FOR JIGSAW STARTING POS: ERROR, NO jigsaws in the structure! Rotation is " + rotation + " and pool element is of class " + ((pool instanceof FeaturePoolElement) ? "FeaturePoolElement" : ((pool instanceof ListPoolElement) ? "ListPoolElement" : ((pool instanceof SinglePoolElement) ? "SinglePoolElement" : "EmptyPoolElement"))));

            return optional;
        }

        for (StructureTemplate.StructureBlockInfo structureBlockInfo : list) {
            Identifier identifier = Identifier.tryParse(Objects.requireNonNull(structureBlockInfo.nbt(), () -> structureBlockInfo + " nbt was null").getString("name"));

            if(SHOW_DEBUG_MESSAGES) {
                if (id.equals(identifier)) VersusMod.MOD_LOGGER.warn("LOOKING FOR JIGSAW STARTING POS: Found match, " + identifier.toUnderscoreSeparatedString());
                else VersusMod.MOD_LOGGER.warn("LOOKING FOR JIGSAW STARTING POS: Found mismatch, " + identifier.toUnderscoreSeparatedString());
            }
            if (!id.equals(identifier)) continue;
            optional = Optional.of(structureBlockInfo.pos());
            break;
        }
        if(optional.isEmpty()) VersusMod.MOD_LOGGER.warn("LOOKING FOR JIGSAW STARTING POS: Failure! Was looking for " + id.toUnderscoreSeparatedString() + " in a list of " + list.size() + " structure blocks, but none matched.");
        return optional;
    }

    @Inject(method = "Lnet/minecraft/structure/pool/StructurePoolBasedGenerator;generate(Lnet/minecraft/world/gen/noise/NoiseConfig;IZLnet/minecraft/world/gen/chunk/ChunkGenerator;Lnet/minecraft/structure/StructureTemplateManager;Lnet/minecraft/world/HeightLimitView;Lnet/minecraft/util/math/random/Random;Lnet/minecraft/registry/Registry;Lnet/minecraft/structure/PoolStructurePiece;Ljava/util/List;Lnet/minecraft/util/shape/VoxelShape;Lnet/minecraft/structure/pool/alias/StructurePoolAliasLookup;)V",
            at = @At("HEAD"), cancellable = false)
    private static void generatePool(NoiseConfig noiseConfig, int maxSize, boolean modifyBoundingBox, ChunkGenerator chunkGenerator, StructureTemplateManager structureTemplateManager, HeightLimitView heightLimitView, Random random, Registry<StructurePool> structurePoolRegistry, PoolStructurePiece firstPiece, List<PoolStructurePiece> pieces, VoxelShape pieceShape, StructurePoolAliasLookup aliasLookup, CallbackInfo ci) {
        VersusMod.MOD_LOGGER.warn("GENERATING POOL: FIRST PIECE " + firstPiece.getPoolElement().toString() + "\n");
        StructurePoolGeneratorDebuger structurePoolGenerator = new StructurePoolGeneratorDebuger(structurePoolRegistry, maxSize, chunkGenerator, structureTemplateManager, pieces, random);
        structurePoolGenerator.generatePiece(firstPiece, new MutableObject<VoxelShape>(pieceShape), 0, modifyBoundingBox, heightLimitView, noiseConfig, aliasLookup);
        while (structurePoolGenerator.structurePieces.hasNext()) {

            StructurePoolGeneratorDebuger.ShapedPoolStructurePiece nextStructurePiece = (StructurePoolGeneratorDebuger.ShapedPoolStructurePiece)structurePoolGenerator.structurePieces.next();
            PoolStructurePiece nextPiece = nextStructurePiece.piece();
            MutableObject<VoxelShape> pieceShapeNext = nextStructurePiece.pieceShape();
            int currentSize = nextStructurePiece.currentSize();
            VersusMod.MOD_LOGGER.warn("NEXT PIECE: " + nextPiece.getPoolElement().toString() + "\n");
            structurePoolGenerator.generatePiece(nextPiece,pieceShapeNext, currentSize, modifyBoundingBox, heightLimitView, noiseConfig, aliasLookup);
        }
        firstPiece.getJunctions().clear();
    }
}
