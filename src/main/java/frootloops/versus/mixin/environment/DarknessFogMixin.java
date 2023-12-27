package frootloops.versus.mixin.environment;

import net.minecraft.block.Blocks;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(BackgroundRenderer.class)
public class DarknessFogMixin {

    @Inject(method = "getFogModifier", at = @At("HEAD"), cancellable = true)
    private static void noMoreDarknessFog(Entity entity, float tickDelta, CallbackInfoReturnable cir) {
        if (entity instanceof PlayerEntity player) {
            if(player.hasStatusEffect(StatusEffects.DARKNESS))
                if(!player.hasStatusEffect(StatusEffects.BLINDNESS))
                    cir.cancel();
        }
    }

}
