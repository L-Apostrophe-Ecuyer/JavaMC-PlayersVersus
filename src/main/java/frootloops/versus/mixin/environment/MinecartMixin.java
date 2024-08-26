package frootloops.versus.mixin.environment;

import frootloops.versus.mod.environment.CustomSpecialEffects;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.ExperimentalMinecartController;
import net.minecraft.entity.vehicle.MinecartController;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(AbstractMinecartEntity.class)
public abstract class MinecartMixin extends VehicleEntity {

    @Shadow private final MinecartController controller;

    @Shadow private boolean onRail;

    public MinecartMixin(EntityType<?> entityType, World world, MinecartController controller) {
        super(entityType, world);
        this.controller = controller;
    }


    @Overwrite
    public static boolean areMinecartImprovementsEnabled(World world) {
        return true;// world.getEnabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS);
    }

    @Overwrite
    public double getMaxSpeed() {
        if(this.getWorld().getEnabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS)) {
            double gameruleMaxSpeed = this.controller.getMaxSpeed();
            return gameruleMaxSpeed == 8.0 ? gameruleMaxSpeed * 8.0 : gameruleMaxSpeed;
        }
        return 64.0 / (this.isTouchingWater() ? 40.0 : 20.0);
    }

    @Inject(method = "method_61564", at = @At("HEAD"), cancellable = true)
    public void slowdownOnFastTurns(BlockPos blockPos, RailShape railShape, double d, CallbackInfoReturnable<Double> cir) {

        // The goal here is twofold:
        //    1. Immersion, make players feel how fast they're turning, and make them take that into consideration when building
        //    2. No more particle accelerators, which are nauseating, and too easy to build

        boolean isMinecartDiagonal = (int)this.getYaw() % 90 != 0;
        if(isMinecartDiagonal) return;

        boolean isTurning = railShape == RailShape.NORTH_EAST || railShape == RailShape.NORTH_WEST || railShape == RailShape.SOUTH_EAST || railShape == RailShape.SOUTH_WEST;
        if(isTurning) {
            float velocitySlowdownAmount = (float)this.getVelocity().lengthSquared()/4f;

            // Tunring at low speed has no effect:
            if(velocitySlowdownAmount < 0.1f) {
                return;
            }

            // Turning at a medium speed has a moderate effect, and spawns a single spark:
            if(velocitySlowdownAmount < 0.5f) {
                this.setVelocity(this.getVelocity().multiply(Math.max(0.6f, 1.0f - velocitySlowdownAmount)));
                if(velocitySlowdownAmount > 0.35f) {
                    this.setDamageWobbleSide(-this.getDamageWobbleSide());
                    this.setDamageWobbleTicks(10);
                    this.setDamageWobbleStrength(20.0F);
                    if (this.getWorld() instanceof ServerWorld serverWorld) {
                        if(velocitySlowdownAmount > 0.45f) serverWorld.playSound((PlayerEntity)null, this.getX(), this.getY(), this.getZ(), CustomSpecialEffects.RAIL_TURNING_SOUND, this.getSoundCategory(), velocitySlowdownAmount - 0.4f, 0.4f + random.nextFloat() * 0.4f);
                        serverWorld.spawnParticles(CustomSpecialEffects.SPARKS_PARTICLE, this.getPos().getX(), this.getPos().getY() + 0.1, this.getPos().getZ(), 2, 0.1, 0.05, 0.1, 0.1);
                    }
                }
            }

            // Turning at high speeds! Minecart will slow down and spawn lots of sparks:
            else {
                this.setVelocity(this.getVelocity().multiply(Math.max(0.5f, 0.95f - velocitySlowdownAmount)));
                this.setDamageWobbleSide(-this.getDamageWobbleSide());
                this.setDamageWobbleTicks(10);
                this.setDamageWobbleStrength(30.0F);
                if (this.getWorld() instanceof ServerWorld serverWorld) {
                    serverWorld.playSound((PlayerEntity)null, this.getX(), this.getY(), this.getZ(), CustomSpecialEffects.RAIL_TURNING_SOUND, this.getSoundCategory(), 0.6f, 0.8f + random.nextFloat() * 0.2f);
                    serverWorld.spawnParticles(CustomSpecialEffects.SPARKS_PARTICLE, this.getPos().getX(), this.getPos().getY() + 0.1, this.getPos().getZ(), 16, 0.1, 0.05, 0.1, 0.2);
                }
            }
        }
    }


    @Overwrite
    public void moveOffRail() {
        double d = getMaxSpeed();
        if(d == 8.0) d = 32.0;

        Vec3d velocity = this.getVelocity();
        this.setVelocity(MathHelper.clamp(velocity.x, -d, d), velocity.y, MathHelper.clamp(velocity.z, -d, d));

        if (this.isOnGround()) this.setVelocity(this.getVelocity().multiply(0.5));
        this.move(MovementType.SELF, this.getVelocity());

        if (!this.isOnGround()) {
            velocity = this.getVelocity();
            double airResistance = velocity.horizontalLengthSquared() / 2.0 - 0.12;
            if(airResistance < 0.03) return;

            double slowdownAmount = Math.max(0.925, 1.0 - airResistance);
            this.setVelocity(velocity.x * slowdownAmount, velocity.y > 0.0 ? velocity.y * slowdownAmount : velocity.y * slowdownAmount/4.0, velocity.z * slowdownAmount);
        }
    }
}
