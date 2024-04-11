/*
 * Decompiled with CFR 0.2.0 (FabricMC d28b102d).
 */
package frootloops.versus.backported.entities.wind_charge;

import frootloops.versus.VersusMod;
import frootloops.versus.backported.items.FutureItems;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FlyingItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ExplosiveProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;

import static net.minecraft.state.property.Properties.POWERED;

public class WindChargeEntity
extends ExplosiveProjectileEntity
implements FlyingItemEntity {
    public static final WindChargeExplosionBehavior EXPLOSION_BEHAVIOR = new WindChargeExplosionBehavior();
    private boolean hasExploded = false;
    public WindChargeEntity(EntityType<? extends WindChargeEntity> entityType, World world) {
        super((EntityType<? extends ExplosiveProjectileEntity>)entityType, world);
        this.hasExploded = false;
    }

    @Override
    protected Box calculateBoundingBox() {
        return new Box(this.getPos().x - 0.075, this.getPos().y - 0.15, this.getPos().z - 0.075, this.getPos().x + 0.075, this.getPos().y, this.getPos().z + 0.075);
    }

    @Override
    public boolean collidesWith(Entity other) {
        if (other instanceof WindChargeEntity) {
            return false;
        }
        return super.collidesWith(other);
    }

    @Override
    protected boolean canHit(Entity entity) {
        if (entity instanceof WindChargeEntity) {
            return false;
        }
        return super.canHit(entity);
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        this.createExplosion();
        if(entityHitResult.getEntity() == this.getOwner() && this.getOwner() != null) return;
        super.onEntityHit(entityHitResult);
        if(!this.getWorld().isClient) {
            Entity entity;
            entityHitResult.getEntity().damage(this.getDamageSources().mobProjectile(this, (entity = this.getOwner()) instanceof LivingEntity ? ((LivingEntity)entity) : null), 2.0f);
            entityHitResult.getEntity().setVelocity(entityHitResult.getEntity().getVelocity().add(this.getVelocity().x, 0.1, this.getVelocity().z));
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        this.createExplosion();
        super.onBlockHit(blockHitResult);
        this.discard();
    }

    private void createExplosion() {

        if(hasExploded) return;
        double x, y, z;
        x = this.getX();
        y = this.getY();
        z = this.getZ();
        WindChargeExplosion explosion = new WindChargeExplosion(this.getWorld(), this, x, y, z, 3.0f);
        explosion.collectBlocksAndDamageEntities();
        explosion.affectWorld(true);

        if(this.getWorld() instanceof ServerWorld serverWorld) {
            for (ServerPlayerEntity serverPlayerEntity : serverWorld.getPlayers()) {
                if (!(serverPlayerEntity.squaredDistanceTo(x, y, z) < 4096.0)) continue;
                serverPlayerEntity.networkHandler.sendPacket(new ExplosionS2CPacket(x, y, z, 4.0f, explosion.getAffectedBlocks(), explosion.getAffectedPlayers().get(serverPlayerEntity)));
            }
        }

        VersusMod.MOD_LOGGER.warn("Is player affected? " + explosion.getAffectedPlayers().containsKey((PlayerEntity) this.getOwner()));
        if(this.getOwner() != null && explosion.getAffectedPlayers().containsKey(this.getOwner())) {
            //FutureItems.LAST_WIND_CHARGE_USE_TIME.put(this.getOwner().getUuid(), this.getWorld().getTime());
        }
        FutureItems.LAST_WIND_CHARGE_USE_TIME.put(this.getOwner().getUuid(), this.getWorld().getTime());
        hasExploded = true;
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        this.createExplosion();
        if (!this.getWorld().isClient) {
            this.discard();
        }
    }

    @Override
    protected boolean isBurning() {
        return false;
    }

    @Override
    public ItemStack getStack() {
        return ItemStack.EMPTY;
    }

    @Override
    protected float getDrag() {
        return 1.0f;
    }

    protected float getDragInWater() {
        return this.getDrag();
    }

    @Override
    protected ParticleEffect getParticleType() {
        return ParticleTypes.CLOUD;
    }

    protected RaycastContext.ShapeType getRaycastShapeType() {
        return RaycastContext.ShapeType.OUTLINE;
    }

    public static final class WindChargeExplosionBehavior
    extends ExplosionBehavior {
        @Override
        public boolean canDestroyBlock(Explosion explosion, BlockView world, BlockPos pos, BlockState state, float power) {
            if(state.emitsRedstonePower()) state.cycle(POWERED);
            return false;
        }
    }
}

