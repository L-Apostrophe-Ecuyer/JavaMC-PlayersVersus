package frootloops.versus.mixin.client.players.attacking;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.Combat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static frootloops.versus.VersusMod.DEBUG_MODE;
import static net.minecraft.world.phys.HitResult.Type.BLOCK;
import static net.minecraft.world.phys.HitResult.Type.ENTITY;

import com.mojang.blaze3d.platform.WindowEventHandler;

@Environment(EnvType.CLIENT)
@Mixin(value = Minecraft.class, priority = 100000)
public abstract class MinecraftClientMixin extends ReentrantBlockableEventLoop<Runnable> implements WindowEventHandler {
    @Shadow public LocalPlayer player;
    @Shadow private Options options;
    @Shadow public ClientLevel level;
    @Shadow protected int missTime;

    @Shadow @Nullable public HitResult hitResult;
    @Shadow @Nullable public MultiPlayerGameMode gameMode;

    @Shadow protected abstract boolean startAttack();

    private int ticksAttackKeyPressed = 0;
    private Entity prevTargettedEntity = null;

    public MinecraftClientMixin(String string) { super(string); }

    @Inject(method = "continueAttack",at = @At("HEAD"), cancellable = true)
    private void holdToAttack(boolean bl, CallbackInfo ci) {
        if(VersusSettings.Combat.CAN_HOLD_TO_ATTACK == false) return;

        boolean tryAttacking = false;
        float attackChargeProgress = player.getAttackStrengthScale(0.0f);
        if(attackChargeProgress > Combat.MIN_COOLDOWN_TO_SWING) {
            if (options.keyAttack.isDown()) {
                ticksAttackKeyPressed++;

                // If the cooldown is complete, swing:
                if (ticksAttackKeyPressed > 1 && ticksAttackKeyPressed >= Combat.getTicksPerAttackOf(player))
                    tryAttacking = true;

                // Otherwise, if at some point we can attack something, we do:
                else if (attackChargeProgress > 0.85d) {
                    double attackRange = Combat.getAttackRange(player, attackChargeProgress);
                    tryAttacking = (attackRange * attackRange) > player.distanceToSqr(hitResult.getLocation());
                }
            }
            else ticksAttackKeyPressed = 0;
        }

        if(tryAttacking) {
            // If the player is breaking a block, return;
            if (hitResult != null && hitResult.getType() == BLOCK) {
                BlockPos pos = ((BlockHitResult)hitResult).getBlockPos();
                if(isMineableBlock(pos, level.getBlockState(pos))) return;
            }
            ticksAttackKeyPressed = 0;
            this.startAttack();
            this.player.resetAttackStrengthTicker();
            ci.cancel();
        }
    }

    private boolean isMineableBlock(BlockPos pos, BlockState block) {
        return !block.getCollisionShape(level, pos).isEmpty() || block.getDestroySpeed(level, pos) != 0.0F;
    }


    @Inject(method = "startAttack",at = @At("HEAD"), cancellable = true)
    private void doAttackOverhaul(CallbackInfoReturnable<Boolean> cir) {
        long timeStart;
        if(DEBUG_MODE) {
            timeStart = System.nanoTime();
            VersusMod.MOD_LOGGER.warn("");
            VersusMod.MOD_LOGGER.warn("PLAYERS - ATTACKING - MinecraftClientMixin - Started Clientside (doAttack)");
        }

        this.ticksAttackKeyPressed = 0;
        float attackProgress =  player.getAttackStrengthScale(0.0f);
        double attackRange = Combat.getAttackRange(player, attackProgress);
        boolean canAttackEntities = attackProgress > Combat.MIN_COOLDOWN_TO_SWING;
        boolean canSoonAttackEntities = !canAttackEntities && attackProgress > 0.2f;

        if(VersusSettings.Combat.CAN_AIM_ASSIST && (canAttackEntities || canSoonAttackEntities)) { // Enables some help & coyote time
            if(attackProgress == 1.0f) prevTargettedEntity = null;
            else if(this.hitResult.getType() != ENTITY) attemptToAimAssistTarget(prevTargettedEntity, attackRange);
            else if(this.hitResult.getType() != ENTITY) attemptToAimAssistTarget(player.getLastHurtByMob(), attackRange);
        }

        if(DEBUG_MODE && System.nanoTime() - timeStart > 200000) VersusMod.MOD_LOGGER.warn("PLAYERS - ATTACKING - Aim assist was incredibly slow: " + ((double)(System.nanoTime() - timeStart)/1000000.0) + " ms");

        boolean breakingBlock = false;
        boolean missedSwing = true;
        switch (this.hitResult.getType()) {
            case ENTITY: {
                if(canAttackEntities && (attackRange * attackRange) > player.distanceToSqr(hitResult.getLocation())) {
                    missedSwing = false;
                    prevTargettedEntity = ((EntityHitResult) this.hitResult).getEntity();
                    gameMode.attack(this.player, prevTargettedEntity);
                }
                else ticksAttackKeyPressed++;
                if(DEBUG_MODE && System.nanoTime() - timeStart > 200000) VersusMod.MOD_LOGGER.warn("PLAYERS - ATTACKING - Attacking an entity took: " + ((double)(System.nanoTime() - timeStart)/1000000.0) + " ms");
                break;
            }
            case BLOCK: {
                BlockHitResult blockHitResult = (BlockHitResult)this.hitResult;
                BlockPos pos = blockHitResult.getBlockPos();
                gameMode.startDestroyBlock(pos, blockHitResult.getDirection());
                if(isMineableBlock(pos, this.level.getBlockState(pos))) {
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
            if (!gameMode.hasMissTime()) this.missTime = 1;
            else this.missTime = 5;
            this.player.resetAttackStrengthTicker();
        }

        if(DEBUG_MODE) {
            VersusMod.MOD_LOGGER.warn("PLAYERS - ATTACKING - MinecraftClientMixin - Ended, lasted " + ((double)(System.nanoTime() - timeStart)/1000000.0) + " ms");
            if(System.nanoTime() - timeStart > 1000000) VersusMod.MOD_LOGGER.warn("  *************** WARNING: EXCESSIVELY SLOW FUNCTION  *************** \n");
        }

        if(canAttackEntities || breakingBlock) this.player.swing(InteractionHand.MAIN_HAND);
        cir.setReturnValue(breakingBlock);
        cir.cancel();
    }

    private void attemptToAimAssistTarget(Entity entity, double range) {
        if(entity == null || !(entity instanceof Monster)) return;
        if(entity.distanceToSqr(player) > range * range) return;
        if(!Combat.isLookingTowards(player, entity.getEyePosition(),-0.9)) return;
        if(!player.hasLineOfSight(entity)) return;
        this.hitResult = new EntityHitResult(entity);
    }
}
