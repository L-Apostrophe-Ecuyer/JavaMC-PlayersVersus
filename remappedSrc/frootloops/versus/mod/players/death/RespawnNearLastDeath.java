package frootloops.versus.mod.players.death;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class RespawnNearLastDeath {

    public static void respawnPlayerNearTheirDeath(ServerPlayerEntity player) {
        if(player == null) return;
        respawnPlayerNearTheirDeath(player,player.getEntityWorld().getServer(),player.getUuid());
    }

    public static void respawnPlayerNearTheirDeath(ServerPlayerEntity player, MinecraftServer server, UUID playerUUID) {
        if(server == null || player == null || !player.getUuid().equals(playerUUID)) return;
        Optional<GlobalPos> lastDeathPos = player.getLastDeathPos();
        RegistryKey<World> deathDimension = lastDeathPos.get().dimension();
        BlockPos deathPosition = lastDeathPos.get().pos();

        if(deathDimension != World.OVERWORLD || deathDimension != player.getEntityWorld().getRegistryKey()) return;
        RespawnNearLastDeath.moveToOverworldDeathLocation(player, deathPosition, server.getOverworld(), 128, 17);
    }

    public static void moveToOverworldDeathLocation(ServerPlayerEntity player, BlockPos blockPos, ServerWorld world, int spawnRadius, int spawnOffsetMultiplier) {
        long l;
        long m;
        int spawnDiameterSquared = (m = (l = (long)(spawnRadius * 2 + 1)) * l) > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)m;

        int worldBorderDist = MathHelper.floor(world.getWorldBorder().getDistanceInsideBorder(blockPos.getX(), blockPos.getZ()));
        if (worldBorderDist < spawnRadius) spawnRadius = worldBorderDist;
        if (worldBorderDist <= 1) spawnRadius = 1;

        int randomSpawnPosition = (75 + Random.create().nextInt(spawnDiameterSquared))/2;
        for (int p = 0; p < spawnDiameterSquared; ++p) {
            int q = (randomSpawnPosition + spawnOffsetMultiplier * p) % spawnDiameterSquared;
            int r = q % (spawnRadius * 2 + 1);
            int s = q / (spawnRadius * 2 + 1);
            BlockPos foundSpawnBlockPos = findOverworldSpawn(world, blockPos.getX() + r - spawnRadius, blockPos.getZ() + s - spawnRadius);
            if (foundSpawnBlockPos == null) continue;
            if (!world.isSpaceEmpty(player)) continue;
            player.requestTeleport(foundSpawnBlockPos.getX(), foundSpawnBlockPos.getY(), foundSpawnBlockPos.getZ());
            player.refreshPositionAndAngles(foundSpawnBlockPos, 0.0f, 0.0f);
            break;
        }
    }

    @Nullable
    private static BlockPos findOverworldSpawn(ServerWorld world, int x, int z) {
        int i;
        boolean bl = world.getDimension().hasCeiling();
        WorldChunk worldChunk = world.getChunk(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z));
        int n = i = bl ? world.getChunkManager().getChunkGenerator().getSpawnHeight(world) : worldChunk.sampleHeightmap(Heightmap.Type.MOTION_BLOCKING, x & 0xF, z & 0xF);
        if (i < world.getBottomY()) {
            return null;
        }
        int j = worldChunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE, x & 0xF, z & 0xF);
        if (j <= i && j > worldChunk.sampleHeightmap(Heightmap.Type.OCEAN_FLOOR, x & 0xF, z & 0xF)) {
            return null;
        }
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int k = i + 1; k >= world.getBottomY(); --k) {
            mutable.set(x, k, z);
            BlockState blockState = world.getBlockState(mutable);
            if (!blockState.getFluidState().isEmpty()) break;
            if (!Block.isFaceFullSquare(blockState.getCollisionShape(world, mutable), Direction.UP)) continue;
            return ((BlockPos)mutable.up()).toImmutable();
        }
        return null;
    }
}
