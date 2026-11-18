package frootloops.versus.mod.items_and_effects.containers;

import frootloops.versus.VersusMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ItemContainerContents;

/** Server to client: what the shulker box here holds (empty: nothing to show, an empty box or one the player can't open). */
public record ShulkerPeekPayload(BlockPos pos, ItemContainerContents contents) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShulkerPeekPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "shulker_peek"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShulkerPeekPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ShulkerPeekPayload::pos, ItemContainerContents.STREAM_CODEC, ShulkerPeekPayload::contents, ShulkerPeekPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
