package frootloops.versus.mixin.players.attacking;

//import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import frootloops.versus.util.Combat;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.RaycastContext;
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

    private boolean queuedAttack = false;
    private int ticksPressed = 0;

    public MinecraftClientMixin(String string) { super(string); }

    @Inject(method = "handleBlockBreaking",at = @At("HEAD"), cancellable = true)
    private void holdToAttack(boolean bl, CallbackInfo ci) {
        boolean tryAttacking = false;
        if(Combat.getAttackChargeProgress(player) > 0.5d) {
            if (options.attackKey.wasPressed()) {
                tryAttacking = true;
            } else if (options.attackKey.isPressed()) {
                ticksPressed++;

                // Otherwise, try seeing if you can hit something:
                if ((ticksPressed >= Combat.getTicksPerAttackOf(player) - 3)) {

                    // If the cooldown is complete, swing:
                    if (ticksPressed >= Combat.getTicksPerAttackOf(player))
                        tryAttacking = true;

                        // For 4 ticks prior, if at some point we can attack something, we do:
                    else if (this.crosshairTarget.getType() == ENTITY) {
                        double attackRange = Combat.getAttackRange(player);
                        if ((attackRange * attackRange) > player.squaredDistanceTo(crosshairTarget.getPos()))
                            tryAttacking = true;
                    }
                }
            }
        }
        else {
            ticksPressed = 0;
        }

        if(tryAttacking) {
            // If the player is breaking a block, return;
            if (crosshairTarget != null && crosshairTarget.getType() == BLOCK) {
                BlockPos pos = ((BlockHitResult)crosshairTarget).getBlockPos();
                if(isMineableBlock(pos, world.getBlockState(pos))) return;
            }
            else {
                ticksPressed = 0;
                this.doAttack();
                this.player.resetLastAttackedTicks();
                ci.cancel();
            }
        }
    }

    private boolean isMineableBlock(BlockPos pos, BlockState block) {
        return !block.getCollisionShape(world, pos).isEmpty() || block.getHardness(world, pos) != 0.0F;
    }


    @Inject(method = "doAttack",at = @At("HEAD"), cancellable = true)
    private void doAttackOverhaul(CallbackInfoReturnable<Boolean> cir) {

        double attackProgress =  Combat.getAttackChargeProgress(player);
        double attackRange = Combat.getAttackRange(player, attackProgress);
        boolean canAttackEntities = attackProgress > 0.1;
        boolean isCharged = attackProgress > 0.95;

        if(isCharged && !player.isSneaking() && EnchantmentHelper.getEquipmentLevel(Enchantments.SWEEPING, player) > 0)
            doSweepAttack(attackRange);

        boolean resetAttackCooldown = false, missedSwing = true;
        switch (this.crosshairTarget.getType()) {
            case ENTITY: {
                if(canAttackEntities && (attackRange * attackRange) > player.squaredDistanceTo(crosshairTarget.getPos())) {
                    missedSwing = false;
                    interactionManager.attackEntity(this.player, ((EntityHitResult) this.crosshairTarget).getEntity());
                }
                else ticksPressed++;
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
                else ticksPressed++;
                break;
            }
        }

        if(missedSwing) {
            if (!interactionManager.hasLimitedAttackSpeed()) this.attackCooldown = 1;
            else this.attackCooldown = 4;
            this.player.resetLastAttackedTicks();
        }

        this.player.swingHand(Hand.MAIN_HAND);
        cir.setReturnValue(resetAttackCooldown);
        cir.cancel();
    }

    private void doSweepAttack(double attackRange){
        Vec3d cameraPos = player.getCameraPosVec(1.0F);
        Vec3d rotation = player.getRotationVec(1.0F);

        Vec3d rotatedEightDegZ = new Vec3d(rotation.x * 0.99026806874 - rotation.z * 0.13917310096, rotation.y, rotation.x * 0.13917310096 + rotation.z * 0.99026806874);
        Vec3d rotatedEightDegX = new Vec3d(rotation.x * 0.99026806874 - rotation.z * -0.13917310096, rotation.y, rotation.x * -0.13917310096 + rotation.z * 0.99026806874);

        swingAtEntities(rotatedEightDegZ, cameraPos, attackRange);
        swingAtEntities(rotatedEightDegX, cameraPos, attackRange);

        // Break all foliage within range of the crosshair target, - 1 block:
        Vec3d crosshairPos = this.crosshairTarget.getPos().subtract(rotation);
        BlockPos pos = new BlockPos(new Vec3i((int)crosshairPos.x, (int)crosshairPos.y, (int)crosshairPos.z));
        swungAtBlockPos(pos);
    }

    private void swingAtEntities(final Vec3d rotation, Vec3d cameraPos, double range){
        Vec3d end = cameraPos.add(rotation.x * range, rotation.y * range, rotation.z * range);
        Predicate<Entity> predicate = EntityPredicates.CAN_COLLIDE.and(e -> e != null);
        EntityHitResult entityResult = ProjectileUtil.getEntityCollision(world, player, cameraPos, end, new Box(cameraPos, end), predicate);

        if (entityResult != null) {
            this.interactionManager.attackEntity(player, entityResult.getEntity());
            Vec3d entityPos = entityResult.getPos().subtract(rotation);
            BlockPos pos = new BlockPos(new Vec3i((int)entityPos.x, (int)entityPos.y, (int)entityPos.z));
            swungAtBlockPos(pos);
        }
    }

    private void swungAtBlockPos(BlockPos pos) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        breakFoliageAt(new BlockPos(x + 1, y, z + 1));
        breakFoliageAt(new BlockPos(x + 1, y, z - 1));
        breakFoliageAt(new BlockPos(x - 1, y, z + 1));
        breakFoliageAt(new BlockPos(x - 1, y, z - 1));
        breakFoliageAt(new BlockPos(x + 1, y, z));
        breakFoliageAt(new BlockPos(x - 1, y, z));
        breakFoliageAt(new BlockPos(x, y, z + 1));
        breakFoliageAt(new BlockPos(x, y, z - 1));
    }

    private void breakFoliageAt(BlockPos pos) {
        if(this.world.getBlockState(pos).getHardness(world, pos) == 0.0F) {
            this.interactionManager.breakBlock(pos);
            BlockPos above = new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
            if(this.world.getBlockState(above).getHardness(world, above) == 0.0F) this.interactionManager.breakBlock(above);
        }
    }
}
