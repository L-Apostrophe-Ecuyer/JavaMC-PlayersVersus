package frootloops.versus.mod.mobs.hostile.ai;

import frootloops.versus.VersusMod;
import frootloops.versus.mixin.mobs.hostile.overworld.PhantomAccessor;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class PhantomMoveControlRevamp extends MoveControl {
    private float targetSpeed;
    private Vec3d targetPos;

    public PhantomMoveControlRevamp(PhantomEntity owner) {
        super(owner);
        this.targetSpeed = 0.1f;
        targetPos = null;
    }


    @Override
    public void tick() {
        if (this.entity.horizontalCollision || this.entity.verticalCollision) {
            this.entity.setYaw(this.entity.getYaw() + 180.0f);
            this.targetSpeed = 0.1f;
            this.targetPos = this.entity.getPos().add(0, 8, 0);
        }
        else {
            this.targetPos = ((PhantomAccessor)this.entity).getTargetPosition();
        }

        double deltaX = targetPos.x - this.entity.getX();
        double deltaY = targetPos.y - this.entity.getY();
        double deltaZ = targetPos.z - this.entity.getZ();
        double distanceHorizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (distanceHorizontal > (double)1.0E-5f){

            double h = 1.0 - Math.abs(deltaY * (double)0.7f) / distanceHorizontal;
            distanceHorizontal = Math.sqrt((deltaX *= h) * deltaX + (deltaZ *= h) * deltaZ);
            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ + deltaY * deltaY);

            float yaw = this.entity.getYaw();
            float yawOrthogonal = MathHelper.wrapDegrees(this.entity.getYaw() + 90.0f);
            float newYaw = MathHelper.stepUnwrappedAngleTowards(yawOrthogonal, MathHelper.wrapDegrees((float) MathHelper.atan2(deltaZ, deltaX) * 57.295776f), 4.0f) - 90.0f;

            this.entity.setYaw(newYaw);
            this.entity.bodyYaw = newYaw;

            // Added distance, so now, can't modify yer yaw if ye try homing in on your target!
            boolean shouldSlowDownToAdjustYaw =  (distance > 8) && MathHelper.angleBetween(yaw, newYaw) < 3.0f;
            this.targetSpeed = shouldSlowDownToAdjustYaw ?
                    MathHelper.stepTowards(this.targetSpeed, 1.8f, 0.005f * (1.8f / this.targetSpeed)) :
                    MathHelper.stepTowards(this.targetSpeed, 0.6f, 0.025f);

            float pitch = (float)(-(MathHelper.atan2(-deltaY, distanceHorizontal) * 57.2957763671875));
            this.entity.setPitch(pitch);

            float angle = newYaw + 90.0f;
            double velocityX = (double)(this.targetSpeed * MathHelper.cos(angle * ((float)Math.PI / 180))) * Math.abs(deltaX / distance);
            double velocityZ = (double)(this.targetSpeed * MathHelper.sin(angle * ((float)Math.PI / 180))) * Math.abs(deltaZ / distance);
            double velocityY = (double)(this.targetSpeed * MathHelper.sin(this.entity.getPitch() * ((float)Math.PI / 180))) * Math.abs(deltaY / distance);

            Vec3d velocity = this.entity.getVelocity();
            this.entity.setVelocity(velocity.add(new Vec3d(velocityX, velocityY, velocityZ).subtract(velocity).multiply(0.2)));
        }
    }
}