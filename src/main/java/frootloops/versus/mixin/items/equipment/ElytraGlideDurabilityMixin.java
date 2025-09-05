package frootloops.versus.mixin.items.equipment;

import frootloops.versus.mod.Combat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;


@Mixin(LivingEntity.class)
public abstract class ElytraGlideDurabilityMixin extends Entity {
    public ElytraGlideDurabilityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @ModifyConstant(method = "tickGliding", constant = @Constant(intValue = 10))
    private int lessDurabilityLossWhileGliding(int numTicksToUntilDurabilityLoss) {
        if(this.getVelocity().lengthSquared() < 2.0) return 360;
        return 80;
    }
}
