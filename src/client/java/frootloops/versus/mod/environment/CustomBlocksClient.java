package frootloops.versus.mod.environment;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.color.world.BiomeColors;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.world.biome.GrassColors;

@Environment(EnvType.CLIENT)
public class CustomBlocksClient {

    public static void onInitialize() {
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(),
                CustomBlocks.SMOLDERING_TORCH,
                CustomBlocks.SMOLDERING_WALL_TORCH,
                CustomBlocks.EXTINGUISHED_TORCH,
                CustomBlocks.EXTINGUISHED_WALL_TORCH,
                CustomBlocks.CORRUPTED_WART_PLANT,
                CustomBlocks.WITHERED_WART_PLANT,
                CustomBlocks.CLOVERS,
                CustomBlocks.WILD_WHEAT,
                CustomBlocks.WHEAT_GRASS,
                CustomBlocks.WHITE_CLOVERS
        );

        ColorProviderRegistry.BLOCK.register((state, view, pos, tintIndex) -> {
            if (view == null || pos == null) return GrassColors.getDefaultColor();
            else return BiomeColors.getGrassColor(view, pos);
        }, CustomBlocks.WHEAT_GRASS, CustomBlocks.CLOVERS, CustomBlocks.WHITE_CLOVERS);
    }

}
