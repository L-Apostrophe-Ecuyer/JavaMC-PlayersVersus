package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.resources.Identifier;

/** A slime in ice: the slime's model and outer layer (which draws with the renderer's texture), in blue. */
@Environment(EnvType.CLIENT)
public class IceCubeRenderer extends SlimeRenderer {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/ice_cube.png");

    public IceCubeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(SlimeRenderState state) {
        return TEXTURE;
    }
}
