package frootloops.versus.mixin.players.attacking;

//import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import frootloops.versus.ClientSettings;
import frootloops.versus.mod.Combat;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

import static net.minecraft.util.hit.HitResult.Type.BLOCK;
import static net.minecraft.util.hit.HitResult.Type.ENTITY;

@Mixin(value = MinecraftClient.class, priority = 100000)
public abstract class MinecraftClientMixin extends ReentrantThreadExecutor<Runnable> implements WindowEventHandler {
    @Shadow public ClientPlayerEntity player;
    @Shadow private GameOptions options;
    @Shadow public ClientWorld world;
    @Shadow protected int attackCooldown;

    @Shadow @Nullable public Entity cameraEntity;
    @Shadow @Nullable public HitResult crosshairTarget;
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;

    @Shadow protected abstract boolean doAttack();

    private int ticksAttackKeyPressed = 0;
    private Entity prevTargettedEntity = null;

    public MinecraftClientMixin(String string) { super(string); }

    @Inject(method = "handleBlockBreaking",at = @At("HEAD"), cancellable = true)
    private void holdToAttack(boolean bl, CallbackInfo ci) {
        boolean tryAttacking = false;
        double attackChargeProgress = Combat.getAttackChargeProgress(player);
        if(attackChargeProgress > Combat.MIN_COOLDOWN_TO_SWING) {
            if (options.attackKey.isPressed() && ClientSettings.CAN_HOLD_TO_ATTACK) {
                ticksAttackKeyPressed++;

                // If the cooldown is complete, swing:
                if (ticksAttackKeyPressed > 1 && ticksAttackKeyPressed >= Combat.getTicksPerAttackOf(player))
                    tryAttacking = true;

                // Otherwise, if at some point we can attack something, we do:
                else if (attackChargeProgress > 0.85d) {
                    double attackRange = Combat.getAttackRange(player,attackChargeProgress);
                    tryAttacking = (attackRange * attackRange) > player.squaredDistanceTo(crosshairTarget.getPos());
                }
            }
            else ticksAttackKeyPressed = 0;
        }

        if(tryAttacking) {
            // If the player is breaking a block, return;
            if (crosshairTarget != null && crosshairTarget.getType() == BLOCK) {
                BlockPos pos = ((BlockHitResult)crosshairTarget).getBlockPos();
                if(isMineableBlock(pos, world.getBlockState(pos))) return;
            }
            ticksAttackKeyPressed = 0;
            this.doAttack();
            this.player.resetLastAttackedTicks();
            ci.cancel();
        }
    }

    private boolean isMineableBlock(BlockPos pos, BlockState block) {
        return !block.getCollisionShape(world, pos).isEmpty() || block.getHardness(world, pos) != 0.0F;
    }


    @Inject(method = "doAttack",at = @At("HEAD"), cancellable = true)
    private void doAttackOverhaul(CallbackInfoReturnable<Boolean> cir) {
        this.ticksAttackKeyPressed = 0;
        double attackProgress =  Combat.getAttackChargeProgress(player);
        double attackRange = Combat.getAttackRange(player, attackProgress);
        boolean canAttackEntities = attackProgress > Combat.MIN_COOLDOWN_TO_SWING;

        if(ClientSettings.CAN_AIM_ASSIST && canAttackEntities) {
            if(this.crosshairTarget.getType() != ENTITY && attackProgress < 4.0 && prevTargettedEntity != null) attemptToAimAssistTarget(prevTargettedEntity, attackRange);
            if(this.crosshairTarget.getType() != ENTITY && player.getAttacker() != null) attemptToAimAssistTarget(player.getAttacker(), attackRange);
            if(this.crosshairTarget.getType() != ENTITY) prevTargettedEntity = null;
        }

        boolean breakingBlock = false;
        boolean missedSwing = true;
        switch (this.crosshairTarget.getType()) {
            case ENTITY: {
                if(canAttackEntities && (attackRange * attackRange) > player.squaredDistanceTo(crosshairTarget.getPos())) {
                    missedSwing = false;
                    prevTargettedEntity = ((EntityHitResult) this.crosshairTarget).getEntity();
                    interactionManager.attackEntity(this.player, prevTargettedEntity);
                }
                else ticksAttackKeyPressed++;
                break;
            }
            case BLOCK: {
                BlockHitResult blockHitResult = (BlockHitResult)this.crosshairTarget;
                BlockPos pos = blockHitResult.getBlockPos();
                interactionManager.attackBlock(pos, blockHitResult.getSide());
                if(isMineableBlock(pos, this.world.getBlockState(pos))) {
                    missedSwing = false;
                    breakingBlock = true;
                    break;
                }
                // Tweaked code from Rongmario's Clean Cut:
                // Allow attacks through weaker blocks to count as an attack, i.e. swinging through foliage
                else if(canAttackEntities) {
                    Vec3d camera = player.getCameraPosVec(1.0F);
                    Vec3d rotation = player.getRotationVec(1.0F);
                    Vec3d end = camera.add(rotation.x * attackRange, rotation.y * attackRange, rotation.z * attackRange);
                    Predicate<Entity> predicate = EntityPredicates.CAN_COLLIDE.and(e -> e != null);

                    EntityHitResult result = ProjectileUtil.getEntityCollision(world, player, camera, end, new Box(camera, end), predicate);
                    if (result != null) {
                        interactionManager.attackEntity(player, result.getEntity());
                        missedSwing = false;
                    }
                }
                else ticksAttackKeyPressed++;
                break;
            }
        }
        if(missedSwing && canAttackEntities) {
            if (!interactionManager.hasLimitedAttackSpeed()) this.attackCooldown = 1;
            else this.attackCooldown = 5;
            this.player.resetLastAttackedTicks();
        }

        if(canAttackEntities || breakingBlock) this.player.swingHand(Hand.MAIN_HAND);
        cir.setReturnValue(breakingBlock);
        cir.cancel();
    }

    private void attemptToAimAssistTarget(Entity entity, double range) {
        if(entity == null) return;
        if(entity.squaredDistanceTo(player) > range * range) return;
        if(!Combat.isLookingTowards(player, entity.getEyePos(),0.85)) return;
        if(!player.canSee(entity)) return;
        this.crosshairTarget = new EntityHitResult(entity);
    }
}
