package frootloops.versus.mod.mobs.hostile.overworld.crawling;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.monster.spider.Spider;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Clients tilt a crawling spider's model onto the surface it grips ({@link SurfaceGrip}): belly to the surface,
 * head the way it crawls, pivoting about the middle of its box and pressed against a wall, since its box stays upright
 * and wider than the spider is tall. Upright on the floor, it draws as usual.
 */
@Environment(EnvType.CLIENT)
public final class SurfaceTilt {
    /** How upright a spider must be to draw as usual. */
    static final float UPRIGHT = 0.999F;

    private SurfaceTilt() {
    }

    /** Fills a spider's render state with how it's turned this frame; its head turns with its body off the floor. */
    public static void extract(Spider spider, LivingEntityRenderState state, float partialTick) {
        GripRenderState tilt = (GripRenderState) state;
        SurfaceCrawler crawler = (SurfaceCrawler) spider;
        Vector3f normal = crawler.playersVersus$surfaceNormal(partialTick);
        if (normal.y > UPRIGHT || spider.deathTime > 0) {
            tilt.playersVersus$setTilt(null, tilt.playersVersus$tiltForward(), 0.0F);
            return;
        }
        Vector3f forward = alongSurface(crawler.playersVersus$surfaceForward(partialTick), normal);
        if (forward == null) forward = alongSurface(Math.abs(normal.y) < 0.9F ? new Vector3f(0.0F, 1.0F, 0.0F) : new Vector3f(0.0F, 0.0F, 1.0F), normal);
        tilt.playersVersus$setTilt(normal, forward, spider.getBbWidth());
        float upright = Math.max(normal.y, 0.0F);
        state.yRot *= upright;
        state.xRot *= upright;
    }

    /** A heading squared to the surface, or null if it points straight out of it. */
    @Nullable
    private static Vector3f alongSurface(Vector3f heading, Vector3f normal) {
        heading.sub(new Vector3f(normal).mul(heading.dot(normal)));
        return heading.lengthSquared() < 1.0E-4F ? null : heading.normalize();
    }

    /** Tilts a crawling spider's model in place of the usual turn to its yaw; returns false for anything upright. */
    public static boolean rotate(LivingEntityRenderState state, PoseStack poseStack) {
        GripRenderState tilt = (GripRenderState) state;
        Vector3f normal = tilt.playersVersus$tiltNormal();
        if (normal == null) return false;
        Vector3f forward = tilt.playersVersus$tiltForward();
        float half = state.boundingBoxHeight / 2.0F;
        float sideways = (float) Math.sqrt(normal.x * normal.x + normal.z * normal.z);
        float gap = Math.max(tilt.playersVersus$tiltWidth() / 2.0F - half, 0.0F) * sideways;
        poseStack.translate(-normal.x * gap, half - normal.y * gap, -normal.z * gap);
        // Model up goes to the normal and model forward (-z) to the heading.
        Matrix3f basis = new Matrix3f(new Vector3f(forward).cross(normal), normal, new Vector3f(forward).negate());
        poseStack.rotate(basis.getNormalizedRotation(new Quaternionf()));
        poseStack.translate(0.0F, -half, 0.0F);
        return true;
    }
}
