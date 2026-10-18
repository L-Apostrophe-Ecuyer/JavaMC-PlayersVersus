package frootloops.versus.mixin.environment.blocks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import frootloops.versus.mod.environment.blocks.leaves.LeafLitter;
import frootloops.versus.mod.environment.blocks.leaves.LeafMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Moving through leaves ({@link LeafMovement}), where vanilla moves a living thing: walking, climbing, falling and
 * gliding through them, and how far a fall through them, or into leaf litter, counts.
 */
@Mixin(LivingEntity.class)
public abstract class LeafMovementMixin {

    /**
     * {@code handleOnClimbable} turns the motion after this tick's input into the one the entity moves with (vanilla
     * clamps it there on ladders and scaffolding): leaves cap, drag, climb and sink it there too. It only runs where the
     * entity is simulated, the client for its player and the server for mobs.
     */
    @ModifyReturnValue(method = "handleOnClimbable", at = @At("RETURN"))
    private Vec3 playersVersus$moveThroughLeaves(Vec3 motion) {
        return LeafMovement.adjust((LivingEntity) (Object) this, motion);
    }

    /** A glider's motion for this tick, before it moves: leaves slow it on every axis. */
    @ModifyReturnValue(method = "updateFallFlyingMovement", at = @At("RETURN"))
    private Vec3 playersVersus$glideThroughLeaves(Vec3 motion) {
        return LeafMovement.glide((LivingEntity) (Object) this, motion);
    }

    /**
     * Before vanilla counts this tick's fall: in leaves a fall counts only as far as the speed it still has, and landing in
     * leaf litter takes some of it off. On both sides; the server counts a player's fall from the movement its client sends.
     */
    @Inject(method = "checkFallDamage", at = @At("HEAD"))
    private void playersVersus$cushionFallInLeaves(double dy, boolean onGround, BlockState state, BlockPos pos, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        LeafMovement.cushionFall(self, dy, onGround);
        LeafLitter.cushionLanding(self, onGround);
    }
}
