package frootloops.versus.mixin.mobs.hostile.end.dragon;

import frootloops.versus.mod.mobs.hostile.end.DragonManager;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.phase.AbstractPhase;
import net.minecraft.entity.boss.dragon.phase.HoldingPatternPhase;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HoldingPatternPhase.class)
public abstract class HoldingPatternPhaseMixin extends AbstractPhase {
	@Shadow
	protected Path path;

	public HoldingPatternPhaseMixin(EnderDragonEntity dragon) {
		super(dragon);
	}

	@Inject(at = @At("HEAD"), method = "tickInRange", cancellable = true)
	private void findNewTarget(ServerWorld world, CallbackInfo callback) {
		if (this.path == null || !this.path.isFinished())
			return;

		if (DragonManager.onPhaseEnd(this.dragon))
			callback.cancel();
	}
}