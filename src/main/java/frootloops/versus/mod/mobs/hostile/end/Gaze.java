package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusSettings;
import net.minecraft.world.phys.Vec3;

/**
 * How well a gaze sees a warped thing (endermen, endermites, watchlings): they show only to a stare. Fully when the
 * middle of the view is on them (within SEEN_FULL_DEGREES of their edge), not at all SEEN_GONE_DEGREES aside, fading
 * between. The client draws them that see-through (GazeTranslucency); the server counts a watchling watched when at
 * least half seen (WatchlingStare), so its static and its sight agree.
 */
public final class Gaze {
    private Gaze() {}

    /**
     * How well a view (a direction) sees a thing this way off (from the eye to its middle), of this radius: 0 to 1.
     */
    public static float visibility(Vec3 view, Vec3 towards, double radius) {
        double distance = towards.length();
        double viewLength = view.length();
        if (distance < 1.0E-3 || viewLength < 1.0E-6) return 1.0F;
        double cos = Math.max(-1.0, Math.min(1.0, view.dot(towards) / (distance * viewLength)));
        double aside = Math.toDegrees(Math.acos(cos)) - Math.toDegrees(Math.atan2(radius, distance));
        return 1.0F - smoothstep(VersusSettings.Gaze.SEEN_FULL_DEGREES, VersusSettings.Gaze.SEEN_GONE_DEGREES, aside);
    }

    /** Whether a thing seen this well is watched. */
    public static boolean watched(float visibility) {
        return visibility >= VersusSettings.Gaze.WATCHED_AT;
    }

    private static float smoothstep(double from, double to, double x) {
        double t = Math.max(0.0, Math.min(1.0, (x - from) / (to - from)));
        return (float) (t * t * (3.0 - 2.0 * t));
    }
}
