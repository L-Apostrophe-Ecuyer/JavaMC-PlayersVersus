package frootloops.versus.mixin.players.attacking;

//import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
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
    @Shadow @Nullable public HitResult crosshairTarget;
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;

    @Shadow protected abstract boolean doAttack();

    private int ticksAttackKeyPressed = 0;

    public MinecraftClientMixin(String string) { super(string); }

    @Inject(method = "handleBlockBreaking",at = @At("HEAD"), cancellable = true)
    private void holdToAttack(boolean bl, CallbackInfo ci) {
        boolean tryAttacking = false;
        double attackChargeProgress = Combat.getAttackChargeProgress(player);
        if(attackChargeProgress > 0.75d) {
            if (options.attackKey.isPressed()) {
                ticksAttackKeyPressed++;

                // If the cooldown is complete, swing:
                if (ticksAttackKeyPressed >= Combat.getTicksPerAttackOf(player))
                    tryAttacking = true;

                // Otherwise, if at some point we can attack something, we do:
                else if (this.crosshairTarget.getType() == ENTITY && attackChargeProgress > 0.85d) {
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
        boolean canAttackEntities = attackProgress > 0.5;

        boolean resetAttackCooldown = false;
        boolean missedSwing = true;
        switch (this.crosshairTarget.getType()) {
            case ENTITY: {
                if(canAttackEntities && (attackRange * attackRange) > player.squaredDistanceTo(crosshairTarget.getPos())) {
                    missedSwing = false;
                    interactionManager.attackEntity(this.player, ((EntityHitResult) this.crosshairTarget).getEntity());
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
                    resetAttackCooldown = true;
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
        if(missedSwing) {
            if (!interactionManager.hasLimitedAttackSpeed()) this.attackCooldown = 1;
            else this.attackCooldown = 5;
            this.player.resetLastAttackedTicks();
        }

        this.player.swingHand(Hand.MAIN_HAND);
        cir.setReturnValue(resetAttackCooldown);
        cir.cancel();
    }

    private void breakFoliageAt(BlockPos pos) {
        if(this.world.getBlockState(pos).getHardness(world, pos) == 0.0F) {
            this.interactionManager.breakBlock(pos);
            BlockPos above = new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
            if(this.world.getBlockState(above).getHardness(world, above) == 0.0F) this.interactionManager.breakBlock(above);
        }
    }
}
