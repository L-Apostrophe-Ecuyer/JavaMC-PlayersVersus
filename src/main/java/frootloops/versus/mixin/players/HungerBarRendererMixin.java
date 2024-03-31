package frootloops.versus.mixin.players;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.hud.InGameHud;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.client.gui.DrawableHelper.drawTexture;


@Mixin(value = InGameHud.class)
public abstract class HungerBarRendererMixin {

    public HungerBarRendererMixin(int scaledWidth, int scaledHeight, int ticks, Random random) {
        this.scaledWidth = scaledWidth;
        this.scaledHeight = scaledHeight;
        this.ticks = ticks;
        this.random = random;
    }

    @Shadow @Nullable private PlayerEntity getCameraPlayer() {return null;}

    @Shadow private int scaledWidth;
    @Shadow private int scaledHeight;
    @Shadow private int ticks;
    @Shadow private final Random random;

    @Inject(method = "renderStatusBars", at = @At(value = "TAIL"))
    public void renderNewHungerBar(MatrixStack matrices, CallbackInfo info) {
        if(VersusSettings.DO_FOOD_OVERHAUL) {

            PlayerEntity playerEntity = this.getCameraPlayer();
            if(playerEntity == null) return;

            HungerManager hungerManager = playerEntity.getHungerManager();
            int foodLevel = hungerManager.getFoodLevel();
            if(foodLevel == 20) return;

            RenderSystem.setShaderTexture(0, VersusMod.HUD_TEXTURE_OVERHAULED_FOOD);

            int hungerBarPosX = this.scaledWidth / 2 + 91;
            int hungerBarPosY = this.scaledHeight - 39;
            int maxHungerLevel = Math.max( 6 + (int) (playerEntity.getMaxHealth() - playerEntity.getHealth()), playerEntity.getHungerManager().getFoodLevel());
            boolean wiggleHungerBar = (hungerManager.getSaturationLevel() <= 0.0f && this.ticks % (foodLevel * 3 + 1) == 0);

            for (int i = 0; i < 10; ++i) {
                int hungerLevelMin = i * 2;
                int u = hungerLevelMin + 1 == maxHungerLevel ? 9 : maxHungerLevel <= hungerLevelMin ? 18 : 0;
                int v = playerEntity.hasStatusEffect(StatusEffects.HUNGER) ? 9 : 0;
                int x = hungerBarPosX - i * 8 - 9;
                int y =  wiggleHungerBar ? hungerBarPosY + this.random.nextInt(3) - 1 : hungerBarPosY;

                drawTexture(matrices, x, y, 0, u, v, 9, 9, 45, 18);
                if(hungerLevelMin + 1 < foodLevel) drawTexture(matrices, x, y, 0, 27, v, 9, 9, 45, 18);
                else if(hungerLevelMin + 1 == foodLevel) drawTexture(matrices, x, y, 0, 36, v, 9, 9, 45, 18);
            }
            RenderSystem.setShaderTexture(0, DrawableHelper.GUI_ICONS_TEXTURE);
        }
    }
}
