package frootloops.versus.mixin.environment;

import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import frootloops.versus.mod.environment.Minecarts.Step;
import frootloops.versus.mod.environment.Minecarts.class_9882;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;


@Mixin(AbstractMinecartEntity.class)
public abstract class MinecartMixin extends VehicleEntity {

    @Shadow private boolean onRail;
    @Shadow @Nullable public Vec3d snapPositionToRail(double x, double y, double z) {return null;}

    @Shadow protected void applySlowdown(){}

    @Shadow private boolean yawFlipped;

    @Shadow public void onActivatorRail(int x, int y, int z, boolean powered) {}

    @Shadow public boolean willHitBlockAt(BlockPos pos) {
        return this.getWorld().getBlockState(pos).isSolidBlock(this.getWorld(), pos);
    }

    @Shadow public abstract AbstractMinecartEntity.Type getMinecartType();

    private final List<Step> stepsList = new LinkedList();
    private final List<Step> field_52530 = new LinkedList();

    public MinecartMixin(EntityType<?> type, World world) {super(type, world);}

    @ModifyConstant(method = "getMaxSpeed", constant = @Constant(doubleValue = 8.0))
    private double fasterMinecarts(double maxSpeed) {
        return getNewMaxSpeed();

        /*
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
        return 8.0; */
    }

    private double getNewMaxSpeed() {
        return 48.0 * (this.isTouchingWater() ? 0.3 : 1.0) / 20.0;
    }

    /*@ModifyConstant(method = "moveOffRail", constant = @Constant(doubleValue = 0.95))
    private double noSlowdownWhenInAir(double speedMultiplier) {
        return 1.0;
    }*/

    @Overwrite
    public void moveOffRail() {
        double d = this.getNewMaxSpeed();
        Vec3d vec3d = this.getVelocity();
        this.setVelocity(MathHelper.clamp(vec3d.x, -d, d), vec3d.y, MathHelper.clamp(vec3d.z, -d, d));
        if (this.isOnGround()) {
            this.setVelocity(this.getVelocity().multiply(0.5));
        }

        this.move(MovementType.SELF, this.getVelocity());
        if (!this.isOnGround()) {
            this.setVelocity(this.getVelocity().multiply(1.0));
        }
    }

    protected Vec3d applySlowdown(Vec3d velocity) {
        double speedRetention = this.hasPassengers() ? 0.997 : 0.975;
        Vec3d newVelocity = velocity.multiply(speedRetention, 0.0, speedRetention);
        if (this.isTouchingWater()) newVelocity = newVelocity.multiply(0.95);
        return newVelocity;
    }

