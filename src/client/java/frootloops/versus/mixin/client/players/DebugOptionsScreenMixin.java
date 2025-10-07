package frootloops.versus.mixin.client.players;

import frootloops.versus.VersusMod;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.gui.screen.DebugOptionsScreen$OptionsListWidget")
public class DebugOptionsScreenMixin {

    @Redirect(method = "fillEntries", at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"))
    private boolean redirectContains(String pathString, CharSequence searchString) {
        MinecraftClient client = MinecraftClient.getInstance();
        boolean shouldRestrictDebug = !client.canSwitchGameMode() || (client.player.getGameMode() != null && client.player.getGameMode().isSurvivalLike()) || !client.player.hasPermissionLevel(2);
        if(shouldRestrictDebug) {
            switch (pathString) {
                case "biome":
                case "light_levels":
                case "player_position":
                case "chunk_borders":
                case "entity_hitboxes":
                    return true;
            }
            return false;
        }
        return pathString.contains(searchString);
    }
}
