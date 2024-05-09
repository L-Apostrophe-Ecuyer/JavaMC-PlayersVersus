package frootloops.versus;

import frootloops.versus.mod.players.death.RespawnNearLastDeath;
import frootloops.versus.mod.players.death.RespawnNearbyPayload;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

public class VersusModServer implements DedicatedServerModInitializer {
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
}
