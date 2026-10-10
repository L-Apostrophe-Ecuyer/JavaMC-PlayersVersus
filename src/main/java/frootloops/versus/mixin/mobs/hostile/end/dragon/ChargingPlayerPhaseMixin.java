package frootloops.versus.mixin.mobs.hostile.end.dragon;

import frootloops.versus.mod.mobs.hostile.end.DragonManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonChargePlayerPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DragonChargePlayerPhase.class)
public abstract class ChargingPlayerPhaseMixin extends AbstractDragonPhaseInstance {
	@Shadow
	private int timeSinceCharge;
	@Shadow
	private Vec3 targetLocation;

	public ChargingPlayerPhaseMixin(EnderDragon dragonIn) {
		super(dragonIn);
	}

	@Override
	public void doServerTick(ServerLevel world) {
		if (this.targetLocation == null) {
			this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);
		} else if (this.timeSinceCharge > 0 && this.timeSinceCharge++ >= 10) {

			// If must not charge or fireball then go back to holding pattern
			if (!DragonManager.onPhaseEnd(this.dragon))
				this.dragon.getPhaseManager().setPhase(EnderDragonPhase.HOLDING_PATTERN);

			// Otherwise reset the phase, in case she charges again
			else this.timeSinceCharge = -10;

		} else {
			double d0 = this.targetLocation.distanceToSqr(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
			if (d0 < 100.0D || d0 > 22500.0D || this.dragon.horizontalCollision || this.dragon.verticalCollision) {
				++this.timeSinceCharge;
			}

		}
	}

	@Inject(at = @At("HEAD"), method = "getFlySpeed()F", cancellable = true)
	private void getFlySpeed(CallbackInfoReturnable<Float> callback) {
		callback.setReturnValue(24f);
	}

	@Override
	public EnderDragonPhase<? extends DragonPhaseInstance> getPhase() {
		return EnderDragonPhase.CHARGING_PLAYER;
	}
}