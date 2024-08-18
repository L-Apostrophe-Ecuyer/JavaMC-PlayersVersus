package frootloops.versus.mod.environment;
import com.mojang.datafixers.util.Pair;
import io.netty.buffer.ByteBuf;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.FurnaceMinecartEntity;
import net.minecraft.entity.vehicle.MinecartController;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.GameRules;
import org.jetbrains.annotations.Nullable;

public class ExperimentalMinecartController extends MinecartController {
    public static final int field_52527 = 3;
    public static final double field_52528 = 0.1;
    @Nullable
    private InterpolatedStep interpolatedStep;
    private int field_52534;
    private float field_52535;
    private int cooldown = 0;
    public final List<Step> field_52529 = new LinkedList();
    public final List<Step> field_52530 = new LinkedList();
    public double field_52531 = 0.0;
    public Step field_52532 = Step.ZERO;
    private boolean field_52537 = true;

    public ExperimentalMinecartController(AbstractMinecartEntity abstractMinecartEntity) {
        super(abstractMinecartEntity);
    }

    @Override
    public void method_61587() {
        if (this.getWorld().isClient) {
            this.method_61615();
            boolean bl = AbstractRailBlock.isRail(this.getWorld().getBlockState(this.minecart.decelerateFromPoweredRail()));
            this.minecart.setOnRail(bl);
            this.field_52537 = false;
        } else {
            BlockPos blockPos = this.minecart.decelerateFromPoweredRail();
            BlockState blockState = this.getWorld().getBlockState(blockPos);
            if (this.field_52537) {
                this.minecart.setOnRail(AbstractRailBlock.isRail(blockState));
                this.method_61605(blockPos, blockState);
            }

            this.minecart.applyGravity();
            method_61589(); // Move on rail
            this.field_52537 = false;
        }
    }

    private void method_61615() {
        if (--this.cooldown <= 0) {
            this.method_61613();
            this.field_52530.clear();
            if (!this.field_52529.isEmpty()) {
                this.field_52530.addAll(this.field_52529);
                this.field_52529.clear();
                this.cooldown = 3;
                this.field_52531 = 0.0;

                for (Step step : this.field_52530) {
                    this.field_52531 = this.field_52531 + (double)step.weight;
                }
            }
        }

        if (this.method_61614()) {
            this.setPos(this.getLerpedPosition(1.0F));
            this.setVelocity(this.getLerpedVelocity(1.0F));
            this.setPitch(this.getLerpedPitch(1.0F));
            this.setYaw(this.getLerpedYaw(1.0F));
        }
    }

    public void method_61613() {
        this.field_52532 = new Step(this.getPos(), this.getVelocity(), this.getYaw(), this.getPitch(), 0.0F);
    }

    public boolean method_61614() {
        return !this.field_52530.isEmpty();
    }

    public float getLerpedPitch(float tickDelta) {
        InterpolatedStep interpolatedStep = this.method_61612(tickDelta);
        return MathHelper.lerpAngleDegrees(interpolatedStep.partialTicksInStep, interpolatedStep.previousStep.xRot, interpolatedStep.currentStep.xRot);
    }

    public float getLerpedYaw(float tickDelta) {
        InterpolatedStep interpolatedStep = this.method_61612(tickDelta);
        return MathHelper.lerpAngleDegrees(interpolatedStep.partialTicksInStep, interpolatedStep.previousStep.yRot, interpolatedStep.currentStep.yRot);
    }

    public Vec3d getLerpedPosition(float tickDelta) {
        InterpolatedStep interpolatedStep = this.method_61612(tickDelta);
        return MathHelper.lerp((double)interpolatedStep.partialTicksInStep, interpolatedStep.previousStep.position, interpolatedStep.currentStep.position);
    }

    public Vec3d getLerpedVelocity(float tickDelta) {
        InterpolatedStep interpolatedStep = this.method_61612(tickDelta);
        return MathHelper.lerp((double)interpolatedStep.partialTicksInStep, interpolatedStep.previousStep.movement, interpolatedStep.currentStep.movement);
    }

    private InterpolatedStep method_61612(float tickDelta) {
        if (tickDelta == this.field_52535 && this.cooldown == this.field_52534 && this.interpolatedStep != null) {
            return this.interpolatedStep;
        } else {
            float f = ((float)(3 - this.cooldown) + tickDelta) / 3.0F;
            float g = 0.0F;
            float h = 1.0F;
            boolean bl = false;

            int i;
            for (i = 0; i < this.field_52530.size(); i++) {
                float j = ((Step)this.field_52530.get(i)).weight;
                if (!(j <= 0.0F)) {
                    g += j;
                    if ((double)g >= this.field_52531 * (double)f) {
                        float k = g - j;
                        h = (float)(((double)f * this.field_52531 - (double)k) / (double)j);
                        bl = true;
                        break;
                    }
                }
            }

            if (!bl) {
                i = this.field_52530.size() - 1;
            }

            Step step = (Step)this.field_52530.get(i);
            Step step2 = i > 0 ? (Step)this.field_52530.get(i - 1) : this.field_52532;
            this.interpolatedStep = new InterpolatedStep(h, step, step2);
            this.field_52534 = this.cooldown;
            this.field_52535 = tickDelta;
            return this.interpolatedStep;
        }
    }

