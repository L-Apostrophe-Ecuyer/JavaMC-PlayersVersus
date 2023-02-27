package frootloops.mobs.mixin.hostile_mobs.dragon;

import frootloops.mobs.util.bosses.DragonManager;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.AbstractPhase;
import net.minecraft.entity.boss.dragon.phase.ChargingPlayerPhase;
import net.minecraft.entity.boss.dragon.phase.Phase;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChargingPlayerPhase.class)
public abstract class ChargingPlayerPhaseMixin extends AbstractPhase {
	@Shadow
	private int chargingTicks;
	@Shadow
	private Vec3d pathTarget;

	public ChargingPlayerPhaseMixin(EnderDragonEntity dragonIn) {
		super(dragonIn);
	}

	@Override
	public void serverTick() {
		if (this.pathTarget == null) {
			this.dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
		} else if (this.chargingTicks > 0 && this.chargingTicks++ >= 10) {

			// If must not charge or fireball then go back to holding pattern
			if (!DragonManager.onPhaseEnd(this.dragon))
				this.dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);

			// Otherwise reset the phase, in case she charges again
			else this.chargingTicks = -10;

		} else {
			double d0 = this.pathTarget.squaredDistanceTo(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
			if (d0 < 100.0D || d0 > 22500.0D || this.dragon.horizontalCollision || this.dragon.verticalCollision) {
				++this.chargingTicks;
			}

		}
	}

	@Inject(at = @At("HEAD"), method = "getMaxYAcceleration()F", cancellable = true)
	private void getFlySpeed(CallbackInfoReturnable<Float> callback) {
		callback.setReturnValue(24f);
	}

	@Override
	public PhaseType<? extends Phase> getType() {
		return PhaseType.CHARGING_PLAYER;
	}
}