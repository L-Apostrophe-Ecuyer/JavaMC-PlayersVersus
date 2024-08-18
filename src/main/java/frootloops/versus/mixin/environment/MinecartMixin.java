package frootloops.versus.mixin.environment;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(AbstractMinecartEntity.class)
public abstract class MinecartMixin extends VehicleEntity {

    @Shadow private boolean onRail;
    @Shadow @Nullable public Vec3d snapPositionToRail(double x, double y, double z) {return null;}

    @Shadow protected void applySlowdown(){}

    @Shadow private boolean yawFlipped;

    @Shadow public void onActivatorRail(int x, int y, int z, boolean powered) {}

    public MinecartMixin(EntityType<?> type, World world) {super(type, world);}

    @ModifyConstant(method = "getMaxSpeed", constant = @Constant(doubleValue = 8.0))
    private double fasterMinecarts(double maxSpeed) {
        int i = MathHelper.floor(this.getX());
        int j = MathHelper.floor(this.getY());
        int k = MathHelper.floor(this.getZ());
        if (this.getWorld().getBlockState(new BlockPos(i, j - 1, k)).isIn(BlockTags.RAILS)) {
            --j;
        }
        BlockState blockState = this.getWorld().getBlockState(new BlockPos(i, j, k));
        if(AbstractRailBlock.isRail(blockState)) {
            RailShape railShape = blockState.get(((AbstractRailBlock)blockState.getBlock()).getShapeProperty());
            if(railShape == RailShape.EAST_WEST || railShape == RailShape.NORTH_SOUTH) return 24.0;
        }
        return 8.0;
    }

    @ModifyConstant(method = "moveOffRail", constant = @Constant(doubleValue = 0.95))
    private double noSlowdownWhenInAir(double speedMultiplier) {

        return 1.0;
    }

    @Inject(method = "applySlowdown", at = @At("HEAD"), cancellable = true)
    protected void applySlowdown(CallbackInfo info) {
        double slowdownAmount = this.isTouchingWater() ? 0.91 : 0.997;
        this.setVelocity(this.getVelocity().multiply(slowdownAmount));
        info.cancel();
    }

    /*
    @Inject(method = "moveOnRail", at = @At("HEAD"), cancellable = false)
    protected void avoidDerailing(BlockPos pos, BlockState state, CallbackInfo info) {
        if(this.getVelocity().horizontalLengthSquared() > 0.99) {
            RailShape railShape = state.get(((AbstractRailBlock) state.getBlock()).getShapeProperty());
            if (railShape != RailShape.NORTH_SOUTH || railShape != RailShape.EAST_WEST) {
                Vec3d velocity = this.getVelocity();
                double speed = velocity.horizontalLength();
                this.setVelocity((velocity.x / speed) * 0.99, (velocity.y / speed) * 0.99, (velocity.z / speed) * 0.99);
            }
        }
    }*/

    @Overwrite
    protected void moveOnRail(BlockPos pos, BlockState state) {
        for (ExperimentalMinecartController.class_9882 lv = new ExperimentalMinecartController.class_9882(); lv.method_61618(); lv.field_52543 = false) {
            BlockPos blockPos = this.decelerateFromPoweredRail();
            BlockState blockState = this.getWorld().getBlockState(blockPos);
            boolean bl = AbstractRailBlock.isRail(blockState);
            if (this.isOnRail() != bl) {
                this.onRail = true;
                this.method_61605(blockPos, blockState);
            }

            if (bl) {
                this.onLanding();
                this.resetPosition();
                if (blockState.isOf(Blocks.ACTIVATOR_RAIL)) {
                    this.onActivatorRail(blockPos.getX(), blockPos.getY(), blockPos.getZ(), (Boolean)blockState.get(PoweredRailBlock.POWERED));
                }

                RailShape railShape = blockState.get(((AbstractRailBlock)blockState.getBlock()).getShapeProperty());
                Vec3d vec3d = this.method_61601(this.getVelocity().getHorizontal(), lv, blockPos, blockState, railShape);
                if (lv.field_52543) {
                    lv.field_52542 = vec3d.horizontalLength();
                } else {
                    lv.field_52542 = lv.field_52542 + (vec3d.horizontalLength() - this.getVelocity().horizontalLength());
                }

                this.setVelocity(vec3d);
                lv.field_52542 = this.method_61564(blockPos, railShape, lv.field_52542);

            } else {
                this.moveOffRail();
                lv.field_52542 = 0.0;
            }

            Vec3d currentPos = this.getPos();
            double deltaPos = new Vec3d(lastRenderX, lastRenderY, lastRenderZ).subtract(currentPos).length();

            if (deltaPos > 1.0E-5F) {
                float f = this.getYaw();
                if (this.getVelocity().horizontalLengthSquared() > 0.0) {
                    f = 180.0F - (float)(Math.atan2(this.getVelocity().z, this.getVelocity().x) * 180.0 / Math.PI);
                    f += yawFlipped ? 180.0F : 0.0F;
                }

                float g = this.isOnGround() && !this.isOnRail()
                        ? 0.0F
                        : 90.0F - (float)(Math.atan2(this.getVelocity().horizontalLength(), this.getVelocity().y) * 180.0 / Math.PI);
                g *= yawFlipped ? -1.0F : 1.0F;

                double e = (double)Math.abs(f - this.getYaw());
                if (e >= 175.0 && e <= 185.0) {
                    yawFlipped = !yawFlipped;
                    f -= 180.0F;
                    g *= -1.0F;
                }

                g = Math.clamp(g, -45.0F, 45.0F);
                this.setPitch(g % 360.0F);
                this.setYaw(f % 360.0F);
                this.field_52529.add(new Step(currentPos, this.getVelocity(), f, g, (float)deltaPos));
            }

            if (deltaPos > 1.0E-5F || lv.field_52543) {
                this.method_61409();
            }
        }
    }
}