    private void method_61605(BlockPos blockPos, BlockState blockState) {
        if (AbstractRailBlock.isRail(blockState)) {
            RailShape railShape = blockState.get(((AbstractRailBlock)blockState.getBlock()).getShapeProperty());
            Pair<Vec3i, Vec3i> pair = AbstractMinecartEntity.getAdjacentRailPositionsByShape(railShape);
            Vec3i vec3i = pair.getFirst();
            Vec3i vec3i2 = pair.getSecond();
            Vec3d vec3d = new Vec3d(vec3i).multiply(0.5).getHorizontal();
            Vec3d vec3d2 = new Vec3d(vec3i2).multiply(0.5).getHorizontal();
            if (this.getVelocity().length() > 1.0E-5F && this.getVelocity().dotProduct(vec3d) < this.getVelocity().dotProduct(vec3d2)) {
                vec3d = vec3d2;
            }

            float f = 180.0F - (float)(Math.atan2(vec3d.z, vec3d.x) * 180.0 / Math.PI);
            f += this.minecart.isYawFlipped() ? 180.0F : 0.0F;
            this.setYaw(f);
            boolean bl = vec3i.getY() != vec3i2.getY();
            Vec3d vec3d3 = this.getPos();
            Vec3d vec3d4 = blockPos.toBottomCenterPos().subtract(vec3d3);
            this.setPos(vec3d3.add(vec3d4));
            if (bl) {
                Vec3d vec3d5 = blockPos.toBottomCenterPos().add(vec3d2);
                double d = vec3d5.distanceTo(this.getPos());
                this.setPos(this.getPos().add(0.0, d + 0.1, 0.0));
            } else {
                this.setPos(this.getPos().add(0.0, 0.1, 0.0));
                this.setPitch(0.0F);
            }

            double e = vec3d3.distanceTo(this.getPos());
            if (e > 0.0) {
                this.field_52529.add(new Step(this.getPos(), this.getVelocity(), this.getYaw(), this.getPitch(), (float)e));
            }
        }
    }

    @Override
    public void method_61589() {
        moveOnRail();
    }
    private void moveOnRail() {
        for (class_9882 lv = new class_9882(); lv.method_61618(); lv.field_52543 = false) {
            BlockPos blockPos = this.minecart.decelerateFromPoweredRail();
            BlockState blockState = this.getWorld().getBlockState(blockPos);
            boolean bl = AbstractRailBlock.isRail(blockState);
            if (this.minecart.isOnRail() != bl) {
                this.minecart.setOnRail(bl);
                this.method_61605(blockPos, blockState);
            }

            if (bl) {
                this.minecart.onLanding();
                this.minecart.resetPosition();
                if (blockState.isOf(Blocks.ACTIVATOR_RAIL)) {
                    this.minecart.onActivatorRail(blockPos.getX(), blockPos.getY(), blockPos.getZ(), (Boolean)blockState.get(PoweredRailBlock.POWERED));
                }

                RailShape railShape = blockState.get(((AbstractRailBlock)blockState.getBlock()).getShapeProperty());
                Vec3d vec3d = this.method_61601(this.getVelocity().getHorizontal(), lv, blockPos, blockState, railShape);
                if (lv.field_52543) {
                    lv.field_52542 = vec3d.horizontalLength();
                } else {
                    lv.field_52542 = lv.field_52542 + (vec3d.horizontalLength() - this.getVelocity().horizontalLength());
                }

                this.setVelocity(vec3d);
                lv.field_52542 = this.method_61577(blockPos, railShape, lv.field_52542);
            } else {
                // Move off rail
                double maxSpeed = getMaxSpeed();
                Vec3d vec3d = minecart.getVelocity();
                this.setVelocity(MathHelper.clamp(vec3d.x, -maxSpeed, maxSpeed), vec3d.y, MathHelper.clamp(vec3d.z, -maxSpeed, maxSpeed));
                if (minecart.isOnGround()) {
                    minecart.setVelocity(this.getVelocity().multiply(0.5));
                }

                minecart.move(MovementType.SELF, this.getVelocity());
                if (!minecart.isOnGround()) {
                    this.setVelocity(this.getVelocity().multiply(0.95));
                }
                lv.field_52542 = 0.0;
            }

            Vec3d vec3d2 = this.getPos();
            double d = this.minecart.getLastRenderPos().subtract(vec3d2).length();
            if (d > 1.0E-5F) {
                float f = this.getYaw();
                if (this.getVelocity().horizontalLengthSquared() > 0.0) {
                    f = 180.0F - (float)(Math.atan2(this.getVelocity().z, this.getVelocity().x) * 180.0 / Math.PI);
                    f += this.minecart.isYawFlipped() ? 180.0F : 0.0F;
                }

                float g = this.minecart.isOnGround() && !this.minecart.isOnRail()
                        ? 0.0F
                        : 90.0F - (float)(Math.atan2(this.getVelocity().horizontalLength(), this.getVelocity().y) * 180.0 / Math.PI);
                g *= this.minecart.isYawFlipped() ? -1.0F : 1.0F;
                double e = (double)Math.abs(f - this.getYaw());
                if (e >= 175.0 && e <= 185.0) {
                    this.minecart.setYawFlipped(!this.minecart.isYawFlipped());
                    f -= 180.0F;
                    g *= -1.0F;
                }

                g = Math.clamp(g, -45.0F, 45.0F);
                this.setPitch(g % 360.0F);
                this.setYaw(f % 360.0F);
                this.field_52529.add(new Step(vec3d2, this.getVelocity(), f, g, (float)d));
            }

            if (d > 1.0E-5F || lv.field_52543) {
                this.minecart.method_61409();
            }
        }
    }

