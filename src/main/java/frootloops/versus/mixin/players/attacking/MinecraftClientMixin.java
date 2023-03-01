package frootloops.versus.mixin.players.attacking;

//import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import frootloops.versus.mixin.players.accessors.LivingEntityAccessor;
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
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
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

@Mixin(value = MinecraftClient.class, priority = 100000)
public abstract class MinecraftClientMixin extends ReentrantThreadExecutor<Runnable> implements WindowEventHandler {
    @Shadow public ClientPlayerEntity player;
    @Shadow private GameOptions options;
    @Shadow public ClientWorld world;
    @Shadow protected int attackCooldown;
    @Shadow @Nullable public HitResult crosshairTarget;
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;

    @Shadow protected abstract boolean doAttack();

    public MinecraftClientMixin(String string) { super(string); }

    private static final boolean SHOW_DEBUG_MESSAGES = false;

    @Inject(method = "handleBlockBreaking",at = @At("HEAD"), cancellable = true)
    private void holdToAttack(boolean bl, CallbackInfo ci) {
        boolean isPressed = options.attackKey.isPressed();
        if(isPressed) {

            // If the player is breaking a block, return;
            if (crosshairTarget != null && crosshairTarget.getType() == BLOCK) {
                BlockPos pos = ((BlockHitResult)crosshairTarget).getBlockPos();
                if(isMineableBlock(pos, world.getBlockState(pos)))
                    return;
            }

            // Otherwise, try swinging:
            double playerAttackSpeed = player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);
            double attackProgress =  ((double)((LivingEntityAccessor)player).getLastAttackedTicks()) * playerAttackSpeed / 20.0;

            if(attackProgress > 1.05) {
                this.doAttack();
                this.player.resetLastAttackedTicks();
            }
            ci.cancel();
        }
    }

    private boolean isMineableBlock(BlockPos pos, BlockState block) {
        return !block.getCollisionShape(world, pos).isEmpty() || block.getHardness(world, pos) != 0.0F;
    }


    @Inject(method = "doAttack",at = @At("HEAD"), cancellable = true)
    private void doAttackOverhaul(CallbackInfoReturnable<Boolean> cir) {

        double attackProgress =  ((double)((LivingEntityAccessor)player).getLastAttackedTicks()) * player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED) / 20.0;
        boolean canAttackEntities = attackProgress > 0.66;
        boolean isCharged = attackProgress > 0.95;

        if(isCharged && !player.isSneaking() && EnchantmentHelper.getEquipmentLevel(Enchantments.SWEEPING, player) > 0)
            doSweepAttack();

        boolean resetAttackCooldown = false;
        switch (this.crosshairTarget.getType()) {
            case ENTITY: {

                if(canAttackEntities && !(this.attackCooldown > 0 || this.player.isRiding())) {
                    this.interactionManager.attackEntity(this.player, ((EntityHitResult) this.crosshairTarget).getEntity());
                    break;
                }
                else {
                    cir.setReturnValue(false);
                    return;
                }
            }
            case BLOCK: {

                BlockHitResult blockHitResult = (BlockHitResult)this.crosshairTarget;
                BlockPos pos = blockHitResult.getBlockPos();
                this.interactionManager.attackBlock(pos, blockHitResult.getSide());
                if(isMineableBlock(pos, this.world.getBlockState(pos))) {
                    resetAttackCooldown = true;
                    break;
                }
                // Tweaked code from Rongmario's Clean Cut:
                // Allow attacks through weaker blocks to count as an attack, i.e. swinging through foliage
                else if(canAttackEntities) {

                    Vec3d camera = player.getCameraPosVec(1.0F);
                    Vec3d rotation = player.getRotationVec(1.0F);
                    double range = 3.0d; // ReachEntityAttributes.getAttackRange(player, 3.0);
                    Vec3d end = camera.add(rotation.x * range, rotation.y * range, rotation.z * range);
                    Predicate<Entity> predicate = EntityPredicates.CAN_COLLIDE.and(e -> e != null);

                    EntityHitResult result = ProjectileUtil.getEntityCollision(world, player, camera, end, new Box(camera, end), predicate);
                    if (result != null) this.interactionManager.attackEntity(player, result.getEntity());
                }
                break;
            }
            case MISS: {
                if (!this.interactionManager.hasLimitedAttackSpeed()) this.attackCooldown = 1;
                else this.attackCooldown = 4;
                this.player.resetLastAttackedTicks();
            }
        }
        this.player.swingHand(Hand.MAIN_HAND);
        cir.setReturnValue(resetAttackCooldown);
    }

    private void doSweepAttack(){
        Vec3d cameraPos = player.getCameraPosVec(1.0F);
        Vec3d rotation = player.getRotationVec(1.0F);
        double range = 3.0d;// ReachEntityAttributes.getAttackRange(player, 3.0);

        Vec3d rotatedEightDegZ = new Vec3d(rotation.x * 0.99026806874 - rotation.z * 0.13917310096, rotation.y, rotation.x * 0.13917310096 + rotation.z * 0.99026806874);
        Vec3d rotatedEightDegX = new Vec3d(rotation.x * 0.99026806874 - rotation.z * -0.13917310096, rotation.y, rotation.x * -0.13917310096 + rotation.z * 0.99026806874);

        swingAtEntities(rotatedEightDegZ, cameraPos, range);
        swingAtEntities(rotatedEightDegX, cameraPos, range);

        // Break all foliage within range of the crosshair target, - 1 block:
        Vec3d crosshairPos = this.crosshairTarget.getPos().subtract(rotation);
        BlockPos pos = new BlockPos(new Vec3i((int)crosshairPos.x, (int)crosshairPos.y, (int)crosshairPos.z));
        swungAtBlockPos(pos);
    }

    private final void swingAtEntities(final Vec3d rotation, Vec3d cameraPos, double range){
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

    private final void swungAtBlockPos(BlockPos pos) {
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

    private final void breakFoliageAt(BlockPos pos) {
        if(this.world.getBlockState(pos).getHardness(world, pos) == 0.0F) {
            this.interactionManager.breakBlock(pos);
            BlockPos above = new BlockPos(pos.getX(), pos.getY() + 1, pos.getZ());
            if(this.world.getBlockState(above).getHardness(world, above) == 0.0F) this.interactionManager.breakBlock(above);
        }
    }
}
