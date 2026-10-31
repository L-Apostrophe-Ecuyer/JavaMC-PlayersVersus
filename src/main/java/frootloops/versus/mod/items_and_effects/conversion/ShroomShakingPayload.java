package frootloops.versus.mod.items_and_effects.conversion;

import frootloops.versus.VersusMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: an entity all white with Conversion shakes for this many more ticks (Shroomify). */
public record ShroomShakingPayload(int entityId, int ticks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ShroomShakingPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "shroom_shaking"));
    public static final StreamCodec<FriendlyByteBuf, ShroomShakingPayload> CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeVarInt(value.entityId);
        buf.writeVarInt(value.ticks);
    }, buf -> new ShroomShakingPayload(buf.readVarInt(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
