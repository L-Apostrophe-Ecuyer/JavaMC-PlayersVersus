package frootloops.versus.mixin.client.players;

import com.mojang.blaze3d.systems.RenderSystem;
import frootloops.versus.VersusModClient;
import frootloops.versus.VersusSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderLayer;
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
        HungerManager hungerManager = player.getHungerManager();
        int playerFoodLevel = hungerManager.getFoodLevel();
        int foodPointsAvailable = !VersusSettings.Combat.DO_FOOD_OVERHAUL ? 20 : Math.max((int) (player.getMaxHealth() - player.getHealth() + 6), playerFoodLevel);
        boolean makeIconsJiggle = (hungerManager.getSaturationLevel() == 0.0f && this.ticks % (playerFoodLevel * 3 + 1) == 0) || (VersusSettings.Combat.DO_FOOD_OVERHAUL && player.hasStatusEffect(StatusEffects.HUNGER));

        Identifier iconHaunchFull, iconHaunchHalf, iconHaunchEmpty, iconHaunchHalfDisabled, iconHaunchEmptyDisabled;
        if (player.hasStatusEffect(StatusEffects.HUNGER)) {
            iconHaunchEmpty = FOOD_EMPTY_HUNGER_TEXTURE;
            iconHaunchHalf = FOOD_HALF_HUNGER_TEXTURE;
            iconHaunchFull = FOOD_FULL_HUNGER_TEXTURE;
            iconHaunchEmptyDisabled = VersusModClient.HUD_TEXTURE_DISABLED_FOOD_HUNGER;
            iconHaunchHalfDisabled = VersusModClient.HUD_TEXTURE_DISABLED_FOOD_HALF_HUNGER;
        } else {
            iconHaunchEmpty = FOOD_EMPTY_TEXTURE;
            iconHaunchHalf = FOOD_HALF_TEXTURE;
            iconHaunchFull = FOOD_FULL_TEXTURE;
            iconHaunchEmptyDisabled = VersusModClient.HUD_TEXTURE_DISABLED_FOOD;
            iconHaunchHalfDisabled = VersusModClient.HUD_TEXTURE_DISABLED_FOOD_HALF;
        }
        int halfHaunchFoodValue;
        for (int renderedHaunch = 0; renderedHaunch < 10; ++renderedHaunch) {
            int y = top;
            if (makeIconsJiggle) y += this.random.nextInt(3) - 1;
            int x = left - renderedHaunch * 8 - 9;

            // Draw the border icon:
            halfHaunchFoodValue = renderedHaunch * 2 + 1;
            if(halfHaunchFoodValue < foodPointsAvailable) context.drawGuiTexture(RenderPipelines.GUI_TEXTURED,iconHaunchEmpty, x, y, 9, 9);
            else if(halfHaunchFoodValue == foodPointsAvailable) context.drawGuiTexture(RenderPipelines.GUI_TEXTURED,iconHaunchHalfDisabled, x, y, 9, 9);
            else context.drawGuiTexture(RenderPipelines.GUI_TEXTURED,iconHaunchEmptyDisabled, x, y, 9, 9);

            // Draw the food points icon:
            if (halfHaunchFoodValue < playerFoodLevel) context.drawGuiTexture(RenderPipelines.GUI_TEXTURED,iconHaunchFull, x, y, 9, 9);
            else if (halfHaunchFoodValue == playerFoodLevel) context.drawGuiTexture(RenderPipelines.GUI_TEXTURED,iconHaunchHalf, x, y, 9, 9);
        }
    }
}
