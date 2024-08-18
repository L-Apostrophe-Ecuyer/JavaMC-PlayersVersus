package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.CreeperEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.CreeperEntityRenderState;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class DeeperCreeperRenderer extends CreeperEntityRenderer {

    public DeeperCreeperRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    private final Identifier TEXTURE = Identifier.of(VersusMod.MOD_ID  + ":textures/entity/deeper_creeper.png");

    @Override
    public Identifier getTexture(CreeperEntityRenderState creeperEntityRenderState) {
        return TEXTURE;
    }
}
