package frootloops.versus.mod.players.death;

import frootloops.versus.VersusMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.UUID;

public record RespawnNearbyPayload(UUID playerUUID) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<RespawnNearbyPayload> ID = CustomPacketPayload.createType("request_nearby_respawn");
    public static final StreamCodec<FriendlyByteBuf, RespawnNearbyPayload> CODEC = StreamCodec.ofMember((value, buf) -> buf.writeUUID(value.playerUUID), buf -> new RespawnNearbyPayload(buf.readUUID()));
    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}
