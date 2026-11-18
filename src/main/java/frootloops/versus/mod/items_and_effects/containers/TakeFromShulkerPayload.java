package frootloops.versus.mod.items_and_effects.containers;

import frootloops.versus.VersusMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Client to server: take the stack in this slot of the shulker box here, if it is still the stack the player saw there. */
public record TakeFromShulkerPayload(BlockPos pos, int slot, ItemStack seen) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TakeFromShulkerPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "take_from_shulker"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TakeFromShulkerPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TakeFromShulkerPayload::pos, ByteBufCodecs.VAR_INT, TakeFromShulkerPayload::slot,
            ItemStack.STREAM_CODEC, TakeFromShulkerPayload::seen, TakeFromShulkerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
