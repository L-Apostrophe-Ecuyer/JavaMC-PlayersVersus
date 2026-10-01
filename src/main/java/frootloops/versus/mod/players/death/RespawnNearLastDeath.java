package frootloops.versus.mod.players.death;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class RespawnNearLastDeath {

    public static void respawnPlayerNearTheirDeath(ServerPlayer player) {
        if(player == null) return;
        respawnPlayerNearTheirDeath(player,player.level().getServer(),player.getUUID());
    }

    public static void respawnPlayerNearTheirDeath(ServerPlayer player, MinecraftServer server, UUID playerUUID) {
        if(server == null || player == null || !player.getUUID().equals(playerUUID)) return;
        Optional<GlobalPos> lastDeathPos = player.getLastDeathLocation();
        ResourceKey<Level> deathDimension = lastDeathPos.get().dimension();
        BlockPos deathPosition = lastDeathPos.get().pos();

        if(deathDimension != Level.OVERWORLD || deathDimension != player.level().dimension()) return;
        RespawnNearLastDeath.moveToOverworldDeathLocation(player, deathPosition, server.overworld(), 128, 17);
    }

    public static void moveToOverworldDeathLocation(ServerPlayer player, BlockPos blockPos, ServerLevel world, int spawnRadius, int spawnOffsetMultiplier) {
        long l;
        long m;
        int spawnDiameterSquared = (m = (l = (long)(spawnRadius * 2 + 1)) * l) > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)m;

        int worldBorderDist = Mth.floor(world.getWorldBorder().getDistanceToBorder(blockPos.getX(), blockPos.getZ()));
        if (worldBorderDist < spawnRadius) spawnRadius = worldBorderDist;
        if (worldBorderDist <= 1) spawnRadius = 1;

        int randomSpawnPosition = (75 + RandomSource.create().nextInt(spawnDiameterSquared))/2;
        for (int p = 0; p < spawnDiameterSquared; ++p) {
            int q = (randomSpawnPosition + spawnOffsetMultiplier * p) % spawnDiameterSquared;
            int r = q % (spawnRadius * 2 + 1);
            int s = q / (spawnRadius * 2 + 1);
            BlockPos foundSpawnBlockPos = findOverworldSpawn(world, blockPos.getX() + r - spawnRadius, blockPos.getZ() + s - spawnRadius);
            if (foundSpawnBlockPos == null) continue;
            if (!world.noCollision(player)) continue;
            player.teleportTo(foundSpawnBlockPos.getX(), foundSpawnBlockPos.getY(), foundSpawnBlockPos.getZ());
            player.snapTo(foundSpawnBlockPos, 0.0f, 0.0f);
            break;
        }
    }

    @Nullable
    private static BlockPos findOverworldSpawn(ServerLevel world, int x, int z) {
        int i;
        boolean bl = world.dimensionType().hasCeiling();
        LevelChunk worldChunk = world.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z));
        int n = i = bl ? world.getChunkSource().getGenerator().getSpawnHeight(world) : worldChunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 0xF, z & 0xF);
        if (i < world.getMinY()) {
            return null;
        }
        int j = worldChunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 0xF, z & 0xF);
        if (j <= i && j > worldChunk.getHeight(Heightmap.Types.OCEAN_FLOOR, x & 0xF, z & 0xF)) {
            return null;
        }
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int k = i + 1; k >= world.getMinY(); --k) {
            mutable.set(x, k, z);
            BlockState blockState = world.getBlockState(mutable);
            if (!blockState.getFluidState().isEmpty()) break;
            if (!Block.isFaceFull(blockState.getCollisionShape(world, mutable), Direction.UP)) continue;
            return ((BlockPos)mutable.above()).immutable();
        }
        return null;
    }
}
