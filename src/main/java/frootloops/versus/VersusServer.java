package frootloops.versus;

import frootloops.versus.mod.players.death.RespawnNearLastDeath;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import java.util.UUID;

public class VersusServer implements DedicatedServerModInitializer {
    /**
     * Runs the mod initializer on the server environment.
     */
    @Override
    public void onInitializeServer() {
        ServerPlayNetworking.registerGlobalReceiver(VersusMod.RESPAWN_NEAR_DEATH_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                UUID playerUUID = buf.readUuid();
                if(player == null || player.getUuid() != playerUUID || !player.isDead()) return;
                RespawnNearLastDeath.respawnPlayerNearTheirDeath(player, server);
            });
        });
    }
}
