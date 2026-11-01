package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;

/**
 * Where paleness holds: below y 0 (the deepslate), and in the pale biomes (the pale garden, the pale grotto and hollow,
 * the deep caves: tag {@code players-versus:pale}). Mushrooms there grow pale, pale mobs spawn there at any hour (above
 * it, only at midnight under a new moon), and whatever turns undead there rises pale.
 */
public final class PaleLine {
    /** The depth below which everything is pale. */
    public static final int Y = 0;
    public static final TagKey<Biome> PALE_BIOMES = TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "pale"));

    private PaleLine() {}

    /** Whether paleness holds there. */
    public static boolean isPale(LevelReader level, BlockPos pos) {
        return pos.getY() < Y || isPaleBiome(level, pos);
    }

    public static boolean isPaleBiome(LevelReader level, BlockPos pos) {
        return level.getBiome(pos).is(PALE_BIOMES);
    }
}
