package frootloops.versus.mixin.hostile_mobs.overworld;

import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PhantomEntity.class)
public interface PhantomAccessor {

    @Accessor("targetPosition")
    Vec3d getTargetPosition();

}
