package frootloops.versus.mixin.environment;

import frootloops.versus.mod.environment.CustomSpecialEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartBehavior;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(AbstractMinecart.class)
public abstract class MinecartMixin extends VehicleEntity {

    @Shadow private final MinecartBehavior behavior;

    @Shadow private boolean onRails;

    public MinecartMixin(EntityType<?> entityType, Level world, MinecartBehavior controller) {
        super(entityType, world);
        this.behavior = controller;
    }


    @Overwrite
    public static boolean useExperimentalMovement(Level world) {
        return true;// world.getEnabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS);
    }

    @Overwrite
    public double getMaxSpeed(ServerLevel world) {
        if(world.enabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS)) {
            double gameruleMaxSpeed = this.behavior.getMaxSpeed((ServerLevel) this.level());
            return gameruleMaxSpeed == 8.0 ? gameruleMaxSpeed * 8.0 : gameruleMaxSpeed;
        }
        return 64.0 / (this.isInWater() ? 40.0 : 20.0);
    }

    @Inject(method = "makeStepAlongTrack", at = @At("HEAD"), cancellable = true)
    public void slowdownOnFastTurns(BlockPos pos, RailShape railShape, double remainingMovement, CallbackInfoReturnable<Double> cir) {

        // The goal here is twofold:
        //    1. Immersion, make players feel how fast they're turning, and make them take that into consideration when building
        //    2. No more particle accelerators, which are nauseating, and too easy to build

        boolean isMinecartDiagonal = (int)this.getYRot() % 90 != 0;
        if(isMinecartDiagonal) return;

        boolean isTurning = railShape == RailShape.NORTH_EAST || railShape == RailShape.NORTH_WEST || railShape == RailShape.SOUTH_EAST || railShape == RailShape.SOUTH_WEST;
        if(isTurning) {
            float velocitySlowdownAmount = (float)this.getDeltaMovement().lengthSqr()/4f;

            // Tunring at low speed has no effect:
            if(velocitySlowdownAmount < 0.1f) {
                return;
            }

            // Turning at a medium speed has a moderate effect, and spawns a single spark:
            if(velocitySlowdownAmount < 0.5f) {
                this.setDeltaMovement(this.getDeltaMovement().scale(Math.max(0.6f, 1.0f - velocitySlowdownAmount)));
                if(velocitySlowdownAmount > 0.35f) {
                    this.setHurtDir(-this.getHurtDir());
                    this.setHurtTime(10);
                    this.setDamage(20.0F);
                    if (this.level() instanceof ServerLevel serverWorld) {
                        if(velocitySlowdownAmount > 0.45f) serverWorld.playSound((Player)null, this.getX(), this.getY(), this.getZ(), CustomSpecialEffects.RAIL_TURNING_SOUND, this.getSoundSource(), velocitySlowdownAmount - 0.4f, 0.4f + random.nextFloat() * 0.4f);
                        serverWorld.sendParticles(CustomSpecialEffects.SPARKS_PARTICLE, this.position().x(), this.position().y() + 0.1, this.position().z(), 2, 0.1, 0.05, 0.1, 0.1);
                    }
                }
            }

            // Turning at high speeds! Minecart will slow down and spawn lots of sparks:
            else {
                this.setDeltaMovement(this.getDeltaMovement().scale(Math.max(0.5f, 0.95f - velocitySlowdownAmount)));
                this.setHurtDir(-this.getHurtDir());
                this.setHurtTime(10);
                this.setDamage(30.0F);
                if (this.level() instanceof ServerLevel serverWorld) {
                    serverWorld.playSound((Player)null, this.getX(), this.getY(), this.getZ(), CustomSpecialEffects.RAIL_TURNING_SOUND, this.getSoundSource(), 0.6f, 0.8f + random.nextFloat() * 0.2f);
                    serverWorld.sendParticles(CustomSpecialEffects.SPARKS_PARTICLE, this.position().x(), this.position().y() + 0.1, this.position().z(), 16, 0.1, 0.05, 0.1, 0.2);
                }
            }
        }
    }


    @Overwrite
    public void comeOffTrack(ServerLevel world) {
        double d = getMaxSpeed(world);
        if(d == 8.0) d = 32.0;

        Vec3 velocity = this.getDeltaMovement();
        this.setDeltaMovement(Mth.clamp(velocity.x, -d, d), velocity.y, Mth.clamp(velocity.z, -d, d));

        if (this.onGround()) this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
        this.move(MoverType.SELF, this.getDeltaMovement());

        if (!this.onGround()) {
            velocity = this.getDeltaMovement();
            double airResistance = velocity.horizontalDistanceSqr() / 2.0 - 0.12;
            if(airResistance < 0.03) return;

            double slowdownAmount = Math.max(0.925, 1.0 - airResistance);
            this.setDeltaMovement(velocity.x * slowdownAmount, velocity.y > 0.0 ? velocity.y * slowdownAmount : velocity.y * slowdownAmount/4.0, velocity.z * slowdownAmount);
        }
    }
}
