package frootloops.versus;

import frootloops.versus.mod.players.death.RespawnNearLastDeath;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.UUID;

public class VersusModServer implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        VersusModServer.addPacketRecievers();
    }

    public static void addPacketRecievers(){
        /*
        ServerPlayNetworking.registerGlobalReceiver(VersusMod.RESPAWN_NEAR_DEATH_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                UUID playerUUID = buf.readUuid();
                RespawnNearLastDeath.respawnPlayerNearTheirDeath(player, player.getServer(), playerUUID);
            });
        });*/
    }
}
