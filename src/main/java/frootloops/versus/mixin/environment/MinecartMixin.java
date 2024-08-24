package frootloops.versus.mixin.environment;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.CustomParticles;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.RailBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(AbstractMinecartEntity.class)
public abstract class MinecartMixin extends VehicleEntity {

    public MinecartMixin(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    @Overwrite
    public static boolean areMinecartImprovementsEnabled(World world) {
        return true;// world.getEnabledFeatures().contains(FeatureFlags.MINECART_IMPROVEMENTS);
    }

    @Override
    public float getVelocityMultiplier() {
        BlockState blockState = this.getWorld().getBlockState(this.getBlockPos());
        if(blockState.getBlock() instanceof AbstractRailBlock railBlock) {
            if(!(railBlock instanceof RailBlock)) return 1.0f;
            RailShape railShape = blockState.get(railBlock.getShapeProperty());
            boolean isTurning = railShape == RailShape.NORTH_EAST || railShape == RailShape.NORTH_WEST || railShape == RailShape.SOUTH_EAST || railShape == RailShape.SOUTH_WEST;
            if(isTurning) {
                int yaw = (int)this.getYaw();
                if(yaw % 90 != 0) return 0.95f; // Less slow down if already facing the right direction

                float velocitySlowdownAmount = Math.min(0.7f, (float)this.getVelocity().lengthSquared()/2f);
                if(velocitySlowdownAmount >= 0.7f) {
                    if(!this.getWorld().isClient()) {
                        //this.playSound(SoundEvents.ENTITY_MINECART_RIDING, 0.01f + (velocitySlowdownAmount - 1F)/8f, 1.8f);
                        ((ServerWorld)this.getWorld()).spawnParticles(CustomParticles.SPARKS, this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(), 3, 0.05, 0.02, 0.05, 0.1);
                    }
                    this.setVelocity(this.getVelocity().multiply(0.1));
                    this.velocityDirty = true;
                    return 0.1f;
                }
                else return Math.min(0.6f, 0.8f - velocitySlowdownAmount);
            }
            else return 1.0f;
        }
        return super.getVelocityMultiplier();
    }

    @Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
    public void getMaxSpeed(CallbackInfoReturnable<Double> cir) {
        if(cir.getReturnValue() == 8.0) cir.setReturnValue(32.0);
    }

    @ModifyConstant(method = "moveOffRail", constant = @Constant(doubleValue = 0.95))
    private double lessSlowdownWhenInAir(double speedMultiplier) {
        if(this.getVelocity().lengthSquared() < 0.6) return 0.97;
        return 0.96;
    }
}
