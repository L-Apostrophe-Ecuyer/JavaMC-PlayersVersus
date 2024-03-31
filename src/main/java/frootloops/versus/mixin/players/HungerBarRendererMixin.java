package frootloops.versus.mixin.players;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import org.spongepowered.asm.mixin.injection.Redirect;



@Mixin(value = InGameHud.class)
public class HungerBarRendererMixin {

    @Shadow private static final Identifier FOOD_EMPTY_HUNGER_TEXTURE = new Identifier("hud/food_empty_hunger");
    @Shadow private static final Identifier FOOD_EMPTY_TEXTURE = new Identifier("hud/food_empty");

    @Shadow @Nullable private PlayerEntity getCameraPlayer() {return null;}

    @Redirect(method = "renderStatusBars", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V"))
    public void checkOnEepyPlayers(DrawContext context, Identifier texture, int x, int y, int width, int height) {
        if(VersusSettings.DO_FOOD_OVERHAUL && (texture == FOOD_EMPTY_HUNGER_TEXTURE || texture == FOOD_EMPTY_TEXTURE)) {
            int n = context.getScaledWindowWidth() / 2 + 91;
            int hungerLevelMin = (n - x - 9)/4; // Reverse mathing the original 0-9 value of the x position, then multiplying by 2
            if(hungerLevelMin > 5) {
                PlayerEntity playerEntity = this.getCameraPlayer();
                int maxHungerLevel = Math.max((int) (6 + playerEntity.getMaxHealth() - playerEntity.getHealth()), playerEntity.getHungerManager().getFoodLevel());
                if(hungerLevelMin + 1 >= maxHungerLevel) {
                    if(hungerLevelMin >= maxHungerLevel) {
                        if(texture == FOOD_EMPTY_TEXTURE) texture = VersusMod.HUD_TEXTURE_DISABLED_FOOD;
                        else texture = VersusMod.HUD_TEXTURE_DISABLED_FOOD_HUNGER;
                    }
                    else {
                        if(texture == FOOD_EMPTY_TEXTURE) texture = VersusMod.HUD_TEXTURE_DISABLED_FOOD_HALF;
                        else texture = VersusMod.HUD_TEXTURE_DISABLED_FOOD_HALF_HUNGER;
                    }
                }
            }
        }
        context.drawGuiTexture(texture, x, y, 0, width, height);
    }
}
