package frootloops.versus.mixin.mobs;

import frootloops.versus.mod.mobs.MobSpawning;
import net.minecraft.entity.SpawnGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SpawnGroup.class)
public abstract class MobSpawnGroupsMixin {

    @Shadow
    private final int capacity;

    protected MobSpawnGroupsMixin(int capacity) {
        this.capacity = capacity;
    }

    @ModifyVariable(method = "<init>",at = @At("HEAD"), ordinal = 0)
    private static int modifyMobCap(int capacity) {
        boolean isMonster = capacity == 70;
        if(isMonster) return MobSpawning.MOB_CAP_MONSTERS;

        boolean isAmbient = capacity == 15;
        if(isAmbient) return MobSpawning.MOB_CAP_AMBIENT;

        return capacity;
    }
}
