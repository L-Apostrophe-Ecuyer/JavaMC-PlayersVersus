package frootloops.versus.mixin.mobs;

import net.minecraft.entity.SpawnGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SpawnGroup.class)
public abstract class MobSpawnGroupsMixin {

    @Shadow
    private final int capacity;

    protected MobSpawnGroupsMixin(int capacity) {
        this.capacity = capacity;
    }

    @Overwrite
    public int getCapacity() {
        return Math.min(this.capacity, 48);
    }


}
