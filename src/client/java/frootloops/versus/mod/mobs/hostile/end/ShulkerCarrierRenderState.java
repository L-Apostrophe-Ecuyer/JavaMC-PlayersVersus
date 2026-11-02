package frootloops.versus.mod.mobs.hostile.end;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Whether an enderman carries a shulker this frame (EndermanShulkerCarrier), carried on its render state for its arms' pose. */
@Environment(EnvType.CLIENT)
public interface ShulkerCarrierRenderState {
    boolean playersVersus$carriesShulker();

    void playersVersus$setCarriesShulker(boolean carries);
}
