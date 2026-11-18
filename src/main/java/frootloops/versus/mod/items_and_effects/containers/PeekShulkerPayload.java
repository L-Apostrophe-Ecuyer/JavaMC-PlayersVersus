package frootloops.versus.mod.items_and_effects.containers;

import frootloops.versus.VersusMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client to server: the player is looking at the shulker box here; what does it hold (ShulkerPeekPayload)? */
public record PeekShulkerPayload(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PeekShulkerPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "peek_shulker"));
    public static final StreamCodec<FriendlyByteBuf, PeekShulkerPayload> CODEC = BlockPos.STREAM_CODEC.map(PeekShulkerPayload::new, PeekShulkerPayload::pos).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
