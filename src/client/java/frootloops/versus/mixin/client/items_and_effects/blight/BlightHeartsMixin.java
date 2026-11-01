package frootloops.versus.mixin.client.items_and_effects.blight;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import frootloops.versus.VersusMod;
import frootloops.versus.mod.items_and_effects.blight.Blight;
import frootloops.versus.mod.items_and_effects.blight.Undeath;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blight's white hearts: the last of the health bar's hearts, one per amplifier + 1, drawn white, full, half or
 * empty as the health in them. Vanilla draws the bar from its last heart to its first, each heart's container first; the
 * containers drawn so far say which heart each sprite is for.
 */
@Environment(EnvType.CLIENT)
@Mixin(Hud.class)
public abstract class BlightHeartsMixin {
    @Unique private static final Identifier CONTAINER = sprite("container"), CONTAINER_HARDCORE = sprite("container_hardcore");
    @Unique private static final Identifier FULL = sprite("full"), HALF = sprite("half");
    @Unique private static final Identifier HARDCORE_FULL = sprite("hardcore_full"), HARDCORE_HALF = sprite("hardcore_half");

    /** The bar's hearts, the white ones among them, the heart drawn now and the next container's. */
    @Unique private int playersVersus$hearts, playersVersus$white, playersVersus$heart = -1, playersVersus$nextHeart = -1;

    @Unique
    private static Identifier sprite(String name) {
        return Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "hud/heart/blight_" + name);
    }

    @Inject(method = "extractHearts", at = @At("HEAD"))
    private void playersVersus$countHearts(GuiGraphicsExtractor graphics, Player player, int x, int y, int rowHeight, int regenerating,
                                           float maxHealth, int health, int displayHealth, int absorption, boolean blinking, CallbackInfo info) {
        this.playersVersus$hearts = Blight.hearts(maxHealth);
        this.playersVersus$white = Undeath.whiteHearts(player);
        this.playersVersus$nextHeart = this.playersVersus$hearts + (int) Math.ceil(absorption / 2.0) - 1;
        this.playersVersus$heart = -1;
    }

    @WrapOperation(method = "extractHearts", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Hud;extractHeart(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Hud$HeartType;IIZZZ)V"))
    private void playersVersus$whiteHeart(Hud hud, GuiGraphicsExtractor graphics, Hud.HeartType type, int x, int y, boolean hardcore,
                                          boolean blinking, boolean half, Operation<Void> original) {
        boolean container = type == Hud.HeartType.CONTAINER;
        if (container) this.playersVersus$heart = this.playersVersus$nextHeart--;
        if (this.playersVersus$white <= 0 || !Blight.isWhite(this.playersVersus$heart, this.playersVersus$hearts, this.playersVersus$white)) {
            original.call(hud, graphics, type, x, y, hardcore, blinking, half);
            return;
        }
        Identifier sprite = container ? (hardcore ? CONTAINER_HARDCORE : CONTAINER)
                : hardcore ? (half ? HARDCORE_HALF : HARDCORE_FULL) : (half ? HALF : FULL);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 9, 9);
    }
}
