package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.blocks.SmolderingTorchBlock;
import frootloops.versus.mod.environment.blocks.SmolderingWallTorchBlock;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.block.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.block.enums.Instrument;


public class CustomBlocks {

    public static final Block SMOLDERING_TORCH = new SmolderingTorchBlock(AbstractBlock.Settings.copy(Blocks.TORCH).luminance(state -> 12), ParticleTypes.SMALL_FLAME);
    public static final Block SMOLDERING_WALL_TORCH = new SmolderingWallTorchBlock(AbstractBlock.Settings.copy(Blocks.WALL_TORCH).luminance(state -> 12).dropsLike(SMOLDERING_TORCH), ParticleTypes.SMALL_FLAME);

    public static final Block EXTINGUISHED_TORCH = new TorchBlock(AbstractBlock.Settings.copy(Blocks.TORCH).luminance(state -> 6), ParticleTypes.SMOKE);
    public static final Block EXTINGUISHED_WALL_TORCH = new WallTorchBlock(AbstractBlock.Settings.copy(Blocks.WALL_TORCH).dropsLike(EXTINGUISHED_TORCH).luminance(state -> 6).dropsLike(EXTINGUISHED_TORCH), ParticleTypes.SMOKE);

    public static final Block GRANITE_BRICKS = new SlabBlock(AbstractBlock.Settings.copy(Blocks.GRANITE));
    public static final Block GRANITE_BRICK_SLAB = new SlabBlock(AbstractBlock.Settings.copy(GRANITE_BRICKS));
    public static final Block GRANITE_BRICK_STAIRS = new StairsBlock(GRANITE_BRICKS.getDefaultState(), AbstractBlock.Settings.copy(GRANITE_BRICKS));

    private static void registerBlock(String name, Block block) {
        Registry.register(Registries.BLOCK, new Identifier(VersusMod.MOD_ID, name), block);
    }

    public static void onInitialize() {
        registerBlock("smoldering_torch", SMOLDERING_TORCH);
        registerBlock("smoldering_wall_torch", SMOLDERING_WALL_TORCH);
        registerBlock("extinguished_torch", EXTINGUISHED_TORCH);
        registerBlock("extinguished_wall_torch", EXTINGUISHED_WALL_TORCH);
        registerBlock("granite_bricks", GRANITE_BRICKS);
        registerBlock("granite_brick_slab", GRANITE_BRICK_SLAB);
        registerBlock("granite_brick_stairs", GRANITE_BRICK_STAIRS);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), SMOLDERING_TORCH, SMOLDERING_WALL_TORCH, EXTINGUISHED_TORCH, EXTINGUISHED_WALL_TORCH);
    }

}
