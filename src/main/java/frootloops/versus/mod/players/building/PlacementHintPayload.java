package frootloops.versus.mod.players.building;

import frootloops.versus.VersusMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client to server, just before the use of a block item it goes with: the state the client predicts for the block it
 * places at pos (by its id), when it isn't vanilla's ({@link PlacementRules}).
 */
public record PlacementHintPayload(BlockPos pos, int state) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PlacementHintPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "placement_hint"));
    public static final StreamCodec<FriendlyByteBuf, PlacementHintPayload> CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeBlockPos(value.pos);
        buf.writeVarInt(value.state);
    }, buf -> new PlacementHintPayload(buf.readBlockPos(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
