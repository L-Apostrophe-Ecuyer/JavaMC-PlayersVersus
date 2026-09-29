package frootloops.versus.mod.mobs.hostile.nether;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class WildfireEntityRenderer extends MobRenderer<WildfireEntity, LivingEntityRenderState, WildfireEntityModel> {

    private final ResourceLocation TEXTURE_LIT = ResourceLocation.parse(VersusMod.MOD_ID  + ":textures/entity/wildfire_lit.png");
    private final ResourceLocation TEXTURE_UNLIT = ResourceLocation.parse(VersusMod.MOD_ID  + ":textures/entity/wildfire_unlit.png");

    public WildfireEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new WildfireEntityModel(context.bakeLayer(ModelLayers.BLAZE)), 0.5F);
    }

    protected int getBlockLight(WildfireEntity entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(LivingEntityRenderState state) {return state.displayFireAnimation? TEXTURE_LIT : TEXTURE_UNLIT;}

    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }
}

