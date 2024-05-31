package frootloops.versus.mixin.players;

import com.mojang.blaze3d.systems.RenderSystem;
import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public class HungerBarRendererMixin {

    @Shadow private static final Identifier FOOD_EMPTY_HUNGER_TEXTURE = Identifier.of("hud/food_empty_hunger"), FOOD_HALF_HUNGER_TEXTURE = Identifier.of("hud/food_half_hunger"), FOOD_FULL_HUNGER_TEXTURE = Identifier.of("hud/food_full_hunger");
    @Shadow private static final Identifier FOOD_EMPTY_TEXTURE = Identifier.of("hud/food_empty"), FOOD_HALF_TEXTURE = Identifier.of("hud/food_half"), FOOD_FULL_TEXTURE = Identifier.of("hud/food_full");
    @Shadow private final Random random = Random.create();
    @Shadow private int ticks;

    @Shadow @Nullable private PlayerEntity getCameraPlayer() {return null;}

    @Overwrite
    private void renderFood(DrawContext context, PlayerEntity player, int top, int left) {

        VersusMod.MOD_LOGGER.warn("Drawing hunger");

        RenderSystem.enableBlend();

        HungerManager hungerManager = player.getHungerManager();
        boolean hasNoSaturation = hungerManager.getSaturationLevel() == 0.0f;
        int playerFoodLevel = hungerManager.getFoodLevel();
        int foodPointsAvailable = !VersusSettings.DO_FOOD_OVERHAUL ? 20 : Math.max((int) (player.getMaxHealth() - player.getHealth() + 6), playerFoodLevel);

        Identifier iconHaunchFull, iconHaunchHalf, iconHaunchEmpty, iconHaunchHalfDisabled, iconHaunchEmptyDisabled;
        if (player.hasStatusEffect(StatusEffects.HUNGER)) {
            iconHaunchEmpty = FOOD_EMPTY_HUNGER_TEXTURE;
            iconHaunchHalf = FOOD_HALF_HUNGER_TEXTURE;
            iconHaunchFull = FOOD_FULL_HUNGER_TEXTURE;
            iconHaunchEmptyDisabled = VersusMod.HUD_TEXTURE_DISABLED_FOOD_HUNGER;
            iconHaunchHalfDisabled = VersusMod.HUD_TEXTURE_DISABLED_FOOD_HALF_HUNGER;
        } else {
            iconHaunchEmpty = FOOD_EMPTY_TEXTURE;
            iconHaunchHalf = FOOD_HALF_TEXTURE;
            iconHaunchFull = FOOD_FULL_TEXTURE;
            iconHaunchEmptyDisabled = VersusMod.HUD_TEXTURE_DISABLED_FOOD;
            iconHaunchHalfDisabled = VersusMod.HUD_TEXTURE_DISABLED_FOOD_HALF;
        }
        int halfHaunchFoodValue;
        for (int renderedHaunch = 0; renderedHaunch < 10; ++renderedHaunch) {
            int y = top;
            if (hasNoSaturation && this.ticks % (playerFoodLevel * 3 + 1) == 0) y += this.random.nextInt(3) - 1;
            int x = left - renderedHaunch * 8 - 9;

            // Draw the border icon:
            halfHaunchFoodValue = renderedHaunch * 2 + 1;
            if(halfHaunchFoodValue < foodPointsAvailable) context.drawGuiTexture(iconHaunchEmpty, x, y, 9, 9);
            else if(halfHaunchFoodValue == foodPointsAvailable) context.drawGuiTexture(iconHaunchHalfDisabled, x, y, 9, 9);
            else context.drawGuiTexture(iconHaunchEmptyDisabled, x, y, 9, 9);

            // Draw the food points icon:
            if (halfHaunchFoodValue < playerFoodLevel) context.drawGuiTexture(iconHaunchFull, x, y, 9, 9);
            else if (halfHaunchFoodValue == playerFoodLevel) context.drawGuiTexture(iconHaunchHalf, x, y, 9, 9);
        }
        RenderSystem.disableBlend();
    }

    /*
    Works, but I opted to optimize performance. The above code is a lot better, and avoids unnecessary ops

    @Redirect(method = "renderFood", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V"))
    public void foodGUI(DrawContext context, Identifier texture, int x, int y, int width, int height) {
        if(VersusSettings.DO_FOOD_OVERHAUL && (texture == FOOD_EMPTY_HUNGER_TEXTURE || texture == FOOD_EMPTY_TEXTURE)) {
            int n = context.getScaledWindowWidth() / 2 + 91;
            int hungerLevelMin = (n - x - 9)/4; // Reverse mathing the original 0-9 value of the x position, then multiplying by 2
            if(hungerLevelMin > 5) {
                PlayerEntity playerEntity = this.getCameraPlayer();
                int maxHungerLevel = Math.max((int) (playerEntity.getMaxHealth() - playerEntity.getHealth()), playerEntity.getHungerManager().getFoodLevel());
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
    }*/
}
