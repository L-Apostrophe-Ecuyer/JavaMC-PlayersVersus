package frootloops.versus.mixin.environment.blocks;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import frootloops.versus.mod.environment.blocks.leaves.LeafMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Moving through leaves ({@link LeafMovement}), where vanilla moves a living thing: walking, climbing, falling, swimming
 * and gliding through them, and how far a fall through them counts.
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

    /**
     * A swimmer's motion for this tick, as it moves: leaves underwater drag it, and the drag stays in its speed for water
     * to slow further, as it slows any swimmer. Where the entity is simulated, as walking.
     */
    @WrapOperation(method = "travelInWater", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private void playersVersus$swimThroughLeaves(LivingEntity self, MoverType type, Vec3 motion, Operation<Void> original) {
        Vec3 dragged = LeafMovement.swim(self, motion);
        if (dragged != motion) self.setDeltaMovement(dragged);
        original.call(self, type, dragged);
    }

    /** A glider's motion for this tick, before it moves: leaves slow it on every axis. */
    @ModifyReturnValue(method = "updateFallFlyingMovement", at = @At("RETURN"))
    private Vec3 playersVersus$glideThroughLeaves(Vec3 motion) {
        return LeafMovement.glide((LivingEntity) (Object) this, motion);
    }

    /**
     * Before vanilla counts this tick's fall: in leaves a fall counts only as far as the speed it still has. On both
     * sides; the server counts a player's fall from the movement its client sends.
     */
    @Inject(method = "checkFallDamage", at = @At("HEAD"))
    private void playersVersus$cushionFallInLeaves(double dy, boolean onGround, BlockState state, BlockPos pos, CallbackInfo ci) {
        LeafMovement.cushionFall((LivingEntity) (Object) this, dy, onGround);
    }
}
