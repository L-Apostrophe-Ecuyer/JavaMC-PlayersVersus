package frootloops.versus.mixin.client.mobs.hostile;

import frootloops.versus.mod.mobs.hostile.end.ShulkerCarrierRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries whether an enderman holds a shulker from its renderer's extraction to its model. */
@Environment(EnvType.CLIENT)
@Mixin(EndermanRenderState.class)
public abstract class EndermanRenderStateShulkerMixin implements ShulkerCarrierRenderState {

    @Unique
    private boolean playersVersus$carriesShulker;

    @Override
    public boolean playersVersus$carriesShulker() {
        return this.playersVersus$carriesShulker;
    }

    @Override
    public void playersVersus$setCarriesShulker(boolean carries) {
        this.playersVersus$carriesShulker = carries;
    }
}
