package frootloops.versus.mixin.client.players.attacking;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.Combat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static frootloops.versus.VersusMod.DEBUG_MODE;
import static net.minecraft.util.hit.HitResult.Type.BLOCK;
import static net.minecraft.util.hit.HitResult.Type.ENTITY;

@Environment(EnvType.CLIENT)
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
        if(VersusSettings.Combat.CAN_HOLD_TO_ATTACK == false) return;

        boolean tryAttacking = false;
        float attackChargeProgress = player.getAttackCooldownProgress(0.0f);
        if(attackChargeProgress > Combat.MIN_COOLDOWN_TO_SWING) {
            if (options.attackKey.isPressed()) {
                ticksAttackKeyPressed++;

                // If the cooldown is complete, swing:
                if (ticksAttackKeyPressed > 1 && ticksAttackKeyPressed >= Combat.getTicksPerAttackOf(player))
                    tryAttacking = true;

                // Otherwise, if at some point we can attack something, we do:
                else if (attackChargeProgress > 0.85d) {
                    double attackRange = Combat.getAttackRange(player, attackChargeProgress);
                    tryAttacking = (attackRange * attackRange) > player.squaredDistanceTo(crosshairTarget.getEntityPos());
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
        long timeStart;
        if(DEBUG_MODE) {
            timeStart = System.nanoTime();
            VersusMod.MOD_LOGGER.warn("");
            VersusMod.MOD_LOGGER.warn("PLAYERS - ATTACKING - MinecraftClientMixin - Started Clientside (doAttack)");
        }

        this.ticksAttackKeyPressed = 0;
        float attackProgress =  player.getAttackCooldownProgress(0.0f);
        double attackRange = Combat.getAttackRange(player, attackProgress);
        boolean canAttackEntities = attackProgress > Combat.MIN_COOLDOWN_TO_SWING;
        boolean canSoonAttackEntities = !canAttackEntities && attackProgress > 0.2f;

        if(VersusSettings.Combat.CAN_AIM_ASSIST && (canAttackEntities || canSoonAttackEntities)) { // Enables some help & coyote time
            if(attackProgress == 1.0f) prevTargettedEntity = null;
            else if(this.crosshairTarget.getType() != ENTITY) attemptToAimAssistTarget(prevTargettedEntity, attackRange);
            else if(this.crosshairTarget.getType() != ENTITY) attemptToAimAssistTarget(player.getAttacker(), attackRange);
        }

        if(DEBUG_MODE && System.nanoTime() - timeStart > 200000) VersusMod.MOD_LOGGER.warn("PLAYERS - ATTACKING - Aim assist was incredibly slow: " + ((double)(System.nanoTime() - timeStart)/1000000.0) + " ms");

        boolean breakingBlock = false;
        boolean missedSwing = true;
        switch (this.crosshairTarget.getType()) {
            case ENTITY: {
                if(canAttackEntities && (attackRange * attackRange) > player.squaredDistanceTo(crosshairTarget.getEntityPos())) {
                    missedSwing = false;
                    prevTargettedEntity = ((EntityHitResult) this.crosshairTarget).getEntity();
                    interactionManager.attackEntity(this.player, prevTargettedEntity);
                }
                else ticksAttackKeyPressed++;
                if(DEBUG_MODE && System.nanoTime() - timeStart > 200000) VersusMod.MOD_LOGGER.warn("PLAYERS - ATTACKING - Attacking an entity took: " + ((double)(System.nanoTime() - timeStart)/1000000.0) + " ms");
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
                else ticksAttackKeyPressed++;
                if(DEBUG_MODE && System.nanoTime() - timeStart > 200000) VersusMod.MOD_LOGGER.warn("PLAYERS - ATTACKING - Attacking a block took: " + ((double)(System.nanoTime() - timeStart)/1000000.0) + " ms");
                break;
            }
        }
        if(missedSwing && canAttackEntities) {
            if (!interactionManager.hasLimitedAttackSpeed()) this.attackCooldown = 1;
            else this.attackCooldown = 5;
            this.player.resetLastAttackedTicks();
        }

        if(DEBUG_MODE) {
            VersusMod.MOD_LOGGER.warn("PLAYERS - ATTACKING - MinecraftClientMixin - Ended, lasted " + ((double)(System.nanoTime() - timeStart)/1000000.0) + " ms");
            if(System.nanoTime() - timeStart > 1000000) VersusMod.MOD_LOGGER.warn("  *************** WARNING: EXCESSIVELY SLOW FUNCTION  *************** \n");
        }

        if(canAttackEntities || breakingBlock) this.player.swingHand(Hand.MAIN_HAND);
        cir.setReturnValue(breakingBlock);
        cir.cancel();
    }

    private void attemptToAimAssistTarget(Entity entity, double range) {
        if(entity == null || !(entity instanceof HostileEntity)) return;
        if(entity.squaredDistanceTo(player) > range * range) return;
        if(!Combat.isLookingTowards(player, entity.getEyePos(),-0.9)) return;
        if(!player.canSee(entity)) return;
        this.crosshairTarget = new EntityHitResult(entity);
    }
}
