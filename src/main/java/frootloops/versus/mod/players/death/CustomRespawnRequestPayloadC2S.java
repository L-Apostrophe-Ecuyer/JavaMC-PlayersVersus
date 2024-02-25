package frootloops.versus.mod.players.death;

import frootloops.versus.VersusMod;
import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientPayloadC2S;
import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientSync;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

public record CustomRespawnRequestPayloadC2S(UUID uuid) implements CustomPayload {
    public static final PacketCodec<PacketByteBuf, CustomRespawnRequestPayloadC2S> CODEC = CustomPayload.codecOf(CustomRespawnRequestPayloadC2S::write, CustomRespawnRequestPayloadC2S::new);

    private CustomRespawnRequestPayloadC2S(PacketByteBuf buf) {
        this(buf.readUuid());
    }

    public void write(PacketByteBuf buf) {
        buf.writeUuid(uuid);
    }

    public static final CustomPayload.Id<CustomRespawnRequestPayloadC2S> ID = new Id<>(VersusMod.RESPAWN_NEAR_DEATH_PACKET_ID);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
