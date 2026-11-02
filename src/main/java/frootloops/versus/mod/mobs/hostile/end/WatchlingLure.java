package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusSettings;

/**
 * The hold of a hunting watchling's gaze on a stare just begun (the client's WatchlingLureView turns the view by it; the
 * numbers are here, pure, to be tested). It grips only while the static rises (its eyes are seen) and is still thin,
 * under LURE_STATIC: then a view on its eyes, within LURE_DEGREES, is drawn onto them, harder the nearer the view and
 * the nearer it stands, so a stare begun is hard to break. Before that (unseen, or only glimpsed) and past it (the
 * static thick) the view is left alone, and a quick turn away always breaks free.
 */
public final class WatchlingLure {
    private WatchlingLure() {}

    /** Whether it grips: the static rising (its eyes seen) and still under LURE_STATIC. */
    public static boolean grips(boolean staticRising, float staticNow) {
        return staticRising && staticNow < VersusSettings.Watchlings.LURE_STATIC;
    }

    /** How hard the view is drawn (0 to 1.5): by how far from it the view is turned (degrees), and how near it stands (0 to 1). */
    public static double strength(double degreesAway, double closeness) {
        double cone = VersusSettings.Watchlings.LURE_DEGREES;
        if (degreesAway >= cone) return 0.0;
        double into = 1.0 - Math.max(0.0, degreesAway) / cone;
        return into * into * (0.5 + Math.clamp(closeness, 0.0, 1.0));
    }

    /** How near it stands, 0 at LURE_RANGE blocks and beyond, 1 at nothing. */
    public static double closeness(double distance) {
        return Math.clamp(1.0 - distance / VersusSettings.Watchlings.LURE_RANGE, 0.0, 1.0);
    }

    /**
     * The turn (yaw, pitch; degrees) towards it this frame: straight at it, by up to {@code degrees}, never past it.
     * {@code yawAway} and {@code pitchAway} are how far it is from where the view points.
     */
    public static float[] turn(float yawAway, float pitchAway, double degrees) {
        double away = Math.sqrt(yawAway * yawAway + pitchAway * pitchAway);
        if (away < 1.0E-4 || degrees <= 0.0) return new float[]{0.0F, 0.0F};
        double part = Math.min(1.0, degrees / away);
        return new float[]{(float) (yawAway * part), (float) (pitchAway * part)};
    }
}
