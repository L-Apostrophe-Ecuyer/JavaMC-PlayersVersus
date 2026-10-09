package frootloops.versus.mod.mobs.hostile.overworld.climbing;

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
 * Clients turn a climbing spider's model to the surface it clings to ({@link SurfaceClimbing}): belly to the surface,
 * head the way it crawls, pivoting about the middle of its box and pressed against a wall, since its box stays upright
 * and wider than the spider is tall. Upright on the floor, it draws as usual.
 */
@Environment(EnvType.CLIENT)
public final class ClimbingRender {
    /** How upright a spider must be to draw as usual. */
    static final float UPRIGHT = 0.999F;

    private ClimbingRender() {
    }

    /** Fills a spider's render state with how it's turned this frame; its head turns with its body off the floor. */
    public static void extract(Spider spider, LivingEntityRenderState state, float partialTick) {
        ClimbRenderState climb = (ClimbRenderState) state;
        ClimbingSpider climber = (ClimbingSpider) spider;
        Vector3f normal = climber.playersVersus$surfaceNormal(partialTick);
        if (normal.y > UPRIGHT || spider.deathTime > 0) {
            climb.playersVersus$setClimb(null, climb.playersVersus$climbForward(), 0.0F);
            return;
        }
        Vector3f forward = alongSurface(climber.playersVersus$surfaceForward(partialTick), normal);
        if (forward == null) forward = alongSurface(Math.abs(normal.y) < 0.9F ? new Vector3f(0.0F, 1.0F, 0.0F) : new Vector3f(0.0F, 0.0F, 1.0F), normal);
        climb.playersVersus$setClimb(normal, forward, spider.getBbWidth());
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

    /** Turns a climbing spider's model in place of the usual turn to its yaw; returns false for anything upright. */
    public static boolean rotate(LivingEntityRenderState state, PoseStack poseStack) {
        ClimbRenderState climb = (ClimbRenderState) state;
        Vector3f normal = climb.playersVersus$climbNormal();
        if (normal == null) return false;
        Vector3f forward = climb.playersVersus$climbForward();
        float half = state.boundingBoxHeight / 2.0F;
        float sideways = (float) Math.sqrt(normal.x * normal.x + normal.z * normal.z);
        float gap = Math.max(climb.playersVersus$climbWidth() / 2.0F - half, 0.0F) * sideways;
        poseStack.translate(-normal.x * gap, half - normal.y * gap, -normal.z * gap);
        // Model up goes to the normal and model forward (-z) to the heading.
        Matrix3f basis = new Matrix3f(new Vector3f(forward).cross(normal), normal, new Vector3f(forward).negate());
        poseStack.rotate(basis.getNormalizedRotation(new Quaternionf()));
        poseStack.translate(0.0F, -half, 0.0F);
        return true;
    }
}
