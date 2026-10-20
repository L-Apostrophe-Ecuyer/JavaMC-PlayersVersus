package frootloops.versus.mod.environment;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/** Implemented on every {@code FallingBlockRenderState} by a mixin: how far a felled tree's block is turned over this frame, and which way. */
@Environment(EnvType.CLIENT)
public interface ToppleRenderState {
    float playersVersus$turn();

    @Nullable Direction playersVersus$turnWay();

    void playersVersus$setTurn(float turn, @Nullable Direction way);
}
