package frootloops.versus.mixin.environment;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(AbstractMinecartEntity.class)
public abstract class MinecartMixin extends VehicleEntity {

    @Shadow private boolean onRail;
    @Shadow @Nullable public Vec3d snapPositionToRail(double x, double y, double z) {return null;}

    @Shadow protected void applySlowdown(){}

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
    }

    @Inject(method = "applySlowdown", at = @At("HEAD"), cancellable = true)
    protected void applySlowdown(CallbackInfo info) {
        double slowdownAmount = this.isTouchingWater() ? 0.91 : 0.997;
        this.setVelocity(this.getVelocity().multiply(slowdownAmount));
        info.cancel();
    }

    @ModifyConstant(method = "moveOffRail", constant = @Constant(doubleValue = 0.95))
    private double lessSlowdownWhenInAir(double speedMultiplier) {
        if(this.getVelocity().lengthSquared() < 0.6) return 0.97;
        return 0.96;
    }
}
