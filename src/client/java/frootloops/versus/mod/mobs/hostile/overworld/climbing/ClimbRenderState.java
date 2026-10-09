package frootloops.versus.mod.mobs.hostile.overworld.climbing;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/** Implemented on every {@code LivingEntityRenderState} by a mixin: how a climbing spider is turned this frame. */
@Environment(EnvType.CLIENT)
public interface ClimbRenderState {
    /** The way out of the surface the spider clings to, or null when it stands upright and draws as usual. */
    @Nullable
    Vector3f playersVersus$climbNormal();

    /** The way the spider faces along its surface. */
    Vector3f playersVersus$climbForward();

    /** The width of the spider's box. */
    float playersVersus$climbWidth();

    void playersVersus$setClimb(@Nullable Vector3f normal, Vector3f forward, float width);
}
