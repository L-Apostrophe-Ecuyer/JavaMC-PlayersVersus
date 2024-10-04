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
                CustomBlocks.WHITE_CLOVERS,

                CustomBlocks.HARMFUL_BILE,
                CustomBlocks.HEALTHY_BILE,
                CustomBlocks.REGENERATION_BILE,
                CustomBlocks.WITHERING_BILE,
                CustomBlocks.MINING_SPEED_BILE,
                CustomBlocks.MINING_FATIGUE_BILE,
                CustomBlocks.TOUGHNESS_BILE,
                CustomBlocks.VULNERABILITY_BILE,
                CustomBlocks.VISION_BILE,
                CustomBlocks.DARKNESS_BILE,
                CustomBlocks.LEAPING_BILE,
                CustomBlocks.SLOW_FALL_BILE,
                CustomBlocks.SPEED_BILE,
                CustomBlocks.SLOWNESS_BILE,
                CustomBlocks.BREATH_BILE,
                CustomBlocks.BUOYANCY_BILE,
                CustomBlocks.LARGENESS_BILE,
                CustomBlocks.SMALLNESS_BILE,
                CustomBlocks.INVISIBILITY_BILE,
                CustomBlocks.GLOWING_BILE,
                CustomBlocks.WEAKNESS_BILE,
                CustomBlocks.STRENGTH_BILE,
                CustomBlocks.WIND_BILE,
                CustomBlocks.FIRE_BILE,
                CustomBlocks.OOZE_BILE,
                CustomBlocks.INFESTATION_BILE,
                CustomBlocks.POISON_BILE,
                CustomBlocks.WEAVING_BILE,
                CustomBlocks.LUCK_BILE,
                CustomBlocks.UNLUCK_BILE
        );

        ColorProviderRegistry.BLOCK.register((state, view, pos, tintIndex) -> {
            if (view == null || pos == null) return GrassColors.getDefaultColor();
            else return BiomeColors.getGrassColor(view, pos);
        }, CustomBlocks.WHEAT_GRASS, CustomBlocks.CLOVERS, CustomBlocks.WHITE_CLOVERS);
    }

}
