package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** A mushroom spider: a spider overgrown with brown mushroom cap and stem, its eyes a spider's. */
@Environment(EnvType.CLIENT)
public class MushroomSpiderRenderer extends SpiderRenderer<MushroomSpiderEntity> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/mushroom_spider.png");

    public MushroomSpiderRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }
}
