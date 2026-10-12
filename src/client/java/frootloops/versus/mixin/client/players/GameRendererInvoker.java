package frootloops.versus.mixin.client.players;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The width vanilla draws the block outline with, for the placement preview (PlacementPreview). */
@Mixin(GameRenderer.class)
public interface GameRendererInvoker {
    @Invoker("gameRenderState")
    GameRenderState playersVersus$gameRenderState();
}