    private Vec3d method_61601(Vec3d vec3d, class_9882 arg, BlockPos blockPos, BlockState railState, RailShape railShape) {
        Vec3d velocity = vec3d;
        if (!arg.field_52544) {
            Vec3d vec3d3 = this.method_61603(vec3d, railShape);
            if (vec3d3.horizontalLengthSquared() != vec3d.horizontalLengthSquared()) {
                arg.field_52544 = true;
                velocity = vec3d3;
            }
        }

        if (arg.field_52543) {
            Vec3d vec3d3 = this.method_61609(velocity);
            if (vec3d3.horizontalLengthSquared() != velocity.horizontalLengthSquared()) {
                arg.field_52545 = true;
                velocity = vec3d3;
            }
        }

        if (!arg.field_52545) {
            Vec3d vec3d3 = this.decelerateFromPoweredRail(velocity, railState);
            if (vec3d3.horizontalLengthSquared() != velocity.horizontalLengthSquared()) {
                arg.field_52545 = true;
                velocity = vec3d3;
            }
        }

        if (arg.field_52543) {

            // Apply slowdown:
            double speedRetention = getSpeedRetention();
            velocity = velocity.multiply(speedRetention, 0.0, speedRetention);
            if (minecart.isTouchingWater()) velocity = velocity.multiply(0.95F);

            if (velocity.lengthSquared() > 0.0) {
                double d = Math.min(velocity.length(), getMaxSpeed());
                velocity = velocity.normalize().multiply(d);
            }
        }

        if (!arg.field_52546) {
            Vec3d vec3d3 = this.accelerateFromPoweredRail(velocity, blockPos, railState);
            if (vec3d3.horizontalLengthSquared() != velocity.horizontalLengthSquared()) {
                arg.field_52546 = true;
                velocity = vec3d3;
            }
        }

        return velocity;
    }

