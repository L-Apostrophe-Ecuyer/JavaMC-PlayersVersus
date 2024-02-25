package frootloops.versus.mixin.players.death;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.players.death.CustomRespawnRequestPayloadC2S;
import frootloops.versus.mod.players.death.RespawnNearLastDeath;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.impl.networking.CommonVersionPayload;
import net.fabricmc.fabric.impl.recipe.ingredient.CustomIngredientPayloadC2S;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ServerCommonPacketListener;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.World;
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
    protected DeathScreenMixin(Text title, boolean isHardcore, List<ButtonWidget> buttons) {
        super(title);
        this.isHardcore = isHardcore;
        this.buttons = buttons;
    }

    @Shadow private final boolean isHardcore;
    @Shadow private final List<ButtonWidget> buttons;

    @ModifyConstant(method = "init", constant = @Constant(intValue = 72))
    private int lowerRespawnButton(int height) {
        if(!client.player.getWorld().getRegistryKey().equals(World.OVERWORLD)) return 72;
        else return 84;
    }

    @ModifyConstant(method = "init", constant = @Constant(intValue = 96))
    private int lowerTitleButton(int height) {
        if(!client.player.getWorld().getRegistryKey().equals(World.OVERWORLD)) return 96;
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
        if(client.player.getWorld().getRegistryKey().equals(World.OVERWORLD) && false) {
            MutableText text = this.isHardcore ? Text.translatable("players-versus.deathScreen.spectateNearby") : Text.translatable("players-versus.deathScreen.respawnNearby");
            this.buttons.add(this.addDrawableChild(ButtonWidget.builder(text, button -> {
                this.client.player.requestRespawn();
                ClientPlayNetworking.send(new CustomRespawnRequestPayloadC2S(this.client.player.getUuid()));
                button.active = false;
            }).dimensions(this.width / 2 - 100, this.height / 4 + 60, 200, 20).build()));
            this.buttons.get(this.buttons.size() - 1).active = false;
        }
    }
}
