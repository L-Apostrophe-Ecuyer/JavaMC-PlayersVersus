package frootloops.versus.mixin.client.players.attacking;

import frootloops.versus.mod.players.RayTraceHandler;
import frootloops.versus.mod.players.SmartBlockPlacement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.PlantBlock;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Overwrite
    private HitResult findCrosshairTarget(Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta) {
        Vec3d cameraPos = camera.getCameraPosVec(tickDelta);
        Vec3d cameraRotation = camera.getRotationVec(tickDelta);
        Vec3d pointingVect = cameraPos.add(cameraRotation.x * blockInteractionRange, cameraRotation.y * blockInteractionRange, cameraRotation.z * blockInteractionRange);
        BlockHitResult blockHitResult = camera.getWorld().raycast(new RaycastContext(cameraPos, pointingVect, RaycastContext.ShapeType.OUTLINE,RaycastContext.FluidHandling.NONE, camera));

        // Ensure block pos is in range (it should be, but oh well)
        Vec3d distVect = cameraPos.relativize(blockHitResult.getPos());
        double squareDistToBlock = distVect.lengthSquared();
        if(squareDistToBlock > blockInteractionRange * blockInteractionRange) {
            Direction direction = Direction.getFacing(distVect);
            blockHitResult = BlockHitResult.createMissed(blockHitResult.getPos(), direction, blockHitResult.getBlockPos());
        }

        Vec3d rotationVec = camera.getRotationVec(tickDelta);
        Vec3d targetPosVec = cameraPos.add(rotationVec.x * entityInteractionRange, rotationVec.y * entityInteractionRange, rotationVec.z * entityInteractionRange);
        Box box = camera.getBoundingBox().stretch(rotationVec.multiply(entityInteractionRange)).expand(1.0, 1.0, 1.0);
        EntityHitResult entityHitResult = ProjectileUtil.raycast(camera, cameraPos, targetPosVec, box, EntityPredicates.CAN_HIT, (entityInteractionRange * entityInteractionRange));
        if(!entityHitResult.getPos().isInRange(cameraPos, entityInteractionRange)) entityHitResult = null;

        // If no entity targetted:
        if(entityHitResult == null) return blockHitResult;

        // If the entity is closer to targetted block, set it as the target:
        double squaredDistToEntity = entityHitResult.getPos().squaredDistanceTo(cameraPos);
        if(squaredDistToEntity < squareDistToBlock || squareDistToBlock - squaredDistToEntity > 4.0) return entityHitResult;

        // Otherwise, try to check if we can target the entity through grass:
        Block targettedBlock = camera.getWorld().getBlockState(blockHitResult.getBlockPos()).getBlock();
        return (targettedBlock instanceof PlantBlock || targettedBlock.getHardness() == 0.0f) ? entityHitResult : blockHitResult;
    }
}
