package frootloops.versus.mod.mobs.hostile.nether;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class WildfireEntityRenderer extends MobEntityRenderer<WildfireEntity, LivingEntityRenderState, WildfireEntityModel> {

    private final Identifier TEXTURE_LIT = Identifier.of(VersusMod.MOD_ID  + ":textures/entity/wildfire_lit.png");
    private final Identifier TEXTURE_UNLIT = Identifier.of(VersusMod.MOD_ID  + ":textures/entity/wildfire_unlit.png");

    public WildfireEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new WildfireEntityModel(context.getPart(EntityModelLayers.BLAZE)), 0.5F);
    }

    protected int getBlockLight(WildfireEntity entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public Identifier getTexture(LivingEntityRenderState state) {return state.onFire? TEXTURE_LIT : TEXTURE_UNLIT;}

    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }
}

