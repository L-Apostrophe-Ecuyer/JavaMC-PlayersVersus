package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.blocks.SmolderingTorchBlock;
import frootloops.versus.mod.environment.blocks.SmolderingWallTorchBlock;
import frootloops.versus.mod.items.Items;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;


public class CustomBlocks {

    public static final Block SMOLDERING_TORCH = new SmolderingTorchBlock(ParticleTypes.SMALL_FLAME, AbstractBlock.Settings.create().noCollision().breakInstantly().luminance((state) -> 12).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY));
    public static final Block SMOLDERING_WALL_TORCH = new SmolderingWallTorchBlock(ParticleTypes.SMALL_FLAME, AbstractBlock.Settings.create().noCollision().breakInstantly().luminance((state) -> 12).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY));
    public static final Block EXTINGUISHED_TORCH = new TorchBlock(ParticleTypes.SMOKE, AbstractBlock.Settings.create().noCollision().breakInstantly().luminance((state) -> 6).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY));
    public static final Block EXTINGUISHED_WALL_TORCH = new WallTorchBlock(ParticleTypes.SMOKE, AbstractBlock.Settings.create().noCollision().breakInstantly().luminance((state) -> 6).sounds(BlockSoundGroup.WOOD).pistonBehavior(PistonBehavior.DESTROY));

    private static void registerBlock(String name, Block block) {
        Registry.register(Registries.BLOCK, new Identifier(VersusMod.MOD_ID, name), block);
    }

    public static void onInitialize() {
        registerBlock("smoldering_torch", SMOLDERING_TORCH);
        registerBlock("smoldering_wall_torch", SMOLDERING_WALL_TORCH);
        registerBlock("extinguished_torch", EXTINGUISHED_TORCH);
        registerBlock("extinguished_wall_torch", EXTINGUISHED_WALL_TORCH);
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(), SMOLDERING_TORCH, SMOLDERING_WALL_TORCH, EXTINGUISHED_TORCH, EXTINGUISHED_WALL_TORCH);
    }

}
