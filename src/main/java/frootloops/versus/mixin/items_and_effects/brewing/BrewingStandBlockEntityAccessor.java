
package frootloops.versus.mixin.items_and_effects.brewing;


import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BrewingStandBlockEntity.class)
public interface  BrewingStandBlockEntityAccessor {

    @Accessor int getBrewTime();

    @Accessor int getFuel();

    @Accessor("fuel")
    public void setFuel(int fuel);
}

