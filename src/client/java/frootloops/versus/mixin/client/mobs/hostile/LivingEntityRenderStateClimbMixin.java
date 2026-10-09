package frootloops.versus.mixin.client.mobs.hostile;

import frootloops.versus.mod.mobs.hostile.overworld.climbing.ClimbRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries how a climbing spider is turned this frame from its renderer to the model's rotation. */
@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateClimbMixin implements ClimbRenderState {

    @Unique
    @Nullable
    private Vector3f playersVersus$climbNormal;
    @Unique
    private Vector3f playersVersus$climbForward = new Vector3f(0.0F, 0.0F, 1.0F);
    @Unique
    private float playersVersus$climbWidth;

    @Override
    @Nullable
    public Vector3f playersVersus$climbNormal() {
        return this.playersVersus$climbNormal;
    }

    @Override
    public Vector3f playersVersus$climbForward() {
        return this.playersVersus$climbForward;
    }

    @Override
    public float playersVersus$climbWidth() {
        return this.playersVersus$climbWidth;
    }

    @Override
    public void playersVersus$setClimb(@Nullable Vector3f normal, Vector3f forward, float width) {
        this.playersVersus$climbNormal = normal;
        this.playersVersus$climbForward = forward;
        this.playersVersus$climbWidth = width;
    }
}
