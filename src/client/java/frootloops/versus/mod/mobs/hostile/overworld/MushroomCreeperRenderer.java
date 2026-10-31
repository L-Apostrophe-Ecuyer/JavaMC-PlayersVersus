package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.resources.Identifier;

/** A mushroom creeper: a creeper overgrown with brown mushroom cap and stem. */
@Environment(EnvType.CLIENT)
public class MushroomCreeperRenderer extends CreeperRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/mushroom_creeper.png");

    public MushroomCreeperRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(CreeperRenderState state) {
        return TEXTURE;
    }
}