    /*
    @Inject(method = "applySlowdown", at = @At("HEAD"), cancellable = true)
    protected void applySlowdown(CallbackInfo info) {
        double slowdownAmount = this.isTouchingWater() ? 0.91 : 0.997;
        this.setVelocity(this.getVelocity().multiply(slowdownAmount));
        info.cancel();
    }*/

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
    public void moveOnRail(BlockPos pos, BlockState state) {
        for (class_9882 classInstance = new class_9882(); classInstance.method_61618(); classInstance.field_52543 = false) {
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
                Vec3d vec3d = this.method_61601(new Vec3d(this.getVelocity().x, 0.0,this.getVelocity().z), classInstance, blockPos, blockState, railShape);
                if (classInstance.field_52543) {
                    classInstance.field_52542 = vec3d.horizontalLength();
                } else {
                    classInstance.field_52542 = classInstance.field_52542 + (vec3d.horizontalLength() - this.getVelocity().horizontalLength());
                }

                this.setVelocity(vec3d);
                classInstance.field_52542 = this.method_61577(blockPos, railShape, classInstance.field_52542);

            } else {
                this.moveOffRail();
                classInstance.field_52542 = 0.0;
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
                this.stepsList.add(new Step(currentPos, this.getVelocity(), f, g, (float)deltaPos));
            }

            if (deltaPos > 1.0E-5F || classInstance.field_52543) {
                this.method_61409();
            }
        }
    }

    @Override
    public void move(MovementType movementType, Vec3d movement) {
        Vec3d vec3d = this.getPos().add(movement);
        super.move(movementType, movement);
        if (this.horizontalCollision || this.verticalCollision) {
            boolean bl = this.method_61562(this.getBoundingBox().expand(1.0E-7), 0.0);
            if (bl) {
                super.move(movementType, vec3d.subtract(this.getPos()));
            }
        }
    }

    public boolean method_61562(Box box, double d) {
        boolean bl = false;
        if (this.getMinecartType() == AbstractMinecartEntity.Type.RIDEABLE && this.getVelocity().horizontalLengthSquared() >= d) {
            List<Entity> list = this.getWorld().getOtherEntities(this, box, EntityPredicates.canBePushedBy(this));
            if (!list.isEmpty()) {
                for (Entity entity : list) {
                    if (!(entity instanceof PlayerEntity)
                            && !(entity instanceof IronGolemEntity)
                            && !(entity instanceof AbstractMinecartEntity)
                            && !this.hasPassengers()
                            && !entity.hasVehicle()) {
                        entity.startRiding(this);
                        bl = true;
                    } else {
                        entity.pushAwayFrom(this);
                    }
                }
            }
        } else {
            for (Entity entity2 : this.getWorld().getOtherEntities(this, box)) {
                if (!this.hasPassenger(entity2) && entity2.isPushable() && entity2 instanceof AbstractMinecartEntity) {
                    entity2.pushAwayFrom(this);
                }
            }
        }
        return bl;
    }

    public void method_61409() {
        if (!this.isRemoved() && !this.noClip) {
            boolean bl = this.isOnFire();
            if (this.isOnGround()) {
                BlockPos blockPos = this.getLandingPos();
                BlockState blockState = this.getWorld().getBlockState(blockPos);
                blockState.getBlock().onSteppedOn(this.getWorld(), blockPos, blockState, this);
            }
            this.tryCheckBlockCollision();
            float h = this.getVelocityMultiplier();
            this.setVelocity(this.getVelocity().multiply(h, 1.0, h));
            if (this.getWorld().getStatesInBoxIfLoaded(this.getBoundingBox().contract(1.0E-6)).noneMatch(state -> state.isIn(BlockTags.FIRE) || state.isOf(Blocks.LAVA))) {
                if (this.getFireTicks() <= 0) {
                    this.setFireTicks(-this.getBurningDuration());
                }
                if (this.wasOnFire && (this.inPowderSnow || this.isWet())) {
                    this.playExtinguishSound();
                }
            }
            if (this.isOnFire() && (this.inPowderSnow || this.isWet())) {
                this.setFireTicks(-this.getBurningDuration());
            }
        }
    }

    private Vec3d method_61601(Vec3d vec3d, class_9882 arg, BlockPos blockPos, BlockState railState, RailShape railShape) {
        Vec3d vec3d2 = vec3d;
        if (!arg.field_52544) {
            Vec3d vec3d3 = this.method_61603(vec3d, railShape);
            if (vec3d3.horizontalLengthSquared() != vec3d.horizontalLengthSquared()) {
                arg.field_52544 = true;
                vec3d2 = vec3d3;
            }
        }

        if (arg.field_52543) {
            Vec3d vec3d3 = this.method_61609(vec3d2);
            if (vec3d3.horizontalLengthSquared() != vec3d2.horizontalLengthSquared()) {
                arg.field_52545 = true;
                vec3d2 = vec3d3;
            }
        }

        if (!arg.field_52545) {
            Vec3d vec3d3 = this.decelerateFromPoweredRail(vec3d2, railState);
            if (vec3d3.horizontalLengthSquared() != vec3d2.horizontalLengthSquared()) {
                arg.field_52545 = true;
                vec3d2 = vec3d3;
            }
        }

        if (arg.field_52543) {
            vec3d2 = this.applySlowdown(vec3d2);
            if (vec3d2.lengthSquared() > 0.0) {
                double d = Math.min(vec3d2.length(), getNewMaxSpeed());
                vec3d2 = vec3d2.normalize().multiply(d);
            }
        }

        if (!arg.field_52546) {
            Vec3d vec3d3 = this.accelerateFromPoweredRail(vec3d2, blockPos, railState);
            if (vec3d3.horizontalLengthSquared() != vec3d2.horizontalLengthSquared()) {
                arg.field_52546 = true;
                vec3d2 = vec3d3;
            }
        }

        return vec3d2;
    }

    private Vec3d method_61603(Vec3d vec3d, RailShape railShape) {
        double d = Math.max(0.0078125, vec3d.horizontalLength() * 0.02);
        if (this.isTouchingWater()) {
            d *= 0.2;
        }
        return switch (railShape) {
            case ASCENDING_EAST -> vec3d.add(-d, 0.0, 0.0);
            case ASCENDING_WEST -> vec3d.add(d, 0.0, 0.0);
            case ASCENDING_NORTH -> vec3d.add(0.0, 0.0, d);
            case ASCENDING_SOUTH -> vec3d.add(0.0, 0.0, -d);
            default -> vec3d;
        };
    }

    private Vec3d method_61609(Vec3d vec3d) {
        Entity entity = this.getFirstPassenger();
        Vec3d vec3d2 = this.getVelocity();
        if (entity instanceof ServerPlayerEntity && vec3d2.lengthSquared() > 0.0) {
            Vec3d vec3d3 = vec3d2.normalize();
            double d = vec3d.horizontalLengthSquared();
            if (vec3d3.lengthSquared() > 0.0 && d < 0.01) {
                return vec3d.add(new Vec3d(vec3d3.x, 0.0, vec3d3.z).normalize().multiply(0.001));
            }
        } else {
            this.setVelocity(Vec3d.ZERO);
        }
        return vec3d;
    }

    private BlockPos decelerateFromPoweredRail() {
        int i = MathHelper.floor(this.getX());
        int j = MathHelper.floor(this.getY());
        int k = MathHelper.floor(this.getZ());
        if (this.getWorld().getBlockState(new BlockPos(i, j - 1, k)).isIn(BlockTags.RAILS)) {
            j--;
        }
        return new BlockPos(i, j, k);
    }

    private Vec3d decelerateFromPoweredRail(Vec3d velocity, BlockState railState) {
        if (railState.isOf(Blocks.POWERED_RAIL) && !(Boolean)railState.get(PoweredRailBlock.POWERED)) {
            return velocity.length() < 0.03 ? Vec3d.ZERO : velocity.multiply(0.5);
        } else {
            return velocity;
        }
    }

    private Vec3d accelerateFromPoweredRail(Vec3d velocity, BlockPos railPos, BlockState railState) {
        if (railState.isOf(Blocks.POWERED_RAIL) && (Boolean)railState.get(PoweredRailBlock.POWERED)) {
            if (velocity.length() > 0.01) {
                return velocity.normalize().multiply(velocity.length() + 0.06);
            } else {
                Vec3d vec3d = this.getLaunchDirection(railPos);
                return vec3d.lengthSquared() <= 0.0 ? velocity : vec3d.multiply(velocity.length() + 0.2);
            }
        } else {
            return velocity;
        }
    }

    public Vec3d getLaunchDirection(BlockPos railPos) {
        BlockState blockState = this.getWorld().getBlockState(railPos);
        if (blockState.isOf(Blocks.POWERED_RAIL) && (Boolean)blockState.get(PoweredRailBlock.POWERED)) {
            RailShape railShape = blockState.get(((AbstractRailBlock)blockState.getBlock()).getShapeProperty());
            if (railShape == RailShape.EAST_WEST) {
                if (this.willHitBlockAt(railPos.west())) {
                    return new Vec3d(1.0, 0.0, 0.0);
                }

                if (this.willHitBlockAt(railPos.east())) {
                    return new Vec3d(-1.0, 0.0, 0.0);
                }
            } else if (railShape == RailShape.NORTH_SOUTH) {
                if (this.willHitBlockAt(railPos.north())) {
                    return new Vec3d(0.0, 0.0, 1.0);
                }

                if (this.willHitBlockAt(railPos.south())) {
                    return new Vec3d(0.0, 0.0, -1.0);
                }
            }

            return Vec3d.ZERO;
        } else {
            return Vec3d.ZERO;
        }
    }

    private static Pair<Vec3i, Vec3i> getAdjacentRailPositionsByShape(RailShape shape) {
        return ADJACENT_RAIL_POSITIONS_BY_SHAPE.get(shape);
    }

    private void method_61605(BlockPos blockPos, BlockState blockState) {
        if (AbstractRailBlock.isRail(blockState)) {
            RailShape railShape = blockState.get(((AbstractRailBlock)blockState.getBlock()).getShapeProperty());
            Pair<Vec3i, Vec3i> pair = getAdjacentRailPositionsByShape(railShape);
            Vec3i vec3i = pair.getFirst();
            Vec3i vec3i2 = pair.getSecond();
            Vec3d vec3d = new Vec3d(vec3i.getX() * 0.5, 0.0, vec3i.getZ() * 0.5);
            Vec3d vec3d2 = new Vec3d(vec3i2.getX() * 0.5, 0.0, vec3i2.getZ() * 0.5);
            if (this.getVelocity().length() > 1.0E-5F && this.getVelocity().dotProduct(vec3d) < this.getVelocity().dotProduct(vec3d2)) {
                vec3d = vec3d2;
            }

            float f = 180.0F - (float)(Math.atan2(vec3d.z, vec3d.x) * 180.0 / Math.PI);
            f += yawFlipped ? 180.0F : 0.0F;

            this.setYaw(f);
            boolean bl = vec3i.getY() != vec3i2.getY();

            Vec3d oldPos = this.getPos();
            Vec3d newPosition = oldPos.add(blockPos.toBottomCenterPos().subtract(oldPos));
            this.setPos(newPosition.x, newPosition.y, newPosition.z);

            if (bl) {
                Vec3d vec3d5 = blockPos.toBottomCenterPos().add(vec3d2);
                this.setPos(newPosition.x, newPosition.y + 0.1 + vec3d5.distanceTo(newPosition), newPosition.z);
            } else {
                this.setPos(newPosition.x, newPosition.y + 0.1, newPosition.z);
                this.setPitch(0.0F);
            }

            double distanceTravelled = oldPos.distanceTo(this.getPos());
            if (distanceTravelled > 0.0) {
                this.stepsList.add(new Step(this.getPos(), this.getVelocity(), this.getYaw(), this.getPitch(), (float)distanceTravelled));
            }
        }
    }

    public double method_61577(BlockPos blockPos, RailShape railShape, double d) {
        if (d < 1.0E-5F) {
            return 0.0;
        } else {
            Vec3d vec3d = this.getPos();
            Pair<Vec3i, Vec3i> pair = getAdjacentRailPositionsByShape(railShape);
            Vec3i vec3i = pair.getFirst();
            Vec3i vec3i2 = pair.getSecond();
            Vec3d vec3d2 = this.getVelocity().multiply(1.0, 0.0, 1.0);
            if (vec3d2.length() < 1.0E-5F) {
                this.setVelocity(Vec3d.ZERO);
                return 0.0;
            } else {
                boolean bl = vec3i.getY() != vec3i2.getY();
                Vec3d vec3d3 = new Vec3d(vec3i2.getX(), vec3i2.getY(), vec3i2.getZ()).multiply(0.5, 0.0, 0.5);
                Vec3d vec3d4 = new Vec3d(vec3i.getX(), vec3i.getY(), vec3i.getZ()).multiply(0.5, 0.0, 0.5);
                if (vec3d2.dotProduct(vec3d4) < vec3d2.dotProduct(vec3d3)) {
                    vec3d4 = vec3d3;
                }

                Vec3d vec3d5 = blockPos.toBottomCenterPos().add(vec3d4).add(0.0, 0.1, 0.0).add(vec3d4.normalize().multiply(1.0E-5F));
                if (bl && !this.ascends(vec3d2, railShape)) {
                    vec3d5 = vec3d5.add(0.0, 1.0, 0.0);
                }

                Vec3d vec3d6 = vec3d5.subtract(this.getPos()).normalize();
                vec3d2 = vec3d6.multiply(vec3d2.length() / vec3d6.horizontalLength());
                Vec3d positionToMoveTo = vec3d.add(vec3d2.normalize().multiply(d * (double)(bl ? MathHelper.SQUARE_ROOT_OF_TWO : 1.0F)));
                if (vec3d.squaredDistanceTo(vec3d5) <= vec3d.squaredDistanceTo(positionToMoveTo)) {
                    d = vec3d5.subtract(positionToMoveTo).horizontalLength();
                    positionToMoveTo = vec3d5;
                } else {
                    d = 0.0;
                }

                this.move(MovementType.SELF, positionToMoveTo.subtract(vec3d));
                BlockPos blockPos2 = BlockPos.ofFloored(positionToMoveTo);
                BlockState blockState = this.getWorld().getBlockState(blockPos2);
                if (bl && AbstractRailBlock.isRail(blockState)) {
                    this.setPos(positionToMoveTo.x, positionToMoveTo.y, positionToMoveTo.z);
                }

                if (this.getPos().distanceTo(vec3d) < 1.0E-5F && positionToMoveTo.distanceTo(vec3d) > 1.0E-5F) {
                    this.setVelocity(Vec3d.ZERO);
                    return 0.0;
                } else {
                    this.setVelocity(vec3d2);
                    return d;
                }
            }
        }
    }

    private boolean ascends(Vec3d velocity, RailShape railShape) {
        return switch (railShape) {
            case ASCENDING_EAST -> velocity.x < 0.0;
            case ASCENDING_WEST -> velocity.x > 0.0;
            case ASCENDING_NORTH -> velocity.z > 0.0;
            case ASCENDING_SOUTH -> velocity.z < 0.0;
            default -> false;
        };
    }

    private double getSpeedRetention() {
        return this.hasPassengers() ? 0.997 : 0.975;
    }

    static record InterpolatedStep(float partialTicksInStep, Step currentStep, Step previousStep) {
    }

    private static final Map<RailShape, Pair<Vec3i, Vec3i>> ADJACENT_RAIL_POSITIONS_BY_SHAPE = Util.make(Maps.newEnumMap(RailShape.class), map -> {
        Vec3i vec3i = Direction.WEST.getVector();
        Vec3i vec3i2 = Direction.EAST.getVector();
        Vec3i vec3i3 = Direction.NORTH.getVector();
        Vec3i vec3i4 = Direction.SOUTH.getVector();
        Vec3i vec3i5 = vec3i.down();
        Vec3i vec3i6 = vec3i2.down();
        Vec3i vec3i7 = vec3i3.down();
        Vec3i vec3i8 = vec3i4.down();
        map.put(RailShape.NORTH_SOUTH, Pair.of(vec3i3, vec3i4));
        map.put(RailShape.EAST_WEST, Pair.of(vec3i, vec3i2));
        map.put(RailShape.ASCENDING_EAST, Pair.of(vec3i5, vec3i2));
        map.put(RailShape.ASCENDING_WEST, Pair.of(vec3i, vec3i6));
        map.put(RailShape.ASCENDING_NORTH, Pair.of(vec3i3, vec3i8));
        map.put(RailShape.ASCENDING_SOUTH, Pair.of(vec3i7, vec3i4));
        map.put(RailShape.SOUTH_EAST, Pair.of(vec3i4, vec3i2));
        map.put(RailShape.SOUTH_WEST, Pair.of(vec3i4, vec3i));
        map.put(RailShape.NORTH_WEST, Pair.of(vec3i3, vec3i));
        map.put(RailShape.NORTH_EAST, Pair.of(vec3i3, vec3i2));
    });
}
