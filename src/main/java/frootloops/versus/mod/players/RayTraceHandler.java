package frootloops.versus.mod.players;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

// Implemented from: https://github.com/CloudG360/BridgingMod/tree/1.19/src/main/java/me/cg360/mod/placement/raytrace
// By CloudG360 and Team Abnormals

public class RayTraceHandler {

    public static HitResult rayTrace(Entity entity, Level world, Player player, ClipContext.Block blockMode, ClipContext.Fluid fluidMode) {
        return rayTrace(entity, world, player, blockMode, fluidMode, player.blockInteractionRange());
    }

    public static HitResult rayTrace(Entity entity, Level world, Entity player, ClipContext.Block blockMode, ClipContext.Fluid fluidMode, double range) {
        Params params = getEntityParams(player);
        return rayTrace(entity, world, params.start(), params.direction(), blockMode, fluidMode, range);
    }

    public static HitResult rayTrace(Entity entity, Level world, Vec3 startPos, Vec3 ray, ClipContext.Block blockMode, ClipContext.Fluid fluidMode, double range) {
        return rayTrace(entity, world, startPos, startPos.add(ray.scale(range)), blockMode, fluidMode);
    }

    public static HitResult rayTrace(Entity entity, Level world, Vec3 startPos, Vec3 endPos, ClipContext.Block blockMode, ClipContext.Fluid fluidMode) {
        ClipContext context = new ClipContext(startPos, endPos, blockMode, fluidMode, entity);

        return world.clip(context);
    }

    /** Where a ray starts and which way it points. */
    public record Params(Vec3 start, Vec3 direction) {}

    /**
     * @param player - the player entity using the raycast guide.
     * @return the ray's starting position and its direction
     */
    public static Params getEntityParams(Entity player) {
        float pitch = player.xRotO + (player.getXRot() - player.xRotO);
        float yaw = player.yRotO + (player.getYRot() - player.yRotO);
        Vec3 pos = player.position();
        double posX = player.xo + (pos.x - player.xo);
        double posY = player.yo + (pos.y - player.yo);
        if (player instanceof Player) posY += player.getEyeHeight(player.getPose());
        double posZ = player.zo + (pos.z - player.zo);
        Vec3 rayPos = new Vec3(posX, posY, posZ);

        float zYaw = -Mth.cos(yaw * (float) Math.PI / 180);
        float xYaw = Mth.sin(yaw * (float) Math.PI / 180);
        float pitchMod = -Mth.cos(pitch * (float) Math.PI / 180);
        float azimuth = -Mth.sin(pitch * (float) Math.PI / 180);
        float xLen = xYaw * pitchMod;
        float yLen = zYaw * pitchMod;
        Vec3 ray = new Vec3(xLen, azimuth, yLen);

        return new Params(rayPos, ray);
    }
}