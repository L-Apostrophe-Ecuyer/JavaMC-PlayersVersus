package frootloops.versus.mixin.players.attacking;

import frootloops.versus.Main;
import frootloops.versus.util.Combat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(GameRenderer.class)
abstract class CrosshairRendererMixin implements SynchronousResourceReloader{
    @Shadow
    @Final
    private MinecraftClient client;

    private double playerAttackRange = 0.0d;

    @ModifyVariable(method = "updateTargetedEntity", at = @At("STORE"), ordinal = 0)
    private double modifyPlayerReachForCrosshairRendering(double reach) {
        playerAttackRange = Combat.getAttackRange(this.client.player);
        return playerAttackRange;
    }

    @ModifyVariable(method = "updateTargetedEntity", at = @At("STORE"), ordinal = 0)
    private EntityHitResult ensureEntityWithinReach(EntityHitResult entityHitResult) {
        if(entityHitResult != null) {
            double squaredDist = entityHitResult.getPos().squaredDistanceTo(client.getCameraEntity().getPos());
            if(squaredDist < (playerAttackRange * playerAttackRange)) return entityHitResult;
        }
        return null;
    }

    @ModifyConstant(method = "updateTargetedEntity", constant = @Constant(doubleValue = 3.0))
    private double getActualAttackRange0(final double attackRange) {
        if (this.client.player != null) {
            return playerAttackRange; //Combat.getAttackRange(this.client.player);
        }
        return attackRange;
    }

    @ModifyConstant(method = "updateTargetedEntity", constant = @Constant(doubleValue = 9.0))
    private double getActualAttackRange1(final double attackRange) {
        if (this.client.player != null) {
            double newAttackRange = playerAttackRange; //Combat.getAttackRange(this.client.player);
            return newAttackRange * newAttackRange;
        }
        return attackRange;
    }
}
