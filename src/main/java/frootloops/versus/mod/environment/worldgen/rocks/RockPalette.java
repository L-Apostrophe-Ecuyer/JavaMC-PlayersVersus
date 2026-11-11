package frootloops.versus.mod.environment.worldgen.rocks;

import java.util.List;
import java.util.Map;

/**
 * The blocks of a rock, by their ids: one for each {@link RockMaterial}, the slab that tops each of them where the rock
 * has slabs (none: that block stays whole), the plants that grow on its grass, and its vine.
 */
public record RockPalette(Map<RockMaterial, String> blocks, List<String> plants) {

    /**
     * Gray stone: tuff in its shade (undersides, corners, cracks), stone for most of it, andesite in the light, clay in
     * the brightest; dirt, grass and moss on its tops, plants on the grass.
     */
    public static final RockPalette GRAY = new RockPalette(Map.of(
            RockMaterial.SHADE, "minecraft:tuff", RockMaterial.BASE, "minecraft:stone", RockMaterial.LIGHT, "minecraft:andesite",
            RockMaterial.BRIGHT, "minecraft:clay", RockMaterial.SOIL, "minecraft:dirt", RockMaterial.GRASS, "minecraft:grass_block",
            RockMaterial.MOSS, "minecraft:moss_block"),
            List.of("minecraft:short_grass", "minecraft:short_grass", "minecraft:short_grass", "minecraft:fern", "minecraft:bush"));

    public String block(RockMaterial material) {
        return this.blocks.get(material);
    }
}
