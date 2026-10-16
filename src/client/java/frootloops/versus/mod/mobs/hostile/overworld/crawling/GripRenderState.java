package frootloops.versus.mod.mobs.hostile.overworld.crawling;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/** Implemented on every {@code LivingEntityRenderState} by a mixin: how a crawling spider is tilted this frame. */
@Environment(EnvType.CLIENT)
public interface GripRenderState {
    /** The way out of the surface the spider clings to, or null when it stands upright and draws as usual. */
    @Nullable
    Vector3f playersVersus$tiltNormal();

    /** The way the spider faces along its surface. */
    Vector3f playersVersus$tiltForward();

    /** The width of the spider's box. */
    float playersVersus$tiltWidth();

    void playersVersus$setTilt(@Nullable Vector3f normal, Vector3f forward, float width);
}
