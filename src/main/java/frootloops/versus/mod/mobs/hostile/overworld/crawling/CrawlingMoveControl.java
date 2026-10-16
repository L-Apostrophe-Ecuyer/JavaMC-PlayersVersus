package frootloops.versus.mod.mobs.hostile.overworld.crawling;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Moves a crawler along its path ({@link SurfaceNavigation}): toward ground it walks as any mob does, but toward a node
 * up a wall or on a ceiling, or while it clings off the ground, it crawls straight at the node at about its walking
 * speed, held to the surfaces it touches; waiting on a wall or a ceiling, it holds on. A hit knocks it off for a moment,
 * and in water it swims as usual.
 */
public class CrawlingMoveControl extends MoveControl {
    /** Crawling speed, compared with walking. */
    static final double CRAWL_FACTOR = 0.9;
    /** The pull into the walls and ceilings the crawler touches, blocks per tick. */
    static final double GRIP_PULL = 0.04;

    public CrawlingMoveControl(Mob mob) {
        super(mob);
    }

    @Override
    public void tick() {
        Mob mob = this.mob;
        int touching = mob instanceof SurfaceCrawler crawler ? crawler.playersVersus$touching() : 0;
        if (mob.hurtTime > 0 || mob.isInLiquid() || mob.isPassenger()) {
            super.tick();
            return;
        }
        boolean clinging = !mob.onGround() && SurfaceGrip.clingsOffTheGround(touching);
        // Over the lip of a wall, off the ground with the node just above, it pulls itself up rather than drop back.
        boolean pullingUp = !mob.onGround() && this.wantedY > mob.getY() + 0.05;
        if (this.operation == Operation.MOVE_TO && (clinging || pullingUp || !this.groundBelowWanted())) {
            this.crawl(mob, touching);
            return;
        }
        if (this.operation == Operation.WAIT && clinging) {
            mob.setDeltaMovement(SurfaceGrip.grip(touching, GRIP_PULL));
            mob.setZza(0.0F);
            mob.setXxa(0.0F);
            return;
        }
        super.tick();
    }

    private void crawl(Mob mob, int touching) {
        this.operation = Operation.WAIT;
        double dx = this.wantedX - mob.getX(), dy = this.wantedY - mob.getY(), dz = this.wantedZ - mob.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double speed = SurfaceGrip.walkingSpeed(mob, this.speedModifier) * CRAWL_FACTOR;
        double scale = distance > 1.0E-4 ? Math.min(speed, distance) / distance : 0.0;
        mob.setDeltaMovement(new Vec3(dx * scale, dy * scale, dz * scale).add(SurfaceGrip.grip(touching, GRIP_PULL)));
        // The speed tells the navigation's stuck check how far the crawler should get; no walking input on top.
        mob.setSpeed((float) (this.speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
        mob.setZza(0.0F);
        mob.setXxa(0.0F);
        if (dx * dx + dz * dz > 1.0E-4) {
            float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
            mob.setYRot(this.rotlerp(mob.getYRot(), yaw, 90.0F));
        }
    }

    /** Whether there's ground right under the wanted position, so the crawler can walk there. */
    private boolean groundBelowWanted() {
        AABB box = this.mob.getDimensions(this.mob.getPose()).makeBoundingBox(this.wantedX, this.wantedY, this.wantedZ);
        return !this.mob.level().noCollision(this.mob, SurfaceGrip.beyond(box, Direction.DOWN, SurfaceGrip.FLOOR_REACH));
    }
}
