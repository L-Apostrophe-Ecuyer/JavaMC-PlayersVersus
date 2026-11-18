package frootloops.versus.mod.items_and_effects.containers;

import frootloops.versus.VersusMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** Client to server: the wheel selected this slot (-1 for none) of the shulker box in this slot of the open menu. */
public record SelectShulkerSlotPayload(int menuSlot, int boxSlot) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectShulkerSlotPayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "select_shulker_slot"));
    public static final StreamCodec<FriendlyByteBuf, SelectShulkerSlotPayload> CODEC = StreamCodec.ofMember((value, buf) -> {
        buf.writeVarInt(value.menuSlot);
        buf.writeVarInt(value.boxSlot);
    }, buf -> new SelectShulkerSlotPayload(buf.readVarInt(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
