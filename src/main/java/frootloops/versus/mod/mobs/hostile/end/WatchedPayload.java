package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server to client: how far the player's screen has filled with a watchling's static, 0 to 1 (WatchlingStare). */
public record WatchedPayload(float stare) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WatchedPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "watched"));
    public static final StreamCodec<FriendlyByteBuf, WatchedPayload> CODEC = StreamCodec.ofMember(
            (value, buf) -> buf.writeFloat(value.stare), buf -> new WatchedPayload(buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
