package frootloops.versus.mod.items_and_effects.fruit;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lemons without lemon plants: the two-block lemon trees (the owner: "it's the two block lemon trees that are ugly"),
 * their sapling and pot, the Tuscan grove's orchards and the full lemon trees of earlier versions are all gone, nothing
 * of them left in the assets or data, not even a name; the lemon (the food) and Sour stay.
 */
class LemonsDataTest {

    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final List<String> GONE = List.of("lemon_bush", "lemon_sapling", "lemon_leaves", "lemon_orchards");

    @Test
    void theLemonPlantsAreGone() throws IOException {
        List<String> leftOver = new ArrayList<>();
        try (Stream<Path> files = Files.walk(RESOURCES)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                if (GONE.stream().anyMatch(name::contains)) leftOver.add(file.toString());
                else if (name.endsWith(".json")) {
                    String text = Files.readString(file);
                    for (String gone : GONE) if (text.contains(gone)) leftOver.add(file + " (" + gone + ")");
                }
            }
        }
        assertTrue(leftOver.isEmpty(), leftOver.size() + " files still hold lemon plants: " + leftOver.subList(0, Math.min(8, leftOver.size())));
    }

    @Test
    void theLemonAndSourStay() throws IOException {
        Path assets = RESOURCES.resolve("assets/players-versus");
        for (Path file : List.of(assets.resolve("items/lemon.json"), assets.resolve("textures/item/lemon.png"), assets.resolve("textures/mob_effect/sour.png"))) {
            assertTrue(Files.exists(file), file + " is gone");
        }
        assertTrue(Files.readString(assets.resolve("lang/en_us.json")).contains("\"item.players-versus.lemon\""), "the lemon has no name");
    }
}
