package frootloops.versus.mixin.items_and_effects.blight;

import frootloops.versus.mod.items_and_effects.blight.Blight;
import frootloops.versus.mod.items_and_effects.blight.Undeath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A potion of Healing pushes Blight back: the blighted hearts it heals (two for Healing I, four for II) are purged, as far
 * as its strength reaches the one it's on (a splash's edge purges less). It only does so where it heals: on the living,
 * not on the undead it harms.
 */
// Package-private in the game, so named
@Mixin(targets = "net.minecraft.world.effect.HealOrHarmMobEffect")
public abstract class HealOrHarmMobEffectMixin {
    @Shadow @Final private boolean isHarm;

    @Inject(method = "applyInstantaneousEffect", at = @At("HEAD"))
    private void playersVersus$healingPurgesBlight(ServerLevel level, @Nullable Entity source, @Nullable Entity direct, LivingEntity target, int amplifier, double scale, CallbackInfo info) {
        // As in the effect itself: it heals when what it does and who it's done to agree
        if (this.isHarm == target.isInvertedHealAndHarm()) Undeath.purge(target, Blight.healingPurge(amplifier, scale));
    }
}
