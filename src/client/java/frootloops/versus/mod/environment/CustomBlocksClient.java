package frootloops.versus.mod.environment;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;

@Environment(EnvType.CLIENT)
public class CustomBlocksClient {

    public static void onInitialize() {
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), CustomBlocks.SMOLDERING_TORCH, CustomBlocks.SMOLDERING_WALL_TORCH, CustomBlocks.EXTINGUISHED_TORCH, CustomBlocks.EXTINGUISHED_WALL_TORCH, CustomBlocks.CORRUPTED_WART_PLANT);
    }

}
