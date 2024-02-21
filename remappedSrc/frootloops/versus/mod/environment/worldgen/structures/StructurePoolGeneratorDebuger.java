package frootloops.versus.mod.environment.worldgen.structures;

import com.google.common.collect.Lists;
import frootloops.versus.VersusMod;
import net.minecraft.block.JigsawBlock;
import net.minecraft.block.entity.JigsawBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.JigsawJunction;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.pool.*;
import net.minecraft.structure.pool.alias.StructurePoolAliasLookup;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.collection.PriorityIterator;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.*;

public class StructurePoolGeneratorDebuger {
    public final Registry<StructurePool> registry;
    public final int maxSize;
    public final ChunkGenerator chunkGenerator;
    public final StructureTemplateManager structureTemplateManager;
    public final List<? super PoolStructurePiece> children;
    public final Random random;
    public final PriorityIterator<ShapedPoolStructurePiece> structurePieces = new PriorityIterator();

    public StructurePoolGeneratorDebuger(Registry<StructurePool> registry, int maxSize, ChunkGenerator chunkGenerator, StructureTemplateManager structureTemplateManager, List<? super PoolStructurePiece> children, Random random) {
        this.registry = registry;
        this.maxSize = maxSize;
        this.chunkGenerator = chunkGenerator;
        this.structureTemplateManager = structureTemplateManager;
        this.children = children;
        this.random = random;
    }

