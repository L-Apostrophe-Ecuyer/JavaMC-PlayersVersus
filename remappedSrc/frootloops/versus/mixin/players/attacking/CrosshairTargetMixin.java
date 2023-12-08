package frootloops.versus.mixin.players.attacking;

import frootloops.versus.mod.Combat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.resource.SynchronousResourceReloader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(GameRenderer.class)
abstract class CrosshairTargetMixin implements SynchronousResourceReloader{
    @Shadow
    @Final
    private MinecraftClient client;

    private double playerAttackRange = 0.0d;

    @ModifyConstant(method = "updateTargetedEntity", constant = @Constant(doubleValue = 3.0))
    private double getActualAttackRange(final double attackRange) {
        playerAttackRange = Combat.getAttackRange(this.client.player);
        if (this.client.player != null) return playerAttackRange;
        return 3.0d;
    }

    @ModifyConstant(method = "updateTargetedEntity", constant = @Constant(doubleValue = 9.0))
    private double getActualAttackRangeSquared(final double attackRange) {
        if (this.client.player != null) return playerAttackRange * playerAttackRange;
        return 9.0d;
    }
}
