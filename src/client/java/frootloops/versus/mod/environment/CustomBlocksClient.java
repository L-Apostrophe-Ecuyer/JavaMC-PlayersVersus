package frootloops.versus.mod.environment;


import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.minecraft.client.color.block.BlockTintSources;

@Environment(EnvType.CLIENT)
public class CustomBlocksClient {

    public static void onInitialize() {
        // The clovers take the grass tint, as they did (26.3 models say which layer they draw in, from their textures' transparency)
        BlockColorRegistry.register(List.of(BlockTintSources.grass()), CustomBlocks.CLOVERS);
    }

}