    public void generatePiece(PoolStructurePiece piece, MutableObject<VoxelShape> pieceShape, int minY, boolean modifyBoundingBox, HeightLimitView world, NoiseConfig noiseConfig, StructurePoolAliasLookup aliasLookup) {
        StructurePoolElement structurePoolElement = piece.getPoolElement();
        BlockPos blockPos = piece.getPos();
        BlockRotation blockRotation = piece.getRotation();
        StructurePool.Projection projection = structurePoolElement.getProjection();
        boolean bl = projection == StructurePool.Projection.RIGID;
        MutableObject<VoxelShape> startingPieceShape = new MutableObject<VoxelShape>();
        BlockBox startingPieceBoundingBox = piece.getBoundingBox();
        int startingPieceMinY = startingPieceBoundingBox.getMinY();
        block0: for (StructureTemplate.StructureBlockInfo jigsawBlocksOfCurrentStructure : structurePoolElement.getStructureBlockInfos(this.structureTemplateManager, blockPos, blockRotation, this.random)) {

            VersusMod.MOD_LOGGER.warn("GENERATING JIGSAW PIECES: Starting piece, " + jigsawBlocksOfCurrentStructure.toString());

            StructurePoolElement connectingStructurePoolElement;
            MutableObject<VoxelShape> mutableObject2;
            BlockPos jigsawPos = jigsawBlocksOfCurrentStructure.pos();
            BlockPos jigsawConnectionPos = jigsawPos.offset(JigsawBlock.getFacing(jigsawBlocksOfCurrentStructure.state()));
            int yUnderJigsaw = jigsawPos.getY() - startingPieceMinY;
            int k = -1;
            RegistryKey<StructurePool> registryKey = this.lookupPool(jigsawBlocksOfCurrentStructure, aliasLookup);
            Optional<RegistryEntry.Reference<StructurePool>> optional = this.registry.getEntry(registryKey);
            if (optional.isEmpty()) {
                VersusMod.MOD_LOGGER.warn("    - Empty or non-existent pool: {}", (Object)registryKey.getValue());
                continue;
            }
            RegistryEntry registryEntry = optional.get();
            if (((StructurePool)registryEntry.value()).getElementCount() == 0 && !registryEntry.matchesKey(StructurePools.EMPTY)) {
                VersusMod.MOD_LOGGER.warn("    - Empty or non-existent pool: {}", (Object)registryKey.getValue());
                continue;
            }
            RegistryEntry<StructurePool> registryEntry2 = ((StructurePool)registryEntry.value()).getFallback();
            if (registryEntry2.value().getElementCount() == 0 && !registryEntry2.matchesKey(StructurePools.EMPTY)) {
                VersusMod.MOD_LOGGER.warn("    - Empty or non-existent fallback pool: {}", (Object)registryEntry2.getKey().map(key -> key.getValue().toString()).orElse("<unregistered>"));
                continue;
            }
            boolean isOutgoingJigsawBlockInbounds = startingPieceBoundingBox.contains(jigsawConnectionPos);
            if (isOutgoingJigsawBlockInbounds) {
                mutableObject2 = startingPieceShape;
                if (startingPieceShape.getValue() == null) {
                    startingPieceShape.setValue(VoxelShapes.cuboid(Box.from(startingPieceBoundingBox)));
                }
            } else {
                mutableObject2 = pieceShape;
            }
            ArrayList<StructurePoolElement> list = Lists.newArrayList();
            if (minY != this.maxSize) {
                list.addAll(((StructurePool)registryEntry.value()).getElementIndicesInRandomOrder(this.random));
            }
            list.addAll(registryEntry2.value().getElementIndicesInRandomOrder(this.random));

            VersusMod.MOD_LOGGER.warn("    - GENERATING JIGSAW PIECES: List of registryEntries has " + list.size() + " elements");

            int placementPriorityIndex = jigsawBlocksOfCurrentStructure.nbt() != null ? jigsawBlocksOfCurrentStructure.nbt().getInt("placement_priority") : 0;
            Iterator iterator = list.iterator();
            while (iterator.hasNext() && (connectingStructurePoolElement = (StructurePoolElement)iterator.next()) != EmptyPoolElement.INSTANCE) {

                VersusMod.MOD_LOGGER.warn("GENERATING JIGSAW PIECES: Checking if we can connect with " + connectingStructurePoolElement.toString());

                for (BlockRotation randomBlockRotation : BlockRotation.randomRotationOrder(this.random)) {
                    List<StructureTemplate.StructureBlockInfo> jigsawsOfConnectingElement = connectingStructurePoolElement.getStructureBlockInfos(this.structureTemplateManager, BlockPos.ORIGIN, randomBlockRotation, this.random);

                    VersusMod.MOD_LOGGER.warn("    - GENERATING JIGSAW PIECES: In direction " + randomBlockRotation.asString() + ", connecting piece has jigsaws: " + jigsawsOfConnectingElement.toString() +"\n");

                    BlockBox connectingStructureBoundingBox = connectingStructurePoolElement.getBoundingBox(this.structureTemplateManager, BlockPos.ORIGIN, randomBlockRotation);

                    int maxY = !modifyBoundingBox || connectingStructureBoundingBox.getBlockCountY() > 16 ? 0 : jigsawsOfConnectingElement.stream().mapToInt(structureBlockInfo -> {
                        if (!connectingStructureBoundingBox.contains(structureBlockInfo.pos().offset(JigsawBlock.getFacing(structureBlockInfo.state())))) {
                            return 0;
                        }
                        RegistryKey<StructurePool> registryPool = this.lookupPool(structureBlockInfo, aliasLookup);
                        Optional<RegistryEntry.Reference<StructurePool>> optional33 = this.registry.getEntry(registryKey);
                        Optional<RegistryEntry> optional2 = optional33.map(entry -> ((StructurePool)entry.value()).getFallback());
                        int highestY = optional33.map(entry -> ((StructurePool)entry.value()).getHighestY(this.structureTemplateManager)).orElse(0);
                        int highestYFallback = optional2.map(entry -> ((StructurePool)entry.value()).getHighestY(this.structureTemplateManager)).orElse(0);
                        return Math.max(highestY, highestYFallback);
                    }).max().orElse(0);

                    for (StructureTemplate.StructureBlockInfo connectingJigsawBlock : jigsawsOfConnectingElement) {
                        int u;
                        int s;
                        int q;
                        if (!JigsawBlock.attachmentMatches(jigsawBlocksOfCurrentStructure, connectingJigsawBlock)) {
                            VersusMod.MOD_LOGGER.warn("    GENERATING JIGSAW PIECES: Failure. Jigsaws don't connect: " + jigsawBlocksOfCurrentStructure.nbt().getString("name") + " mismatch with " + connectingJigsawBlock.nbt().getString("name"));

                            Direction direction = JigsawBlock.getFacing(jigsawBlocksOfCurrentStructure.state());
                            Direction direction2 = JigsawBlock.getFacing(connectingJigsawBlock.state());
                            Direction direction3 = JigsawBlock.getRotation(jigsawBlocksOfCurrentStructure.state());
                            Direction direction4 = JigsawBlock.getRotation(connectingJigsawBlock.state());

                            JigsawBlockEntity.Joint joint = JigsawBlockEntity.Joint.byName(jigsawBlocksOfCurrentStructure.nbt().getString("joint")).orElseGet(() -> direction.getAxis().isHorizontal() ? JigsawBlockEntity.Joint.ALIGNED : JigsawBlockEntity.Joint.ROLLABLE);
                            boolean isJointRollable = joint == JigsawBlockEntity.Joint.ROLLABLE;

                            boolean areFacingOppositeDirections = direction == direction2.getOpposite();
                            boolean areRotatedSameDirection = (isJointRollable || direction3 == direction4);
                            boolean areTargetsCompatible = jigsawBlocksOfCurrentStructure.nbt().getString("target").equals(connectingJigsawBlock.nbt().getString("name"));
                            boolean doAttachmentsMatch = areFacingOppositeDirections && areRotatedSameDirection && areTargetsCompatible;

                            if(!areTargetsCompatible) VersusMod.MOD_LOGGER.warn(("    GENERATING JIGSAW PIECES: Failure. Jigsaws don't connect: " + jigsawBlocksOfCurrentStructure.nbt().getString("name") + " mismatch with " + connectingJigsawBlock.nbt().getString("name")) + "  ->  Incompatible target and name. Target is '" + jigsawBlocksOfCurrentStructure.nbt().getString("target") + "', while name is '" + connectingJigsawBlock.nbt().getString("name") + "'\n");
                            else if(!areFacingOppositeDirections) VersusMod.MOD_LOGGER.warn(("    GENERATING JIGSAW PIECES: Failure. Jigsaws don't connect: " + jigsawBlocksOfCurrentStructure.nbt().getString("name") + " mismatch with " + connectingJigsawBlock.nbt().getString("name")) + "  ->  Not facing opposing directions. " + direction + " != " + direction2.getOpposite() + "\n");
                            else if(!areRotatedSameDirection) VersusMod.MOD_LOGGER.warn(("    GENERATING JIGSAW PIECES: Failure. Jigsaws don't connect: " + jigsawBlocksOfCurrentStructure.nbt().getString("name") + " mismatch with " + connectingJigsawBlock.nbt().getString("name")) + ("  ->  Not rotated the same, and not rollable. " + direction3 + " != " + direction4) + "\n");
                            continue;
                        }
                        VersusMod.MOD_LOGGER.warn("    GENERATING JIGSAW PIECES: Found connection!****");

                        BlockPos blockPos4 = connectingJigsawBlock.pos();
                        BlockPos blockPos5 = jigsawConnectionPos.subtract(blockPos4);
                        BlockBox blockBox3 = connectingStructurePoolElement.getBoundingBox(this.structureTemplateManager, blockPos5, randomBlockRotation);
                        int n = blockBox3.getMinY();
                        StructurePool.Projection projection2 = connectingStructurePoolElement.getProjection();
                        boolean isRigid = projection2 == StructurePool.Projection.RIGID;

                        int o = blockPos4.getY();
                        int p = yUnderJigsaw - o + JigsawBlock.getFacing(jigsawBlocksOfCurrentStructure.state()).getOffsetY();
                        if (bl && isRigid) {
                            q = startingPieceMinY + p;
                        } else {
                            if (k == -1) {
                                k = this.chunkGenerator.getHeightOnGround(jigsawPos.getX(), jigsawPos.getZ(), Heightmap.Type.WORLD_SURFACE_WG, world, noiseConfig);
                            }
                            q = k - o;
                        }
                        int r = q - n;
                        BlockBox blockBox4 = blockBox3.offset(0, r, 0);
                        BlockPos blockPos6 = blockPos5.add(0, r, 0);
                        if (maxY > 0) {
                            s = Math.max(maxY + 1, blockBox4.getMaxY() - blockBox4.getMinY());
                            blockBox4.encompass(new BlockPos(blockBox4.getMinX(), blockBox4.getMinY() + s, blockBox4.getMinZ()));
                        }

                        if (VoxelShapes.matchesAnywhere((VoxelShape)mutableObject2.getValue(), VoxelShapes.cuboid(Box.from(blockBox4).contract(0.25)), BooleanBiFunction.ONLY_SECOND)) {
                            VersusMod.MOD_LOGGER.warn("    GENERATING JIGSAW PIECES: Failure. Weird issue, where " + ((VoxelShape)mutableObject2.getValue()).toString() + " seems to overlap with " + (VoxelShapes.cuboid(Box.from(blockBox4).contract(0.25))).toString() + "\n");
                            continue;
                        }


                        mutableObject2.setValue(VoxelShapes.combine((VoxelShape)mutableObject2.getValue(), VoxelShapes.cuboid(Box.from(blockBox4)), BooleanBiFunction.ONLY_FIRST));
                        s = piece.getGroundLevelDelta();
                        int t = isRigid ? s - p : connectingStructurePoolElement.getGroundLevelDelta();
                        PoolStructurePiece poolStructurePiece = new PoolStructurePiece(this.structureTemplateManager, connectingStructurePoolElement, blockPos6, t, randomBlockRotation, blockBox4);
                        if (bl) {
                            u = startingPieceMinY + yUnderJigsaw;
                        } else if (isRigid) {
                            u = q + o;
                        } else {
                            if (k == -1) {
                                k = this.chunkGenerator.getHeightOnGround(jigsawPos.getX(), jigsawPos.getZ(), Heightmap.Type.WORLD_SURFACE_WG, world, noiseConfig);
                            }
                            u = k + p / 2;
                        }
                        VersusMod.MOD_LOGGER.warn("    - GENERATING JIGSAW PIECES: Adding junction...");

                        piece.addJunction(new JigsawJunction(jigsawConnectionPos.getX(), u - yUnderJigsaw + s, jigsawConnectionPos.getZ(), p, projection2));
                        poolStructurePiece.addJunction(new JigsawJunction(jigsawPos.getX(), u - o + t, jigsawPos.getZ(), -p, projection));
                        this.children.add(poolStructurePiece);
                        if (minY + 1 > this.maxSize) {
                            VersusMod.MOD_LOGGER.warn("    GENERATING JIGSAW PIECES: Failure. MinY is bigger then the maximum size: " + minY + " + 1 > " + this.maxSize +"\n");
                            continue block0;
                        }
                        ShapedPoolStructurePiece shapedPoolStructurePiece = new ShapedPoolStructurePiece(poolStructurePiece, mutableObject2, minY + 1);
                        this.structurePieces.enqueue(shapedPoolStructurePiece, placementPriorityIndex);
                        VersusMod.MOD_LOGGER.warn("    GENERATING JIGSAW PIECES: Success!\n");
                        continue block0;
                    }
                }
            }
        }
    }

    public static RegistryKey<StructurePool> lookupPool(StructureTemplate.StructureBlockInfo structureBlockInfo, StructurePoolAliasLookup aliasLookup) {
        NbtCompound nbtCompound = Objects.requireNonNull(structureBlockInfo.nbt(), () -> structureBlockInfo + " nbt was null");
        RegistryKey<StructurePool> registryKey = StructurePools.of(nbtCompound.getString("pool"));
        return aliasLookup.lookup(registryKey);
    }

    public record ShapedPoolStructurePiece(PoolStructurePiece piece, MutableObject<VoxelShape> pieceShape, int currentSize) {
    }
}