package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.block.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.BlockItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;


public class CustomBlocks {

    public static final Block SMOLDERING_TORCH = new TorchBlock(AbstractBlock.Settings.copy(Blocks.TORCH).luminance(state -> 8), ParticleTypes.SMOKE);
    public static final Block SMOLDERING_WALL_TORCH = new WallTorchBlock(AbstractBlock.Settings.copy(Blocks.WALL_TORCH).luminance(state -> 8).dropsLike(SMOLDERING_TORCH), ParticleTypes.SMOKE);

    private static void registerBlocks(String name, Block block) {
        Registry.register(Registries.BLOCK, new Identifier(VersusMod.MOD_ID, name), block);
        Registry.register(Registries.ITEM, new Identifier(VersusMod.MOD_ID, name), new BlockItem(block, new FabricItemSettings()));
    }

    public static void onInitialize() {
        registerBlocks("smoldering_torch", SMOLDERING_TORCH);
        registerBlocks("smoldering_wall_torch", SMOLDERING_WALL_TORCH);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), SMOLDERING_TORCH, SMOLDERING_WALL_TORCH);
    }

}
