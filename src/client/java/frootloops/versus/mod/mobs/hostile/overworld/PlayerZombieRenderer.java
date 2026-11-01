package frootloops.versus.mod.mobs.hostile.overworld;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.zombie.Zombie;

/**
 * A player zombie: a zombie in the skin of the player it was, drawn greener and duller than they were (the body's tint:
 * LivingEntityRendererGazeMixin, which reads {@link #TINT}). Worn armor and held items draw as on any zombie.
 */
@Environment(EnvType.CLIENT)
public class PlayerZombieRenderer extends ZombieRenderer {
    public static final Identifier ZOMBIE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png");
    /** Multiplied into the skin: its colours gone grey-green. */
    public static final int TINT = 0xFFA8C69A;

    public PlayerZombieRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ZombieRenderState createRenderState() {
        return new PlayerZombieRenderState();
    }

    @Override
    public void extractRenderState(Zombie entity, ZombieRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (entity instanceof PlayerZombieEntity zombie && state instanceof PlayerZombieRenderState own) {
            own.skin = Minecraft.getInstance().playerSkinRenderCache().getOrDefault(zombie.getProfile()).playerSkin().body().texturePath();
        }
    }

    @Override
    public Identifier getTextureLocation(ZombieRenderState state) {
        return state instanceof PlayerZombieRenderState own ? own.skin : ZOMBIE_TEXTURE;
    }
}
