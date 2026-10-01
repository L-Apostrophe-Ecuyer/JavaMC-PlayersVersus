package frootloops.versus.mixin.mobs;

import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MobCategory.class)
public abstract class MobSpawnGroupsMixin {

    @Shadow
    private final int max;

    protected MobSpawnGroupsMixin(int capacity) {
        this.max = capacity;
    }

    @Overwrite
    public int getMaxInstancesPerChunk() {
        return Math.min(this.max, 48);
    }

}
