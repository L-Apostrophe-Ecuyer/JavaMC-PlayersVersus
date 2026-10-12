package frootloops.versus.mixin.client.mobs.hostile;

import frootloops.versus.mod.mobs.hostile.overworld.crawling.SurfaceTilt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.monster.spider.Spider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hands a spider's grip on its surface to its render state (and cave spiders', whose renderer is a spider's). */
@Environment(EnvType.CLIENT)
@Mixin(SpiderRenderer.class)
public abstract class SpiderRendererGripMixin {

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/monster/spider/Spider;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void playersVersus$extractGrip(Spider spider, LivingEntityRenderState state, float partialTick, CallbackInfo info) {
        SurfaceTilt.extract(spider, state, partialTick);
    }
}
