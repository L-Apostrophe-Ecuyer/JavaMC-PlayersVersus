package frootloops.versus.mixin.client.players;

import frootloops.versus.VersusMod;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.client.gui.screens.debug.DebugOptionsScreen$OptionList")
public class DebugOptionsScreenMixin {

    @Redirect(method = "updateSearch", at = @At(value = "INVOKE", target = "Ljava/lang/String;contains(Ljava/lang/CharSequence;)Z"))
    private boolean redirectContains(String pathString, CharSequence searchString) {
        Minecraft client = Minecraft.getInstance();
        boolean shouldRestrictDebug = !client.canSwitchGameMode() || (client.player.gameMode() != null && client.player.gameMode().isSurvival() && !client.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));
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
