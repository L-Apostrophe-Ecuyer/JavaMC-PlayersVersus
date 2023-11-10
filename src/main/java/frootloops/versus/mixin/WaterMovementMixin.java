package frootloops.versus.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class WaterMovementMixin extends Entity {
    public WaterMovementMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyVariable(method = "travel", at = @At("STORE"), ordinal = 2)
    private float fasterWaterMovement(float h) {
        return this.isSprinting() ? h + 0.1f : h + 0.5f;
    }
}
