package frootloops.versus.mixin.client.items_and_effects.inventory;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(MultiPlayerGameMode.class)
public abstract class DebugInteractionManagerMixin {

    @Inject(method = "handleContainerInput",at = @At("HEAD"), cancellable = false)
    public void clickSlot(int syncId, int slotId, int button, ContainerInput actionType, Player player, CallbackInfo info) {
        VersusMod.MOD_LOGGER.warn("               * Clicked slot " + slotId + " with button " + button + " and action type: " + actionType.name());
    }
}
