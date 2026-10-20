package frootloops.versus.mixin.client.environment.visuals;

import frootloops.versus.mod.environment.ToppleRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Carries how far a felled tree's block is turned over from its renderer's extraction to its drawing (ClientTopples). */
@Environment(EnvType.CLIENT)
@Mixin(FallingBlockRenderState.class)
public abstract class FallingBlockRenderStateToppleMixin implements ToppleRenderState {

    @Unique
    private float playersVersus$turn;
    @Unique
    private @Nullable Direction playersVersus$turnWay;

    @Override
    public float playersVersus$turn() {
        return this.playersVersus$turn;
    }

    @Override
    public @Nullable Direction playersVersus$turnWay() {
        return this.playersVersus$turnWay;
    }

    @Override
    public void playersVersus$setTurn(float turn, @Nullable Direction way) {
        this.playersVersus$turn = turn;
        this.playersVersus$turnWay = way;
    }
}
