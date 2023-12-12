package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.VersusMod;
import net.minecraft.client.render.entity.CreeperEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.util.Identifier;

public class DeeperCreeperRenderer extends CreeperEntityRenderer {

    public DeeperCreeperRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(CreeperEntity creeperEntity) {
        return new Identifier(VersusMod.MOD_ID  + ":textures/entity/deeper_creeper.png");
    }
}
