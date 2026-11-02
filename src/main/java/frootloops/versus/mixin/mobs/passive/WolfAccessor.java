package frootloops.versus.mixin.mobs.passive;

import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** A wolf's collar colour, set: what a wolf turning undead hands its undead self (Undeath). */
@Mixin(Wolf.class)
public interface WolfAccessor {

    @Invoker("setCollarColor")
    void playersVersus$setCollarColor(DyeColor color);

}
