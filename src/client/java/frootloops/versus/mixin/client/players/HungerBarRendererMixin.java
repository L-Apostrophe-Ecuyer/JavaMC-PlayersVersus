package frootloops.versus.mixin.client.players;

import com.mojang.blaze3d.systems.RenderSystem;
import frootloops.versus.VersusModClient;
import frootloops.versus.VersusSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Gui.class)
public class HungerBarRendererMixin {

    @Shadow private static final ResourceLocation FOOD_EMPTY_HUNGER_SPRITE = ResourceLocation.parse("hud/food_empty_hunger"), FOOD_HALF_HUNGER_SPRITE = ResourceLocation.parse("hud/food_half_hunger"), FOOD_FULL_HUNGER_SPRITE = ResourceLocation.parse("hud/food_full_hunger");
    @Shadow private static final ResourceLocation FOOD_EMPTY_SPRITE = ResourceLocation.parse("hud/food_empty"), FOOD_HALF_SPRITE = ResourceLocation.parse("hud/food_half"), FOOD_FULL_SPRITE = ResourceLocation.parse("hud/food_full");
    @Shadow private final RandomSource random = RandomSource.create();
    @Shadow private int tickCount;

    @Shadow @Nullable private Player getCameraPlayer() {return null;}

    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void renderFood(GuiGraphics context, Player player, int top, int left, CallbackInfo info) {
        if(!VersusSettings.Combat.DO_FOOD_OVERHAUL) return;

        FoodData hungerManager = player.getFoodData();
        int playerFoodLevel = hungerManager.getFoodLevel();
        int foodPointsAvailable = Math.max((int) (player.getMaxHealth() - player.getHealth() + 6), playerFoodLevel);
        boolean makeIconsJiggle = (hungerManager.getSaturationLevel() == 0.0f && this.tickCount % (playerFoodLevel * 3 + 1) == 0) || (VersusSettings.Combat.DO_FOOD_OVERHAUL && player.hasEffect(MobEffects.HUNGER));

        ResourceLocation iconHaunchFull, iconHaunchHalf, iconHaunchEmpty, iconHaunchHalfDisabled, iconHaunchEmptyDisabled;
        if (player.hasEffect(MobEffects.HUNGER)) {
            iconHaunchEmpty = FOOD_EMPTY_HUNGER_SPRITE;
            iconHaunchHalf = FOOD_HALF_HUNGER_SPRITE;
            iconHaunchFull = FOOD_FULL_HUNGER_SPRITE;
            iconHaunchEmptyDisabled = VersusModClient.HUD_TEXTURE_DISABLED_FOOD_HUNGER;
            iconHaunchHalfDisabled = VersusModClient.HUD_TEXTURE_DISABLED_FOOD_HALF_HUNGER;
        } else {
            iconHaunchEmpty = FOOD_EMPTY_SPRITE;
            iconHaunchHalf = FOOD_HALF_SPRITE;
            iconHaunchFull = FOOD_FULL_SPRITE;
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
            if(halfHaunchFoodValue < foodPointsAvailable) context.blitSprite(RenderPipelines.GUI_TEXTURED,iconHaunchEmpty, x, y, 9, 9);
            else if(halfHaunchFoodValue == foodPointsAvailable) context.blitSprite(RenderPipelines.GUI_TEXTURED,iconHaunchHalfDisabled, x, y, 9, 9);
            else context.blitSprite(RenderPipelines.GUI_TEXTURED,iconHaunchEmptyDisabled, x, y, 9, 9);

            // Draw the food points icon:
            if (halfHaunchFoodValue < playerFoodLevel) context.blitSprite(RenderPipelines.GUI_TEXTURED,iconHaunchFull, x, y, 9, 9);
            else if (halfHaunchFoodValue == playerFoodLevel) context.blitSprite(RenderPipelines.GUI_TEXTURED,iconHaunchHalf, x, y, 9, 9);
        }
        info.cancel();
    }
}
