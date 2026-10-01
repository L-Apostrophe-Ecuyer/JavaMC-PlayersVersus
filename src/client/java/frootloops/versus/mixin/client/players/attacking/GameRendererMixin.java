package frootloops.versus.mixin.client.players.attacking;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

// GameRenderer#pick moved to LocalPlayer (static) in 26.3, called by raycastHitResult.
@Mixin(LocalPlayer.class)
public abstract class GameRendererMixin {

    @Overwrite
    private static HitResult pick(Entity camera, double blockInteractionRange, double entityInteractionRange, float tickDelta) {
        Vec3 cameraPos = camera.getEyePosition(tickDelta);
        Vec3 cameraRotation = camera.getViewVector(tickDelta);
        Vec3 pointingVect = cameraPos.add(cameraRotation.x * blockInteractionRange, cameraRotation.y * blockInteractionRange, cameraRotation.z * blockInteractionRange);
        BlockHitResult blockHitResult = camera.level().clip(new ClipContext(cameraPos, pointingVect, ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE, camera));

        // Ensure block pos is in range (it should be, but oh well)
        Vec3 distVect = cameraPos.vectorTo(blockHitResult.getLocation());
        double squareDistToBlock = distVect.lengthSqr();
        if(squareDistToBlock > blockInteractionRange * blockInteractionRange) {
            Direction direction = Direction.getApproximateNearest(distVect);
            blockHitResult = BlockHitResult.miss(blockHitResult.getLocation(), direction, blockHitResult.getBlockPos());
        }

        Vec3 rotationVec = camera.getViewVector(tickDelta);
        Vec3 targetPosVec = cameraPos.add(rotationVec.x * entityInteractionRange, rotationVec.y * entityInteractionRange, rotationVec.z * entityInteractionRange);
        AABB box = camera.getBoundingBox().expandTowards(rotationVec.scale(entityInteractionRange)).inflate(1.0, 1.0, 1.0);
        EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(camera, cameraPos, targetPosVec, box, EntitySelector.CAN_BE_PICKED, (entityInteractionRange * entityInteractionRange));

        // If no entity targetted, or if out of range:
        if(entityHitResult == null || !entityHitResult.getLocation().closerThan(cameraPos, entityInteractionRange)) return blockHitResult;

        // If the entity is closer to targetted block, set it as the target:
        double squaredDistToEntity = entityHitResult.getLocation().distanceToSqr(cameraPos);
        if(squaredDistToEntity < squareDistToBlock || squareDistToBlock - squaredDistToEntity > 4.0) return entityHitResult;

        // Otherwise, try to check if we can target the entity through grass:
        Block targettedBlock = camera.level().getBlockState(blockHitResult.getBlockPos()).getBlock();
        return (targettedBlock instanceof VegetationBlock || targettedBlock.defaultDestroyTime() == 0.0f) ? entityHitResult : blockHitResult;
    }
}
