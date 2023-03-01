package frootloops.versus.mixin.environment;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TntEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TntEntity.class)
public abstract class TntMixin extends Entity {
    public TntMixin(EntityType<?> type, World world) {
        super(type, world);
    }


    @Overwrite
    private void explode() {
        this.world.createExplosion(this, this.getX(), this.getBodyY(0.0625), this.getZ(), 6.0F, World.ExplosionSourceType.TNT);
    }
}
