package frootloops.versus.mixin.client.items_and_effects.blight;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import frootloops.versus.mod.items_and_effects.blight.Blight;
import frootloops.versus.mod.items_and_effects.blight.Undeath;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blight's hearts are the Wither's, which it replaces: the last of the health bar's hearts, one per amplifier + 1, are
 * drawn black (the Wither's own sprites, full or half, hardcore or not, blinking as it blinks). A blighted heart whose
 * health is lost is an empty container, as any lost heart is. Vanilla draws the bar from its last heart to its first,
 * each heart's container first; the containers drawn so far say which heart each sprite is for.
 */
@Environment(EnvType.CLIENT)
@Mixin(Hud.class)
public abstract class BlightHeartsMixin {
    /** The bar's hearts, the blighted ones among them, the heart drawn now and the next container's. */
    @Unique private int playersVersus$hearts, playersVersus$blighted, playersVersus$heart = -1, playersVersus$nextHeart = -1;

    @Inject(method = "extractHearts", at = @At("HEAD"))
    private void playersVersus$countHearts(GuiGraphicsExtractor graphics, Player player, int x, int y, int rowHeight, int regenerating,
                                           float maxHealth, int health, int displayHealth, int absorption, boolean blinking, CallbackInfo info) {
        this.playersVersus$hearts = Blight.hearts(maxHealth);
        this.playersVersus$blighted = Undeath.blightedHearts(player);
        this.playersVersus$nextHeart = this.playersVersus$hearts + (int) Math.ceil(absorption / 2.0) - 1;
        this.playersVersus$heart = -1;
    }

    @WrapOperation(method = "extractHearts", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Hud;extractHeart(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Hud$HeartType;IIZZZ)V"))
    private void playersVersus$blackHeart(Hud hud, GuiGraphicsExtractor graphics, Hud.HeartType type, int x, int y, boolean hardcore,
                                          boolean blinking, boolean half, Operation<Void> original) {
        boolean container = type == Hud.HeartType.CONTAINER;
        if (container) this.playersVersus$heart = this.playersVersus$nextHeart--;
        // A blighted heart's health is drawn as the Wither's; its container (all a lost heart shows) is any heart's
        boolean blighted = !container && this.playersVersus$blighted > 0
                && Blight.isBlighted(this.playersVersus$heart, this.playersVersus$hearts, this.playersVersus$blighted);
        original.call(hud, graphics, blighted ? Hud.HeartType.WITHERED : type, x, y, hardcore, blinking, half);
    }
}
