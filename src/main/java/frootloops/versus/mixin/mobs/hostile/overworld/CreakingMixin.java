package frootloops.versus.mixin.mobs.hostile.overworld;

import frootloops.versus.mod.mobs.hostile.overworld.CreakingScares;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Creakings that teleport when stuck and nobody's looking, and that answer a hit with blindness or a silverfish ({@link CreakingScares}). */
@Mixin(Creaking.class)
public abstract class CreakingMixin extends Monster {

    @Unique
    private Vec3 playersVersus$stuckAt = Vec3.ZERO;
    @Unique
    private int playersVersus$stuckTicks;
    @Unique
    private int playersVersus$teleportCooldown;

    protected CreakingMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Shadow
    public abstract boolean canMove();

    /**
     * Stuck: free to move (no one looking), going somewhere, yet within a block of where it was three seconds ago, or
     * the navigation gave up. Then it teleports out of sight, if it can find a place.
     */
    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void playersVersus$teleportWhenStuck(ServerLevel level, CallbackInfo info) {
        if (this.playersVersus$teleportCooldown > 0) this.playersVersus$teleportCooldown--;
        LivingEntity target = this.getTarget();
        boolean going = target != null ? !this.isWithinMeleeAttackRange(target) : this.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET);
        if (!this.canMove() || !going || !this.position().closerThan(this.playersVersus$stuckAt, 1.0)) {
            this.playersVersus$stuckAt = this.position();
            this.playersVersus$stuckTicks = 0;
            return;
        }
        if (++this.playersVersus$stuckTicks < CreakingScares.STUCK_TICKS && !this.getNavigation().isStuck()) return;
        this.playersVersus$stuckTicks = 0;
        if (this.playersVersus$teleportCooldown == 0 && CreakingScares.teleportOutOfSight(level, (Creaking) (Object) this)) {
            this.playersVersus$teleportCooldown = CreakingScares.TELEPORT_COOLDOWN;
            this.playersVersus$stuckAt = this.position();
        }
    }

    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void playersVersus$hitIsAGamble(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        if (info.getReturnValue() && !this.isRemoved() && !this.isDeadOrDying() && source.getEntity() instanceof LivingEntity attacker) {
            CreakingScares.onHit(level, (Creaking) (Object) this, attacker);
        }
    }
}
