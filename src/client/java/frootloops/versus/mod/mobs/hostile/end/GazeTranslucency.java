package frootloops.versus.mod.mobs.hostile.end;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;

/**
 * The warped (endermen, endermites, watchlings) and phantoms show only to a stare: always see-through, a shimmer at
 * the edge of sight, filling in smoothly as the middle of the view turns onto them, whole when stared at (Gaze). Their
 * glowing eyes fade with them; what an enderman carries shows only once it's mostly seen; their shadows fade
 * (LivingEntityRendererGazeMixin, EyesLayerGazeMixin, CarriedBlockLayerGazeMixin).
 */
@Environment(EnvType.CLIENT)
public final class GazeTranslucency {
    /** A carried block (which can't be drawn see-through) shows from this opaque. */
    public static final float CARRIED_BLOCK_FROM = 0.6F;

    private GazeTranslucency() {}

    public static boolean faded(LivingEntity entity) {
        return entity instanceof Enderman || entity instanceof Endermite || entity instanceof Phantom;
    }

    /** How opaque to draw the entity now, as the camera's view sees it. */
    public static float opacity(LivingEntity entity, float partialTick) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null || camera == entity) return 1.0F;
        Vec3 middle = entity.getPosition(partialTick).add(0.0, entity.getBbHeight() / 2.0, 0.0);
        double radius = Math.max(entity.getBbWidth(), entity.getBbHeight()) / 2.0;
        return Gaze.opacity(Gaze.visibility(camera.getViewVector(partialTick), middle.subtract(camera.getEyePosition(partialTick)), radius));
    }
}
