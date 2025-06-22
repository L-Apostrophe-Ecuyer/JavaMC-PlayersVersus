package frootloops.versus.mod.players.death;

import frootloops.versus.VersusMod;
import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientPayloadC2S;
import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientSync;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

public record RespawnNearbyPayload(UUID playerUUID) implements CustomPayload {
    public static final CustomPayload.Id<RespawnNearbyPayload> ID = CustomPayload.id("request_nearby_respawn");
    public static final PacketCodec<PacketByteBuf, RespawnNearbyPayload> CODEC = PacketCodec.of((value, buf) -> buf.writeUuid(value.playerUUID), buf -> new RespawnNearbyPayload(buf.readUuid()));
    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
