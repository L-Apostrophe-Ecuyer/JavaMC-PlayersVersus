package frootloops.mobs.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.brain.task.SonicBoomTask;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(WardenEntity.class)
public class WardenSonicBoomMixin extends HostileEntity {

    private static final double OLD_RANGE_HORIZONTAL = 15.0d,
            OLD_RANGE_VERTICAL = 20.0d,
            NEW_RANGE_HORIZONTAL = 6.0d,
            NEW_RANGE_VERTICAL = 12.0d,
            NEW_RANGE_HORIZONTAL_SQUARED = NEW_RANGE_HORIZONTAL * NEW_RANGE_HORIZONTAL,
            NEW_RANGE_VERTICAL_SQUARED = NEW_RANGE_VERTICAL * NEW_RANGE_VERTICAL;

    protected WardenSonicBoomMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public boolean isInRange(Entity entity, double horizontalRadius, double verticalRadius) {
        double d = entity.getX() - this.getX();
        double e = entity.getY() - this.getY();
        double f = entity.getZ() - this.getZ();
        if(horizontalRadius == OLD_RANGE_HORIZONTAL && verticalRadius == OLD_RANGE_VERTICAL)
            return MathHelper.squaredHypot(d, f) < NEW_RANGE_HORIZONTAL_SQUARED && MathHelper.square(e) < NEW_RANGE_VERTICAL_SQUARED;
        else
            return MathHelper.squaredHypot(d, f) < MathHelper.square(horizontalRadius) && MathHelper.square(e) < MathHelper.square(verticalRadius);
    }
}
