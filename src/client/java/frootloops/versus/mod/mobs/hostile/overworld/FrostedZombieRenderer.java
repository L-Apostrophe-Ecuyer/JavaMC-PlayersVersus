package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class FrostedZombieRenderer extends ZombieRenderer {


    private final ResourceLocation TEXTURE = ResourceLocation.parse(VersusMod.MOD_ID  + ":textures/entity/frosted_zombie.png");

    public FrostedZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ZombieRenderState zombieEntityRenderState) {
        return TEXTURE;
    }
}
