package frootloops.versus.mixin.client.players.death;

import frootloops.versus.mod.players.death.RespawnNearbyPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(DeathScreen.class)
public class DeathScreenMixin extends Screen {
    protected DeathScreenMixin(Component title, boolean isHardcore, List<Button> buttons) {
        super(title);
        this.hardcore = isHardcore;
        this.exitButtons = buttons;
    }

    @Shadow private final boolean hardcore;
    @Shadow private final List<Button> exitButtons;

    @ModifyConstant(method = "init", constant = @Constant(intValue = 72))
    private int lowerRespawnButton(int height) {
        if(!minecraft.player.level().dimension().equals(Level.OVERWORLD)) return 72;
        else return 84;
    }

    @ModifyConstant(method = "init", constant = @Constant(intValue = 96))
    private int lowerTitleButton(int height) {
        if(!minecraft.player.level().dimension().equals(Level.OVERWORLD)) return 96;
        else return 108;
    }

    @ModifyConstant(method = "init", constant = @Constant(stringValue = "deathScreen.respawn"))
    private String newMessageRespawn(String msg) {
        return "players-versus.deathScreen.respawnHome";
    }

    @ModifyConstant(method = "init", constant = @Constant(stringValue = "deathScreen.spectate"))
    private String newMessageSpectate(String msg) {
        return "players-versus.deathScreen.spectateHome";
    }

    @Inject(method = "init", at = @At("TAIL"), cancellable = false)
    public void addRespawnNearbyButton(CallbackInfo info) {
        if(minecraft.player.level().dimension().equals(Level.OVERWORLD)) {
            MutableComponent text = this.hardcore ? Component.translatable("players-versus.deathScreen.spectateNearby") : Component.translatable("players-versus.deathScreen.respawnNearby");
            this.exitButtons.add(this.addRenderableWidget(Button.builder(text, button -> {
                this.minecraft.player.respawn();
                ClientPlayNetworking.send(new RespawnNearbyPayload(this.minecraft.player.getUUID()));
                button.active = false;
            }).bounds(this.width / 2 - 100, this.height / 4 + 60, 200, 20).build()));
            this.exitButtons.get(this.exitButtons.size() - 1).active = false;
        }
    }
}
