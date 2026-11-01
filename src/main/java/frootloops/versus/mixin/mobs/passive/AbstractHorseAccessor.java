package frootloops.versus.mixin.mobs.passive;

import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A horse's owner as it's kept (the owner may be away): what a horse turning undead hands its undead self (Undeath). */
@Mixin(AbstractHorse.class)
public interface AbstractHorseAccessor {

    @Accessor("owner")
    void setOwnerKept(EntityReference<LivingEntity> owner);

}
