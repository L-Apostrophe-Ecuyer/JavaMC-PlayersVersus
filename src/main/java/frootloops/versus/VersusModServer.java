package frootloops.versus;

import frootloops.versus.mod.players.death.RespawnNearLastDeath;
import frootloops.versus.mod.players.death.RespawnNearbyPayload;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Climate;
import java.util.UUID;

public class VersusModServer implements DedicatedServerModInitializer {

    private static MinecraftServer serverInstance;
    private static Climate.Sampler multiNoiseSampler;
    private static Level prevWorld;

    @Override
    public void onInitializeServer() {
        VersusModServer.addPacketRecievers();
    }

    public static void addPacketRecievers(){
        ServerPlayNetworking.registerGlobalReceiver(RespawnNearbyPayload.ID, (payload, context) -> {
            context.player().level().getServer().execute(() -> {
                RespawnNearLastDeath.respawnPlayerNearTheirDeath(context.player(), context.player().level().getServer(), payload.playerUUID());
            });
        });
    }

    public static ServerLevel getServerWorld(Level world) {
        if(serverInstance == null || !serverInstance.isRunning()) serverInstance = world.getServer();
        if(serverInstance != null) return serverInstance.getLevel(world.dimension());
        return null;
    }

    public static Climate.Sampler getNoiseSampler(Level world) {
        if(multiNoiseSampler != null && world == prevWorld) {
            return multiNoiseSampler;
        }
        else {
            ServerLevel serverWorld = getServerWorld(world);
            multiNoiseSampler = (serverWorld == null) ? null : getServerWorld(world).getChunkSource().randomState().sampler();
            return multiNoiseSampler;
        }
    }
}
