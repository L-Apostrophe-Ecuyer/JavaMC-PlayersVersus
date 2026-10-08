package frootloops.versus.mod.mobs.melee;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Implemented on every {@code LivingEntityRenderState} by a mixin: how the mob's melee swing looks this frame. */
@Environment(EnvType.CLIENT)
public interface MeleeRenderState {
    MeleeState.Look playersVersus$meleeLook();

    void playersVersus$setMeleeLook(MeleeState.Look look);
}
