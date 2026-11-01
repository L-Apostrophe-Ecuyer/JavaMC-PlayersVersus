package frootloops.versus.mixin.client.mobs.hostile;

import frootloops.versus.mod.mobs.hostile.end.GazeRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries how well the camera sees a warped entity this frame from its renderer's extraction to its drawing. */
@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateGazeMixin implements GazeRenderState {

    @Unique
    private float playersVersus$seen = 1.0F;

    @Override
    public float playersVersus$seen() {
        return this.playersVersus$seen;
    }

    @Override
    public void playersVersus$setSeen(float seen) {
        this.playersVersus$seen = seen;
    }
}
