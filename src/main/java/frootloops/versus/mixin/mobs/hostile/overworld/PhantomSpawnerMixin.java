package frootloops.versus.mixin.mobs.hostile.overworld;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * No insomnia: phantoms don't come for players who haven't slept. They are the new moon's, spawning at its midnight
 * like any other monster, in the overworld's biomes but the mushroom islands' (MobSpawning, PhantomMixin).
 */
@Mixin(PhantomSpawner.class)
public abstract class PhantomSpawnerMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void playersVersus$noInsomnia(ServerLevel level, boolean spawnEnemies, CallbackInfo info) {
        info.cancel();
    }
}
