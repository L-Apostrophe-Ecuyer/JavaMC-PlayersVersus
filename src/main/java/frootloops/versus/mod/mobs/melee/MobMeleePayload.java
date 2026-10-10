package frootloops.versus.mod.mobs.melee;

import frootloops.versus.VersusMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server to client: a mob's melee swing has started winding up (and for how many ticks), a heavy one has started its
 * crouch (for how many ticks) or taken off (for about how many in the air), it has struck and hit, struck and missed, or
 * been cut short, so clients can animate it ({@link MobMelee}).
 */
public record MobMeleePayload(int entityId, byte action, int ticks) implements CustomPacketPayload {
    public static final byte WIND_UP = 0, HEAVY_WIND_UP = 1, HIT = 2, MISS = 3, CANCEL = 4, LEAP = 5;

    public static final CustomPacketPayload.Type<MobMeleePayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "mob_melee"));
    public static final StreamCodec<FriendlyByteBuf, MobMeleePayload> CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeVarInt(value.entityId);
        buf.writeByte(value.action);
        buf.writeVarInt(value.ticks);
    }, buf -> new MobMeleePayload(buf.readVarInt(), buf.readByte(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
