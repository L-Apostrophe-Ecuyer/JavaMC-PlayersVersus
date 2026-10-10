package frootloops.versus.mixin.client.mobs.hostile;

import frootloops.versus.mod.mobs.melee.MeleeRenderState;
import frootloops.versus.mod.mobs.melee.MeleeState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries how a mob's melee swing looks this frame from the renderer to its model and layers. */
@Environment(EnvType.CLIENT)
@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMeleeMixin implements MeleeRenderState {

    @Unique
    private MeleeState.Look playersVersus$meleeLook = MeleeState.Look.NONE;

    @Override
    public MeleeState.Look playersVersus$meleeLook() {
        return this.playersVersus$meleeLook;
    }

    @Override
    public void playersVersus$setMeleeLook(MeleeState.Look look) {
        this.playersVersus$meleeLook = look;
    }
}
