package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.VersusMod;
import frootloops.versus.mixin.mobs.hostile.overworld.PhantomAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.phys.Vec3;

public class PhantomMoveControlRevamp extends MoveControl {
    private float targetSpeed;
    private Vec3 targetPos;

    public PhantomMoveControlRevamp(Phantom owner) {
        super(owner);
        this.targetSpeed = 0.1f;
        targetPos = null;
    }


    @Override
    public void tick() {
        if (this.mob.horizontalCollision || this.mob.verticalCollision) {
            this.mob.setYRot(this.mob.getYRot() + 180.0f);
            this.targetSpeed = 0.1f;
            this.targetPos = this.mob.position().add(0, 8, 0);
        }
        else {
            this.targetPos = ((PhantomAccessor)this.mob).getTargetPosition();
        }

        double deltaX = targetPos.x - this.mob.getX();
        double deltaY = targetPos.y - this.mob.getY();
        double deltaZ = targetPos.z - this.mob.getZ();
        double distanceHorizontal = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (distanceHorizontal > (double)1.0E-5f){

            double h = 1.0 - Math.abs(deltaY * (double)0.7f) / distanceHorizontal;
            distanceHorizontal = Math.sqrt((deltaX *= h) * deltaX + (deltaZ *= h) * deltaZ);
            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ + deltaY * deltaY);

            float yaw = this.mob.getYRot();
            float yawOrthogonal = Mth.wrapDegrees(this.mob.getYRot() + 90.0f);
            float newYaw = Mth.approachDegrees(yawOrthogonal, Mth.wrapDegrees((float) Mth.atan2(deltaZ, deltaX) * 57.295776f), 4.0f) - 90.0f;

            this.mob.setYRot(newYaw);
            this.mob.yBodyRot = newYaw;

            // Added distance, so now, can't modify yer yaw if ye try homing in on your target!
            boolean shouldSlowDownToAdjustYaw =  (distance > 8) && Mth.degreesDifferenceAbs(yaw, newYaw) < 3.0f;
            this.targetSpeed = shouldSlowDownToAdjustYaw ?
                    Mth.approach(this.targetSpeed, 1.8f, 0.005f * (1.8f / this.targetSpeed)) :
                    Mth.approach(this.targetSpeed, 0.6f, 0.025f);

            float pitch = (float)(-(Mth.atan2(-deltaY, distanceHorizontal) * 57.2957763671875));
            this.mob.setXRot(pitch);

            float angle = newYaw + 90.0f;
            double velocityX = (double)(this.targetSpeed * Mth.cos(angle * ((float)Math.PI / 180))) * Math.abs(deltaX / distance);
            double velocityZ = (double)(this.targetSpeed * Mth.sin(angle * ((float)Math.PI / 180))) * Math.abs(deltaZ / distance);
            double velocityY = (double)(this.targetSpeed * Mth.sin(this.mob.getXRot() * ((float)Math.PI / 180))) * Math.abs(deltaY / distance);

            Vec3 velocity = this.mob.getDeltaMovement();
            this.mob.setDeltaMovement(velocity.add(new Vec3(velocityX, velocityY, velocityZ).subtract(velocity).scale(0.2)));
        }
    }
}