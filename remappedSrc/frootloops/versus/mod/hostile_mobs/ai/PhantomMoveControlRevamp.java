package frootloops.versus.mod.hostile_mobs.ai;

import frootloops.versus.mixin.mobs.hostile.overworld.PhantomAccessor;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class PhantomMoveControlRevamp extends MoveControl {
    private float targetSpeed;
    private Vec3d targetPos;

    public PhantomMoveControlRevamp(PhantomEntity owner) {
        super(owner);
        this.targetSpeed = 0.1f;
        this.targetPos = ((PhantomAccessor)owner).getTargetPosition();
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
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (Math.abs(distance) > (double)1.0E-5f){

            double h = 1.0 - Math.abs(deltaY * (double)0.7f) / distance;
            distance = Math.sqrt((deltaX *= h) * deltaX + (deltaZ *= h) * deltaZ);
            double i = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ + deltaY * deltaY);

            // Can't modify neither yaw, pitch nor speed if ye being a cunt!
            boolean isActingLikeHomingMissile = (!this.entity.verticalCollision) && (this.entity.getTarget() != null) && (Math.abs(deltaY) < 4) && (deltaX * deltaZ < 32);
            if(!isActingLikeHomingMissile) {
                float yaw = this.entity.getYaw();
                float k = (float) MathHelper.atan2(deltaZ, deltaX);
                float l = MathHelper.wrapDegrees(this.entity.getYaw() + 90.0f);
                float m = MathHelper.wrapDegrees(k * 57.295776f);

                this.entity.setYaw(MathHelper.stepUnwrappedAngleTowards(l, m, 4.0f) - 90.0f);
                this.entity.bodyYaw = this.entity.getYaw();

                this.targetSpeed = MathHelper.angleBetween(yaw,this.entity.getYaw()) < 3.0f ?
                        MathHelper.stepTowards(this.targetSpeed, 1.8f, 0.005f * (1.8f / this.targetSpeed)) :
                        MathHelper.stepTowards(this.targetSpeed, 0.2f, 0.025f);

                float pitch = (float)(-(MathHelper.atan2(-deltaY, distance) * 57.2957763671875));
                this.entity.setPitch(pitch);
            }

            float angle = this.entity.getYaw() + 90.0f;
            double velocityX = (double)(this.targetSpeed * MathHelper.cos(angle * ((float)Math.PI / 180))) * Math.abs(deltaX / i);
            double velocityZ = (double)(this.targetSpeed * MathHelper.sin(angle * ((float)Math.PI / 180))) * Math.abs(deltaZ / i);
            double velocityY = (double)(this.targetSpeed * MathHelper.sin(this.entity.getPitch() * ((float)Math.PI / 180))) * Math.abs(deltaY / i);

            Vec3d velocity = this.entity.getVelocity();
            this.entity.setVelocity(velocity.add(new Vec3d(velocityX, velocityY, velocityZ).subtract(velocity).multiply(0.2)));
        }
    }
}
