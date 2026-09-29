package frootloops.versus.mixin.mobs.hostile.end.dragon;

import frootloops.versus.mod.mobs.hostile.end.DragonManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonHoldingPatternPhase;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DragonHoldingPatternPhase.class)
public abstract class HoldingPatternPhaseMixin extends AbstractDragonPhaseInstance {
	@Shadow
	protected Path currentPath;

	public HoldingPatternPhaseMixin(EnderDragon dragon) {
		super(dragon);
	}

	@Inject(at = @At("HEAD"), method = "findNewTarget", cancellable = true)
	private void findNewTarget(ServerLevel world, CallbackInfo callback) {
		if (this.currentPath == null || !this.currentPath.isDone())
			return;

		if (DragonManager.onPhaseEnd(this.dragon))
			callback.cancel();
	}
}