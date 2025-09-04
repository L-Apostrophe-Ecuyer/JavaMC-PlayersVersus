package frootloops.versus.mod.players;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

// Implemented from: https://github.com/CloudG360/BridgingMod/tree/1.19/src/main/java/me/cg360/mod/placement/raytrace
// By CloudG360 and Team Abnormals

public class RayTraceHandler {

    public static HitResult rayTrace(Entity entity, World world, PlayerEntity player, RaycastContext.ShapeType blockMode, RaycastContext.FluidHandling fluidMode) {
        return rayTrace(entity, world, player, blockMode, fluidMode, player.getBlockInteractionRange());
    }

    public static HitResult rayTrace(Entity entity, World world, Entity player, RaycastContext.ShapeType blockMode, RaycastContext.FluidHandling fluidMode, double range) {
        Pair<Vec3d, Vec3d> params = getEntityParams(player);
        return rayTrace(entity, world, params.getLeft(), params.getRight(), blockMode, fluidMode, range);
    }

    public static HitResult rayTrace(Entity entity, World world, Vec3d startPos, Vec3d ray, RaycastContext.ShapeType blockMode, RaycastContext.FluidHandling fluidMode, double range) {
        return rayTrace(entity, world, startPos, startPos.add(ray.multiply(range)), blockMode, fluidMode);
    }

    public static HitResult rayTrace(Entity entity, World world, Vec3d startPos, Vec3d endPos, RaycastContext.ShapeType blockMode, RaycastContext.FluidHandling fluidMode) {
        RaycastContext context = new RaycastContext(startPos, endPos, blockMode, fluidMode, entity);

        return world.raycast(context);
    }

    /**
     * @param player - the player entity using the raycast guide.
     * @return Pair | Left = Starting position, Right = Direction
     */
    public static Pair<Vec3d, Vec3d> getEntityParams(Entity player) {
        float pitch = player.lastPitch + (player.getPitch() - player.lastPitch);
        float yaw = player.prevYaw + (player.getYaw() - player.prevYaw);
        Vec3d pos = player.getPos();
        double posX = player.prevX + (pos.x - player.prevX);
        double posY = player.prevY + (pos.y - player.prevY);
        if (player instanceof PlayerEntity) posY += player.getEyeHeight(player.getPose());
        double posZ = player.prevZ + (pos.z - player.prevZ);
        Vec3d rayPos = new Vec3d(posX, posY, posZ);

        float zYaw = -MathHelper.cos(yaw * (float) Math.PI / 180);
        float xYaw = MathHelper.sin(yaw * (float) Math.PI / 180);
        float pitchMod = -MathHelper.cos(pitch * (float) Math.PI / 180);
        float azimuth = -MathHelper.sin(pitch * (float) Math.PI / 180);
        float xLen = xYaw * pitchMod;
        float yLen = zYaw * pitchMod;
        Vec3d ray = new Vec3d(xLen, azimuth, yLen);

        return new Pair<>(rayPos, ray);
    }
}