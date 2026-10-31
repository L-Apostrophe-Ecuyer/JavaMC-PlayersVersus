package frootloops.versus.mod.items_and_effects.recipes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The recipe book's collections: which items made by recipes are gathered in one, named by their ids. The first that
 * applies:
 * <ul>
 * <li>Tools by material ("iron_tools"), armor too ("iron_armor").</li>
 * <li>Dyed things by kind ("dyed:wool"), where every one of the sixteen colours of the kind is an item; the undyed one,
 * if there is one (a candle, terracotta), goes with them.</li>
 * <li>Blocks by material family, named by what's left of their name without the words for their shape and their
 * finish: oak planks, stairs, doors and boats in "oak"; cobbled, polished, brick and tile deepslate in "deepslate";
 * every cut, waxed and weathered copper block, grate, door and bulb in "copper". A family's plain block (andesite) goes
 * in its family.</li>
 * </ul>
 * Anything else stands alone. The book shows a collection only when two or more of its items have recipes there.
 */
public final class RecipeFamilies {
    private RecipeFamilies() {}

    /** The dyes' colours, longest first, so that light blue is read before blue. */
    private static final List<String> COLOURS = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black").stream()
            .sorted(Comparator.comparingInt(String::length).reversed()).toList();

    /** The words for a block's finish, before its material: what's left is its family. */
    private static final List<String> FINISHES = List.of("waxed_", "exposed_", "weathered_", "oxidized_", "polished_", "chiseled_", "cut_",
            "cracked_", "smooth_", "mossy_", "cobbled_", "packed_", "stripped_");
    /** The words for a block's shape or form, after its material. */
    private static final List<String> SHAPES = List.of("_stairs", "_slab", "_wall", "_fence_gate", "_fence", "_trapdoor", "_door",
            "_pressure_plate", "_button", "_hanging_sign", "_sign", "_shelf", "_chest_boat", "_chest_raft", "_boat", "_raft", "_grate", "_bulb",
            "_planks", "_block", "_bricks", "_brick", "_tiles", "_tile", "_mosaic", "_pillar", "_wood", "_hyphae");
    /** Families whose name doesn't say their material. */
    private static final Map<String, String> ALIASES = Map.of("heavy_weighted", "iron", "light_weighted", "gold", "bricks", "brick");
    private static final List<String> TOOLS = List.of("_pickaxe", "_axe", "_shovel", "_hoe", "_sword", "_spear");
    private static final List<String> ARMOR = List.of("_helmet", "_chestplate", "_leggings", "_boots");
    private static final String DYED = "dyed:";

    /** The collection the item with this id's path belongs to, among the items in the registry, or null if none. */
    @Nullable
    public static String collection(String path) {
        return collection(path, itemPaths());
    }

    /** The collection the item with this path belongs to, among these item paths, or null if none. */
    @Nullable
    public static String collection(String path, Set<String> items) {
        for (String tool : TOOLS) if (path.endsWith(tool) && path.length() > tool.length()) return path.substring(0, path.length() - tool.length()) + "_tools";
        for (String piece : ARMOR) if (path.endsWith(piece) && path.length() > piece.length()) return path.substring(0, path.length() - piece.length()) + "_armor";
        String dyed = dyedKind(path, items);
        if (dyed != null) return DYED + dyed;
        String alias = ALIASES.get(path);
        if (alias != null) return alias;
        String family = family(path);
        if (family != null) return ALIASES.getOrDefault(family, family);
        // A family's plain block (sandstone, andesite, stone): its family is its name
        return families(items).contains(path) ? path : null;
    }

    /** A collection's title, in words: "Iron Tools", "Stained Glass Pane". */
    public static String title(String collection) {
        String name = collection.startsWith(DYED) ? collection.substring(DYED.length()) : collection;
        List<String> words = new ArrayList<>();
        for (String word : name.split("_")) {
            if (!word.isEmpty()) words.add(word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1));
        }
        return String.join(" ", words);
    }

    /** The kind of dyed thing the item is, coloured or plain, if every colour of the kind is an item; else null. */
    @Nullable
    private static String dyedKind(String path, Set<String> items) {
        String kind = path;
        for (String colour : COLOURS) {
            if (path.startsWith(colour + "_") && path.length() > colour.length() + 1) {
                kind = path.substring(colour.length() + 1);
                break;
            }
        }
        for (String colour : COLOURS) {
            if (!items.contains(colour + "_" + kind)) return null;
        }
        return kind;
    }

    /** What's left of a name without its shape and finish words, or null if it has none (it isn't a family's). */
    @Nullable
    public static String family(String path) {
        String name = path;
        boolean stripped = false;
        for (boolean changed = true; changed; ) {
            changed = false;
            for (String shape : SHAPES) {
                if (name.endsWith(shape) && name.length() > shape.length()) {
                    name = name.substring(0, name.length() - shape.length());
                    changed = stripped = true;
                    break;
                }
            }
            if (changed) continue;
            for (String finish : FINISHES) {
                if (name.startsWith(finish) && name.length() > finish.length()) {
                    name = name.substring(finish.length());
                    changed = stripped = true;
                    break;
                }
            }
        }
        return stripped ? name : null;
    }

    private static volatile Set<String> itemPaths;
    private static volatile Set<String> families;
    private static volatile Set<String> familiesOf;

    /** The paths of the items in the registry, read once. */
    private static Set<String> itemPaths() {
        Set<String> known = itemPaths;
        if (known == null) {
            known = new HashSet<>();
            for (Identifier id : BuiltInRegistries.ITEM.keySet()) known.add(id.getPath());
            itemPaths = known;
        }
        return known;
    }

    /** Every family some item's name belongs to. */
    private static Set<String> families(Set<String> items) {
        Set<String> known = families;
        if (known == null || familiesOf != items) {
            known = new HashSet<>();
            for (String path : items) {
                String family = family(path);
                if (family != null) known.add(family);
            }
            families = known;
            familiesOf = items;
        }
        return known;
    }
}
