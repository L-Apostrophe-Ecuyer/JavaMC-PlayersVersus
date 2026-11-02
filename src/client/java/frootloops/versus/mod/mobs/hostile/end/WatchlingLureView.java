package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.VersusSettings;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A hunting watchling's gaze draws the eye (WatchlingLure): each tick, the nearest one hunting you within LURE_RANGE,
 * in plain sight, is the one that draws; each frame, as the mouse turns the view (MouseHandlerLureMixin), the view is
 * turned a little more onto it if it's already turned near it, smoothly, by the time the frame took. Never while a
 * screen is open, nor with a carved pumpkin over the eyes, nor in creative or spectator.
 */
@Environment(EnvType.CLIENT)
public final class WatchlingLureView {
    @Nullable
    private static WatchlingEntity drawing;
    private static double closeness;

    private WatchlingLureView() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(WatchlingLureView::tick);
    }

    private static void tick(Minecraft minecraft) {
        drawing = null;
        LocalPlayer player = minecraft.player;
        if (!VersusSettings.Watchlings.LURE_ENABLED || player == null || minecraft.level == null || !mayBeDrawn(player)) return;
        double range = VersusSettings.Watchlings.LURE_RANGE, nearest = range;
        for (WatchlingEntity watchling : minecraft.level.getEntitiesOfClass(WatchlingEntity.class, player.getBoundingBox().inflate(range))) {
            double distance = player.getEyePosition().distanceTo(watchling.getBoundingBox().getCenter());
            if (watchling.isHunting() && watchling.isAlive() && distance < nearest && player.hasLineOfSight(watchling)) {
                drawing = watchling;
                nearest = distance;
            }
        }
        closeness = WatchlingLure.closeness(nearest);
    }

    private static boolean mayBeDrawn(LocalPlayer player) {
        return !player.isCreative() && !player.isSpectator() && player.isAlive()
                && !player.getItemBySlot(EquipmentSlot.HEAD).is(ItemTags.GAZE_DISGUISE_EQUIPMENT);
    }

    /** A frame's turn of the view ({@code seconds} since the last), after the mouse's. */
    public static void frame(double seconds) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        WatchlingEntity watchling = drawing;
        if (player == null || watchling == null || watchling.isRemoved() || seconds <= 0.0) return;
        Vec3 eye = player.getEyePosition();
        Vec3 toward = watchling.getBoundingBox().getCenter().subtract(eye).normalize();
        double away = Math.toDegrees(Math.acos(Math.clamp(player.getViewVector(1.0F).dot(toward), -1.0, 1.0)));
        double strength = WatchlingLure.strength(away, closeness);
        if (strength <= 0.0) return;
        double flat = Math.sqrt(toward.x * toward.x + toward.z * toward.z);
        float yaw = (float) (Mth.atan2(toward.z, toward.x) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) -(Mth.atan2(toward.y, flat) * (180.0 / Math.PI));
        float[] turn = WatchlingLure.turn(Mth.wrapDegrees(yaw - player.getYRot()), pitch - player.getXRot(),
                VersusSettings.Watchlings.LURE_SPEED * strength * Math.min(seconds, 0.1));
        // Entity#turn is what the mouse calls: it scales by 0.15 itself
        player.turn(turn[0] / 0.15, turn[1] / 0.15);
    }
}
