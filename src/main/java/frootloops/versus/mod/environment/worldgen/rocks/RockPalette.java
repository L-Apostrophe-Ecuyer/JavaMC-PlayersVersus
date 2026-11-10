package frootloops.versus.mod.environment.worldgen.rocks;

import java.util.List;
import java.util.Map;

/**
 * The blocks of a rock, by their ids: one for each {@link RockMaterial}, the slab that tops each of them where the rock
 * has slabs (none: that block stays whole), the plants that grow on its grass, and its vine.
 */
public record RockPalette(Map<RockMaterial, String> blocks, Map<String, String> slabs, List<String> plants, String vine) {

    /** Gray stone: tuff in its cracks and shadows, andesite on what catches the light, stone for everything else. */
    public static final RockPalette GRAY = new RockPalette(Map.of(
            RockMaterial.SHADE, "minecraft:tuff", RockMaterial.BASE, "minecraft:stone", RockMaterial.LIGHT, "minecraft:andesite",
            RockMaterial.SOIL, "minecraft:dirt", RockMaterial.GRASS, "minecraft:grass_block", RockMaterial.MOSS, "minecraft:moss_block"),
            Map.of("minecraft:stone", "minecraft:stone_slab", "minecraft:andesite", "minecraft:andesite_slab", "minecraft:tuff", "minecraft:tuff_slab"),
            List.of("minecraft:short_grass", "minecraft:short_grass", "minecraft:short_grass", "minecraft:fern", "minecraft:bush"),
            "minecraft:vine");

    public String block(RockMaterial material) {
        return this.blocks.get(material);
    }

    /** The slab of a material's block, or null where it has none. */
    public String slab(RockMaterial material) {
        return this.slabs.get(this.blocks.get(material));
    }
}
