package frootloops.versus.mixin.environment.blocks;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(PrimedTnt.class)
public abstract class TntMixin extends Entity {
    public TntMixin(EntityType<?> type, Level world) {
        super(type, world);
    }


    @Overwrite
    private void explode() { // Triple the power!
        this.level().explode(this, this.getX(), this.getY(0.0625), this.getZ(), 6.0F, Level.ExplosionInteraction.TNT);
    }
}
