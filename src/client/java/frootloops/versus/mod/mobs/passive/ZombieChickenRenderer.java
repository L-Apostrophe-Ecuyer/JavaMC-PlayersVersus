package frootloops.versus.mod.mobs.passive;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.resources.Identifier;

/** The zombie chicken: the temperate chicken's model in its own skin (tools/undead-animals.py). It's never a chick. */
@Environment(EnvType.CLIENT)
public class ZombieChickenRenderer extends ChickenRenderer {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "textures/entity/zombie_chicken.png");

    public ZombieChickenRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public Identifier getTextureLocation(ChickenRenderState state) {
        return TEXTURE;
    }
}