    private Vec3d method_61603(Vec3d vec3d, RailShape railShape) {
        double d = Math.max(0.0078125, vec3d.horizontalLength() * 0.02);
        if (this.minecart.isTouchingWater()) {
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
        Entity entity = this.minecart.getFirstPassenger();
        Vec3d vec3d2 = this.minecart.getMovementVelocity();
        if (entity instanceof ServerPlayerEntity && vec3d2.lengthSquared() > 0.0) {
            Vec3d vec3d3 = vec3d2.normalize();
            double d = vec3d.horizontalLengthSquared();
            if (vec3d3.lengthSquared() > 0.0 && d < 0.01) {
                return vec3d.add(new Vec3d(vec3d3.x, 0.0, vec3d3.z).normalize().multiply(0.001));
            }
        } else {
            this.minecart.setMovementVelocity(Vec3d.ZERO);
        }

        return vec3d;
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
                Vec3d vec3d = this.minecart.getLaunchDirection(railPos);
                return vec3d.lengthSquared() <= 0.0 ? velocity : vec3d.multiply(velocity.length() + 0.2);
            }
        } else {
            return velocity;
        }
    }

    @Override
    public double method_61577(BlockPos blockPos, RailShape railShape, double d) {
        if (d < 1.0E-5F) {
            return 0.0;
        } else {
            Vec3d vec3d = this.getPos();
            Pair<Vec3i, Vec3i> pair = AbstractMinecartEntity.getAdjacentRailPositionsByShape(railShape);
            Vec3i vec3i = pair.getFirst();
            Vec3i vec3i2 = pair.getSecond();
            Vec3d vec3d2 = this.getVelocity().getHorizontal();
            if (vec3d2.length() < 1.0E-5F) {
                this.setVelocity(Vec3d.ZERO);
                return 0.0;
            } else {
                boolean bl = vec3i.getY() != vec3i2.getY();
                Vec3d vec3d3 = new Vec3d(vec3i2).multiply(0.5).getHorizontal();
                Vec3d vec3d4 = new Vec3d(vec3i).multiply(0.5).getHorizontal();
                if (vec3d2.dotProduct(vec3d4) < vec3d2.dotProduct(vec3d3)) {
                    vec3d4 = vec3d3;
                }

                Vec3d vec3d5 = blockPos.toBottomCenterPos().add(vec3d4).add(0.0, 0.1, 0.0).add(vec3d4.normalize().multiply(1.0E-5F));
                if (bl && !this.ascends(vec3d2, railShape)) {
                    vec3d5 = vec3d5.add(0.0, 1.0, 0.0);
                }

                Vec3d vec3d6 = vec3d5.subtract(this.getPos()).normalize();
                vec3d2 = vec3d6.multiply(vec3d2.length() / vec3d6.horizontalLength());
                Vec3d vec3d7 = vec3d.add(vec3d2.normalize().multiply(d * (double)(bl ? MathHelper.SQUARE_ROOT_OF_TWO : 1.0F)));
                if (vec3d.squaredDistanceTo(vec3d5) <= vec3d.squaredDistanceTo(vec3d7)) {
                    d = vec3d5.subtract(vec3d7).horizontalLength();
                    vec3d7 = vec3d5;
                } else {
                    d = 0.0;
                }

                this.minecart.move(MovementType.SELF, vec3d7.subtract(vec3d));
                BlockPos blockPos2 = BlockPos.ofFloored(vec3d7);
                BlockState blockState = this.getWorld().getBlockState(blockPos2);
                if (bl && AbstractRailBlock.isRail(blockState)) {
                    this.setPos(vec3d7);
                }

                if (this.getPos().distanceTo(vec3d) < 1.0E-5F && vec3d7.distanceTo(vec3d) > 1.0E-5F) {
                    this.setVelocity(Vec3d.ZERO);
                    return 0.0;
                } else {
                    this.setVelocity(vec3d2);
                    return d;
                }
            }
        }
    }

    @Override
    public double getMaxSpeed() {
        return 48.0 * (this.minecart.isTouchingWater() ? 0.5 : 1.0) * (minecart instanceof FurnaceMinecartEntity ? 1.5 : 1.0) / 20.0;
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

    @Override
    public double getSpeedRetention() {
        return this.minecart.hasPassengers() ? 0.997 : 0.975;
    }

    static record InterpolatedStep(float partialTicksInStep, Step currentStep, Step previousStep) {
    }

    public static record Step(Vec3d position, Vec3d movement, float yRot, float xRot, float weight) {
        public static final PacketCodec<ByteBuf, Float> DEGREES_AS_BYTE_PACKET_CODEC = PacketCodecs.BYTE
                .xmap(Step::byteToDegrees, Step::degreesToByte);
        public static final PacketCodec<ByteBuf, Step> PACKET_CODEC = PacketCodec.tuple(
                Vec3d.PACKET_CODEC,
                Step::position,
                Vec3d.PACKET_CODEC,
                Step::movement,
                DEGREES_AS_BYTE_PACKET_CODEC,
                Step::yRot,
                DEGREES_AS_BYTE_PACKET_CODEC,
                Step::xRot,
                PacketCodecs.FLOAT,
                Step::weight,
                Step::new
        );
        public static Step ZERO = new Step(Vec3d.ZERO, Vec3d.ZERO, 0.0F, 0.0F, 0.0F);

        private static byte degreesToByte(float degrees) {
            return (byte)MathHelper.floor(degrees * 256.0F / 360.0F);
        }

        private static float byteToDegrees(byte b) {
            return (float)b * 360.0F / 256.0F;
        }
    }

    static class class_9882 {
        double field_52542 = 0.0;
        boolean field_52543 = true;
        boolean field_52544 = false;
        boolean field_52545 = false;
        boolean field_52546 = false;

        public boolean method_61618() {
            return this.field_52543 || this.field_52542 > 1.0E-5F;
        }
    }
}
