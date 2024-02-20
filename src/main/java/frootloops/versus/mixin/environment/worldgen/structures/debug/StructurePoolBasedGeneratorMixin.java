package frootloops.versus.mixin.environment.worldgen.structures.debug;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.worldgen.structures.StructurePoolGeneratorDebuger;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.pool.*;
import net.minecraft.structure.pool.alias.StructurePoolAliasLookup;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
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

        VersusMod.MOD_LOGGER.warn("STARTED GENERATING JIGSAW: " + structurePool.getIdAsString());
        VersusMod.MOD_LOGGER.warn("STARTED GENERATING JIGSAW WITH POOL OF SIZE " + structurePool.value().getElementCount());
        VersusMod.MOD_LOGGER.warn("STARTED GENERATING JIGSAW WITH KEY? " + structurePool.getKey().isPresent());

        DynamicRegistryManager dynamicRegistryManager = context.dynamicRegistryManager();
        Registry<StructurePool> registry = dynamicRegistryManager.get(RegistryKeys.TEMPLATE_POOL);
        StructurePool structurePool2 = structurePool.getKey().flatMap(registryKey -> registry.getOrEmpty(aliasLookup.lookup((RegistryKey<StructurePool>)registryKey))).orElse(structurePool.value());


    }

    @Inject(method = "Lnet/minecraft/structure/pool/StructurePoolBasedGenerator;generate(Lnet/minecraft/world/gen/structure/StructureType$Context;Lnet/minecraft/util/registry/RegistryEntry;ILnet/minecraft/structure/pool/StructurePoolBasedGenerator$PieceFactory;Lnet/minecraft/util/math/BlockPos;ZLjava/util/Optional;I)Ljava/util/Optional;",
            at = @At("RETURN"), cancellable = false)
    private static void stopGeneratingJigsaw(
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

        if(cir.getReturnValue().isEmpty()) {
            VersusMod.MOD_LOGGER.warn("ENDED JIGSAW GENERATION: NOTHING GENERATED");
        }
        else {
            VersusMod.MOD_LOGGER.warn("ENDED JIGSAW GENERATION: " + cir.getReturnValue().stream().count() + " STRUCT POSITIONS");
        }
    }

    @Overwrite
    private static Optional<BlockPos> findStartingJigsawPos(StructurePoolElement pool, Identifier id, BlockPos pos, BlockRotation rotation, StructureTemplateManager structureManager, ChunkRandom random) {
        List<StructureTemplate.StructureBlockInfo> list = pool.getStructureBlockInfos(structureManager, pos, rotation, random);
        Optional<BlockPos> optional = Optional.empty();

        for (StructureTemplate.StructureBlockInfo structureBlockInfo : list) {
            Identifier identifier = Identifier.tryParse(Objects.requireNonNull(structureBlockInfo.nbt(), () -> structureBlockInfo + " nbt was null").getString("name"));
            if (!id.equals(identifier)) continue;
            VersusMod.MOD_LOGGER.warn("LOOKING FOR JIGSAW STARTING POS: " + identifier.toUnderscoreSeparatedString());
            optional = Optional.of(structureBlockInfo.pos());
            break;
        }
        if(optional.isEmpty()) VersusMod.MOD_LOGGER.warn("LOOKING FOR JIGSAW STARTING POS: Failure! Was looking for " + id.toUnderscoreSeparatedString());
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
