package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;

/**
 * The pull of a watchling that hunts: now and then, for a moment, the view of whoever it hunts turns towards it, as a
 * hand on the cursor, whether they mean to look or not (and looking is what it wants). Rarer far off, sooner and
 * stronger the nearer; never in a menu, nor with a carved pumpkin over the eyes, nor in creative or spectator. It turns
 * the view as the mouse does, so a hand on the mouse can fight it.
 */
@Environment(EnvType.CLIENT)
public final class WatchlingPull {
    private static final RandomSource RANDOM = RandomSource.create();
    /** Ticks until the next pull, and ticks left of this one. */
    private static int gap = firstGap(), pulling;

    private WatchlingPull() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(WatchlingPull::tick);
    }

    /** How strong a pull is from this far (0 at PULL_RANGE and beyond, 1 at nothing). */
    public static float closeness(double distance) {
        return (float) Mth.clamp(1.0 - distance / VersusSettings.Watchlings.PULL_RANGE, 0.0, 1.0);
    }

    /** The yaw and pitch (degrees) that face a point from an eye. */
    public static float[] facing(Vec3 eye, Vec3 point) {
        Vec3 delta = point.subtract(eye);
        double flat = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) -(Mth.atan2(delta.y, flat) * (180.0 / Math.PI));
        return new float[]{yaw, pitch};
    }

    private static int firstGap() {
        return VersusSettings.Watchlings.PULL_GAP_MIN;
    }

    private static void tick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (!VersusSettings.Watchlings.PULL_ENABLED || player == null || minecraft.level == null || minecraft.screen != null
                || minecraft.isPaused() || player.isCreative() || player.isSpectator() || !player.isAlive()
                || player.getItemBySlot(EquipmentSlot.HEAD).is(ItemTags.GAZE_DISGUISE_EQUIPMENT)) {
            pulling = 0;
            return;
        }
        WatchlingEntity hunter = null;
        double nearest = VersusSettings.Watchlings.PULL_RANGE;
        for (WatchlingEntity watchling : minecraft.level.getEntitiesOfClass(WatchlingEntity.class, player.getBoundingBox().inflate(VersusSettings.Watchlings.PULL_RANGE))) {
            double distance = player.getEyePosition().distanceTo(watchling.getBoundingBox().getCenter());
            if (watchling.isHunting() && watchling.isAlive() && distance < nearest) {
                hunter = watchling;
                nearest = distance;
            }
        }
        if (hunter == null) {
            pulling = 0;
            return;
        }
        float closeness = closeness(nearest);
        if (pulling > 0) {
            pulling--;
            pull(player, hunter, closeness);
        } else if (--gap <= 0) {
            int min = VersusSettings.Watchlings.PULL_MIN, max = VersusSettings.Watchlings.PULL_MAX;
            pulling = Math.round((min + RANDOM.nextInt(max - min + 1)) * (0.5F + closeness));
            int low = VersusSettings.Watchlings.PULL_GAP_MIN, high = VersusSettings.Watchlings.PULL_GAP_MAX;
            // Sooner the nearer
            gap = Math.max(20, Math.round((low + RANDOM.nextInt(high - low + 1)) * (1.0F - 0.6F * closeness)));
        }
    }

    private static void pull(LocalPlayer player, WatchlingEntity hunter, float closeness) {
        float[] want = facing(player.getEyePosition(), hunter.getBoundingBox().getCenter());
        float step = VersusSettings.Watchlings.PULL_DEGREES * (0.5F + closeness);
        float yaw = Mth.clamp(Mth.wrapDegrees(want[0] - player.getYRot()), -step, step);
        float pitch = Mth.clamp(want[1] - player.getXRot(), -step, step);
        // Entity#turn is what the mouse calls: it scales by 0.15 itself
        player.turn(yaw / 0.15, pitch / 0.15);
    }
}
