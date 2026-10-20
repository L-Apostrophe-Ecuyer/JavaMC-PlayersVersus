package frootloops.versus.mixin.client.environment.visuals;

import frootloops.versus.mod.environment.ClientTopples;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** On the client, a felled tree's falling block turns over with its tree (ClientTopples) instead of falling on its own. */
@Environment(EnvType.CLIENT)
@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityToppleMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void playersVersus$turnWithItsTree(CallbackInfo info) {
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (self.level().isClientSide() && ClientTopples.drive(self)) info.cancel();
    }
}
