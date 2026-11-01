package frootloops.versus.mod.mobs.hostile.overworld;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.resources.Identifier;

/** A zombie's render state, with the skin of the player it was. */
@Environment(EnvType.CLIENT)
public class PlayerZombieRenderState extends ZombieRenderState {
    /** The skin's texture: a classic player skin, which a zombie's model wears as it is. */
    public Identifier skin = PlayerZombieRenderer.ZOMBIE_TEXTURE;
}
