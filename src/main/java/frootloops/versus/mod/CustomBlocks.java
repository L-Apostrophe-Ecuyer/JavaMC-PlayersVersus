package frootloops.versus.mod;

import frootloops.versus.VersusMod;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.block.*;
import net.minecraft.item.BlockItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;


public class CustomBlocks {

    public static final Block SMOLDERING_TORCH = new TorchBlock(AbstractBlock.Settings.of(Material.DECORATION).noCollision().breakInstantly().luminance(state -> 6).sounds(BlockSoundGroup.WOOD).dropsLike(Blocks.TORCH), ParticleTypes.FLAME);
    public static final Block SMOLDERING_WALL_TORCH = new WallTorchBlock(AbstractBlock.Settings.of(Material.DECORATION).noCollision().breakInstantly().luminance(state -> 6).sounds(BlockSoundGroup.WOOD).dropsLike(SMOLDERING_TORCH), ParticleTypes.FLAME);

    private static void registerBlocks(String name, Block block) {
        Registry.register(Registries.BLOCK, new Identifier(VersusMod.MOD_ID, name), block);
        Registry.register(Registries.ITEM, new Identifier(VersusMod.MOD_ID, name), new BlockItem(block, new FabricItemSettings()));
    }

    public static void init() {
        registerBlocks("smoldering_torch", SMOLDERING_TORCH);
        registerBlocks("smoldering_wall_torch", SMOLDERING_WALL_TORCH);
    }

}
