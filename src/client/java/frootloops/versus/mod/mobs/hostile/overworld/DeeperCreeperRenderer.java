package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class DeeperCreeperRenderer extends CreeperRenderer {

    public DeeperCreeperRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    private final Identifier TEXTURE = Identifier.parse(VersusMod.MOD_ID  + ":textures/entity/deeper_creeper.png");

    @Override
    public Identifier getTextureLocation(CreeperRenderState creeperEntityRenderState) {
        return TEXTURE;
    }
}
