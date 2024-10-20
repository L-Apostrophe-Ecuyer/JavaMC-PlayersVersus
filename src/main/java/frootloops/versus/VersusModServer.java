package frootloops.versus;

import frootloops.versus.mod.players.death.RespawnNearLastDeath;
import frootloops.versus.mod.players.death.RespawnNearbyPayload;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.UUID;

public class VersusModServer implements DedicatedServerModInitializer {

    private static MinecraftServer serverInstance;
    private static MultiNoiseUtil.MultiNoiseSampler multiNoiseSampler;
    private static World prevWorld;

    @Override
    public void onInitializeServer() {
        VersusModServer.addPacketRecievers();
    }

    public static void addPacketRecievers(){
        ServerPlayNetworking.registerGlobalReceiver(RespawnNearbyPayload.ID, (payload, context) -> {
            context.player().server.execute(() -> {
                RespawnNearLastDeath.respawnPlayerNearTheirDeath(context.player(), context.player().getServer(), payload.playerUUID());
            });
        });
    }

    public static ServerWorld getServerWorld(World world) {
        if(serverInstance == null || !serverInstance.isRunning()) serverInstance = world.getServer();
        if(serverInstance != null) return serverInstance.getWorld(world.getRegistryKey());
        return null;
    }

    public static MultiNoiseUtil.MultiNoiseSampler getNoiseSampler(World world) {
        if(multiNoiseSampler != null && world == prevWorld) {
            return multiNoiseSampler;
        }
        else {
            ServerWorld serverWorld = getServerWorld(world);
            multiNoiseSampler = (serverWorld == null) ? null : getServerWorld(world).getChunkManager().getNoiseConfig().getMultiNoiseSampler();
            return multiNoiseSampler;
        }
    }
}
