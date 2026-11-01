package frootloops.versus.mod.mobs.hostile.end;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.phys.Vec3;

/**
 * The warped (endermen, endermites, watchlings) show only to a stare: drawn see-through, as well as the camera sees them
 * (Gaze), so that a glance aside leaves nothing but a shimmer and the middle of the view brings them out. Their eyes and
 * what they carry show only once they're mostly seen; their shadows fade with them (LivingEntityRendererGazeMixin).
 */
@Environment(EnvType.CLIENT)
public final class GazeTranslucency {
    /** Layers (eyes, a carried block) show from this well seen. */
    public static final float LAYERS_FROM = 0.6F;

    private GazeTranslucency() {}

    public static boolean warped(LivingEntity entity) {
        return entity instanceof Enderman || entity instanceof Endermite;
    }

    /** How well the camera's view sees the entity now. */
    public static float seen(LivingEntity entity, float partialTick) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera == null || camera == entity) return 1.0F;
        Vec3 middle = entity.getPosition(partialTick).add(0.0, entity.getBbHeight() / 2.0, 0.0);
        double radius = Math.max(entity.getBbWidth(), entity.getBbHeight()) / 2.0;
        return Gaze.visibility(camera.getViewVector(partialTick), middle.subtract(camera.getEyePosition(partialTick)), radius);
    }
}
