package frootloops.versus.mixin.client.mobs.hostile;

import frootloops.versus.mod.mobs.hostile.overworld.crawling.GripRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries how a crawling spider is tilted this frame from its renderer to the model's rotation. */
@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateGripMixin implements GripRenderState {

    @Unique
    @Nullable
    private Vector3f playersVersus$tiltNormal;
    @Unique
    private Vector3f playersVersus$tiltForward = new Vector3f(0.0F, 0.0F, 1.0F);
    @Unique
    private float playersVersus$tiltWidth;

    @Override
    @Nullable
    public Vector3f playersVersus$tiltNormal() {
        return this.playersVersus$tiltNormal;
    }

    @Override
    public Vector3f playersVersus$tiltForward() {
        return this.playersVersus$tiltForward;
    }

    @Override
    public float playersVersus$tiltWidth() {
        return this.playersVersus$tiltWidth;
    }

    @Override
    public void playersVersus$setTilt(@Nullable Vector3f normal, Vector3f forward, float width) {
        this.playersVersus$tiltNormal = normal;
        this.playersVersus$tiltForward = forward;
        this.playersVersus$tiltWidth = width;
    }
}
