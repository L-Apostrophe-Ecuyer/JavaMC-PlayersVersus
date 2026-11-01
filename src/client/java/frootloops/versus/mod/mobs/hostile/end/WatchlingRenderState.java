package frootloops.versus.mod.mobs.hostile.end;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;

/** A watchling as it's drawn this frame: an enderman's, and how open its maw is and how far its head is cocked. */
@Environment(EnvType.CLIENT)
public class WatchlingRenderState extends EndermanRenderState {
    /** 0 shut, 1 open all the way (it fights). */
    public float jawOpen;
    /** 0 straight, 1 cocked all the way (it haunts). */
    public float headTilt;
}
