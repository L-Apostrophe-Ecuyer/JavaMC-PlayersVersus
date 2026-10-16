package frootloops.versus.mod.environment.blocks;

import frootloops.versus.VersusMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Since 26.4 a block blocks motion by being in {@code #minecraft:blocks_motion_no_leaves} (or one of its tags), not by its
 * collision shape: the heightmaps, suffocation, fluid flow, dolphin jumps, teleports and lava fires all ask the tag. A solid
 * block of the mod that isn't in it is skipped by the ocean floor heightmap (mangrove trees were placed over the clay of
 * the mangrove swamp, where they couldn't survive), lets water flow into it, and so on.
 * {@code data/minecraft/tags/block/blocks_motion_no_leaves.json} lists the mod's; this finds the ones it misses, and warns
 * of them when the server starts.
 */
public final class BlockTagAudit {
    public static final TagKey<Block> BLOCKS_MOTION = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("minecraft", "blocks_motion_no_leaves"));

    /** The least a collision shape's size (or its height) must be to count as solid: the game's own rule for a block's "solid" flag. */
    private static final double SOLID_SIZE = 0.7291666666666666;

    private BlockTagAudit() {
    }

    public static void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            List<Identifier> missing = solidBlocksNotBlockingMotion();
            if (!missing.isEmpty()) {
                VersusMod.MOD_LOGGER.warn("Solid blocks of the mod that are not in #minecraft:blocks_motion_no_leaves (add them to its tag file): {}", missing);
            }
        });
    }

    /** The mod's blocks that have a solid collision shape and aren't in {@link #BLOCKS_MOTION}. */
    public static List<Identifier> solidBlocksNotBlockingMotion() {
        List<Identifier> missing = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (!id.getNamespace().equals(VersusMod.MOD_ID)) continue;
            BlockState state = block.defaultBlockState();
            if (isSolid(state) && !state.is(BLOCKS_MOTION)) missing.add(id);
        }
        return missing;
    }

    private static boolean isSolid(BlockState state) {
        VoxelShape shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        if (shape.isEmpty()) return false;
        AABB bounds = shape.bounds();
        return bounds.getSize() >= SOLID_SIZE || bounds.getYsize() >= 1.0;
    }
}
