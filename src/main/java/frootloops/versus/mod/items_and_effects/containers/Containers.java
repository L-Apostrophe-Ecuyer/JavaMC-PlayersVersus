package frootloops.versus.mod.items_and_effects.containers;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.ItemContainerContents;

/** Bundles and shulker boxes: the payloads, and the server's side of them (the wheel's selection, the peek). */
public final class Containers {

    private Containers() {
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(SelectShulkerSlotPayload.TYPE, SelectShulkerSlotPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PeekShulkerPayload.TYPE, PeekShulkerPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TakeFromShulkerPayload.TYPE, TakeFromShulkerPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ShulkerPeekPayload.TYPE, ShulkerPeekPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SelectShulkerSlotPayload.TYPE, (payload, context) ->
                ShulkerSelection.selectInMenu(context.player().containerMenu, payload.menuSlot(), payload.boxSlot()));
        ServerPlayNetworking.registerGlobalReceiver(PeekShulkerPayload.TYPE, (payload, context) -> answer(context.player(), payload.pos()));
        ServerPlayNetworking.registerGlobalReceiver(TakeFromShulkerPayload.TYPE, (payload, context) -> {
            ShulkerPeeking.take(context.player(), payload.pos(), payload.slot(), payload.item());
            answer(context.player(), payload.pos());
        });
    }

    /** What the box the player looks at holds, or nothing (an empty box, or one they couldn't open). */
    private static void answer(ServerPlayer player, BlockPos pos) {
        ServerPlayNetworking.send(player, new ShulkerPeekPayload(pos, ShulkerPeeking.contents(player, pos).orElse(ItemContainerContents.EMPTY)));
    }
}
