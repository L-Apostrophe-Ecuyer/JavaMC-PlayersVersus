package frootloops.versus.mixin.client.mobs.hostile;

import frootloops.versus.mod.mobs.hostile.end.EndermanShulkerCarrier;
import frootloops.versus.mod.mobs.hostile.end.ShulkerCarrierRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EndermanRenderer;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.world.entity.monster.Enderman;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Notes on an enderman's render state whether it carries a shulker (EndermanModelShulkerMixin poses its arms for it). */
@Environment(EnvType.CLIENT)
@Mixin(EndermanRenderer.class)
public abstract class EndermanRendererShulkerMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/Enderman;Lnet/minecraft/client/renderer/entity/state/EndermanRenderState;F)V", at = @At("TAIL"))
    private void playersVersus$carriesShulker(Enderman enderman, EndermanRenderState state, float partialTick, CallbackInfo info) {
        ((ShulkerCarrierRenderState) state).playersVersus$setCarriesShulker(EndermanShulkerCarrier.carries(enderman));
    }
}
