package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class WitheredZombieRenderer extends ZombieRenderer {

    private final Identifier TEXTURE = Identifier.parse(VersusMod.MOD_ID  + ":textures/entity/withered_zombie.png");

    public WitheredZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(ZombieRenderState zombieEntityRenderState) {
        return TEXTURE;
    }
}