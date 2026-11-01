package frootloops.versus.mod.mobs.hostile.end;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** How well the camera sees an entity this frame (Gaze), carried on its render state: 1 for all but the warped. */
@Environment(EnvType.CLIENT)
public interface GazeRenderState {
    float playersVersus$seen();

    void playersVersus$setSeen(float seen);
}
