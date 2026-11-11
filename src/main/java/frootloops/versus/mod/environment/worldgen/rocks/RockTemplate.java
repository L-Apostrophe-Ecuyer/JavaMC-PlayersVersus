package frootloops.versus.mod.environment.worldgen.rocks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import frootloops.versus.mod.environment.worldgen.rocks.RockPlan.Body;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A kind of rock formation, as the data describes it: from a seed it draws a {@link RockPlan}. In JSON:
 * <pre>
 * {"bodies": [
 *    {"shape": "monolith", "height": [26, 38], "radius": [8, 11]},
 *    {"shape": "boulder", "count": [2, 5], "distance": [9, 14], "cover": 0.3}],
 *  "palette": {"shade": "minecraft:tuff", "light": "minecraft:andesite"}}
 * </pre>
 * A body has a {@code shape} ({@link RockShape}, in lower case) and may have: {@code count} (how many of it, 1),
 * {@code distance} (how far from the formation's middle each stands, 0), {@code angle} (where around the middle, in
 * degrees from east toward south; left out, copies are spread around), {@code height} and {@code radius} (the shape's
 * own range of sizes), each a number or a range {@code [least, most]} that the seed picks from; and, in place of the
 * shape's own, {@code cover} (0 to 1).
 *
 * <p>The {@code palette} names the blocks in place of gray stone's ({@link RockPalette#GRAY}): {@code shade},
 * {@code base}, {@code light}, {@code bright}, {@code soil}, {@code grass}, {@code moss}, {@code plants} (a list).
 *
 * <p>A mistake is an {@link IllegalArgumentException} whose message starts with the field's path and a colon.
 */
public final class RockTemplate {

    private static final Set<String> KEYS = Set.of("bodies", "palette");
    private static final Set<String> BODY_KEYS = Set.of("shape", "count", "distance", "angle", "height", "radius", "cover");
    private static final Map<String, RockMaterial> PALETTE_BLOCKS = Map.of("shade", RockMaterial.SHADE, "base", RockMaterial.BASE, "light", RockMaterial.LIGHT,
            "bright", RockMaterial.BRIGHT, "soil", RockMaterial.SOIL, "grass", RockMaterial.GRASS, "moss", RockMaterial.MOSS);

    private record Range(double least, double most) {
        double pick(long hash) {
            return this.least + (this.most - this.least) * RockNoise.unit(hash);
        }

        int pickWhole(long hash) {
            return (int) this.least + (int) ((this.most - this.least + 1) * RockNoise.unit(hash));
        }
    }

    private record BodySpec(RockShape shape, Range count, Range distance, Range angle, Range height, Range radius, Double cover) {
    }

    private final JsonObject json;
    private final List<BodySpec> bodies;
    private final RockPalette palette;

    private RockTemplate(JsonObject json, List<BodySpec> bodies, RockPalette palette) {
        this.json = json;
        this.bodies = bodies;
        this.palette = palette;
    }

    public static RockTemplate fromJson(JsonObject json) {
        for (String key : json.keySet()) {
            if (!KEYS.contains(key)) throw mistake(key, "not something a rock has; it has " + sorted(KEYS));
        }
        if (!json.has("bodies") || !json.get("bodies").isJsonArray() || json.getAsJsonArray("bodies").isEmpty()) {
            throw mistake("bodies", "a rock needs a list of at least one body");
        }
        List<BodySpec> bodies = new ArrayList<>();
        JsonArray list = json.getAsJsonArray("bodies");
        for (int index = 0; index < list.size(); index++) bodies.add(body("bodies[" + index + "]", list.get(index)));
        RockPalette palette = json.has("palette") ? palette(json.get("palette")) : RockPalette.GRAY;
        return new RockTemplate(json.deepCopy(), List.copyOf(bodies), palette);
    }

    /** The JSON this template was read from. */
    public JsonObject toJson() {
        return this.json.deepCopy();
    }

    public RockPalette palette() {
        return this.palette;
    }

    /** How far from its middle, along x or z, anything of a formation of this kind can lie. */
    public int reach() {
        int reach = 0;
        for (BodySpec spec : this.bodies) {
            // A body's place is rounded to whole blocks
            int away = spec.distance.most > 0 ? (int) Math.ceil(spec.distance.most) + 1 : 0;
            reach = Math.max(reach, away + spec.shape.reach(spec.radius.most, 1, 1));
        }
        return reach;
    }

    public RockPlan plan(long seed) {
        List<Body> bodies = new ArrayList<>();
        for (int index = 0; index < this.bodies.size(); index++) {
            BodySpec spec = this.bodies.get(index);
            long specSeed = RockNoise.hash(seed, 1000 + index);
            int count = spec.count.pickWhole(RockNoise.hash(specSeed, 1));
            double turn = 2 * Math.PI * RockNoise.unit(RockNoise.hash(specSeed, 2));
            for (int copy = 0; copy < count; copy++) {
                long bodySeed = RockNoise.hash(specSeed, 10 + copy);
                double height = spec.height.pick(RockNoise.hash(bodySeed, 1)), radius = spec.radius.pick(RockNoise.hash(bodySeed, 2));
                double away = spec.distance.pick(RockNoise.hash(bodySeed, 3));
                // Copies stand around the middle, each in its own part of the circle, unless the angle is given
                double angle = spec.angle != null ? Math.toRadians(spec.angle.pick(RockNoise.hash(bodySeed, 4)))
                        : turn + 2 * Math.PI * (copy + 0.7 * (RockNoise.unit(RockNoise.hash(bodySeed, 4)) - 0.5)) / count;
                Body body = spec.shape.body(bodySeed, height, radius);
                if (spec.cover != null) body = body.withCover(spec.cover);
                bodies.add(body.at((int) Math.round(away * Math.cos(angle)), (int) Math.round(away * Math.sin(angle))));
            }
        }
        return new RockPlan(List.copyOf(bodies), this.palette);
    }

    // Reading

    private static IllegalArgumentException mistake(String path, String message) {
        return new IllegalArgumentException(path + ": " + message);
    }

    private static String sorted(Set<String> keys) {
        return keys.stream().sorted().toList().toString();
    }

    private static BodySpec body(String path, JsonElement element) {
        if (!element.isJsonObject()) throw mistake(path, "a body is an object with a shape");
        JsonObject json = element.getAsJsonObject();
        for (String key : json.keySet()) {
            if (!BODY_KEYS.contains(key)) throw mistake(path + "." + key, "not something a body has; it has " + sorted(BODY_KEYS));
        }
        if (!json.has("shape") || !json.get("shape").isJsonPrimitive()) throw mistake(path + ".shape", "a body needs a shape: one of " + shapes());
        RockShape shape;
        try {
            shape = RockShape.valueOf(json.get("shape").getAsString().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw mistake(path + ".shape", "no shape '" + json.get("shape").getAsString() + "'; there are " + shapes());
        }
        Range count = range(path + ".count", json, new Range(1, 1), true, false);
        Range distance = range(path + ".distance", json, new Range(0, 0), false, false);
        Range angle = range(path + ".angle", json, null, false, false);
        Range height = range(path + ".height", json, new Range(shape.minHeight, shape.maxHeight), false, true);
        Range radius = range(path + ".radius", json, new Range(shape.minRadius, shape.maxRadius), false, true);
        return new BodySpec(shape, count, distance, angle, height, radius, share(path, json, "cover"));
    }

    private static String shapes() {
        List<String> names = new ArrayList<>();
        for (RockShape shape : RockShape.values()) names.add(shape.name().toLowerCase(Locale.ROOT));
        return names.toString();
    }

    private static Double share(String path, JsonObject json, String key) {
        return json.has(key) ? number(path + "." + key, json.get(key), 0, 1) : null;
    }

    private static double number(String path, JsonElement element, double least, double most) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) throw mistake(path, "a number");
        double value = element.getAsDouble();
        if (!(value >= least && value <= most)) throw mistake(path, "a number from " + least + " to " + most + ", not " + value);
        return value;
    }

    /** A number, or a range {@code [least, most]}. */
    private static Range range(String path, JsonObject json, Range fallback, boolean whole, boolean positive) {
        String key = path.substring(path.lastIndexOf('.') + 1);
        if (!json.has(key)) return fallback;
        JsonElement element = json.get(key);
        double least, most;
        if (element.isJsonArray() && element.getAsJsonArray().size() == 2) {
            least = number(path, element.getAsJsonArray().get(0), 0, 1000);
            most = number(path, element.getAsJsonArray().get(1), 0, 1000);
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            least = most = number(path, element, 0, 1000);
        } else {
            throw mistake(path, "a number or a range [least, most]");
        }
        if (least > most) throw mistake(path, "a range is [least, most]: " + least + " is more than " + most);
        if (positive && least <= 0) throw mistake(path, "more than 0, not " + least);
        if (whole && (least != Math.rint(least) || most != Math.rint(most))) throw mistake(path, "whole numbers");
        return new Range(least, most);
    }

    private static RockPalette palette(JsonElement element) {
        if (!element.isJsonObject()) throw mistake("palette", "an object of blocks");
        JsonObject json = element.getAsJsonObject();
        Map<RockMaterial, String> blocks = new EnumMap<>(RockPalette.GRAY.blocks());
        List<String> plants = RockPalette.GRAY.plants();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String path = "palette." + entry.getKey();
            JsonElement value = entry.getValue();
            if (PALETTE_BLOCKS.containsKey(entry.getKey())) {
                blocks.put(PALETTE_BLOCKS.get(entry.getKey()), text(path, value));
            } else if (entry.getKey().equals("plants")) {
                if (!value.isJsonArray()) throw mistake(path, "a list of blocks");
                List<String> listed = new ArrayList<>();
                for (JsonElement plant : value.getAsJsonArray()) listed.add(text(path, plant));
                plants = List.copyOf(listed);
            } else {
                throw mistake(path, "not something a palette has; it has " + sorted(PALETTE_BLOCKS.keySet()) + " and plants");
            }
        }
        return new RockPalette(Map.copyOf(blocks), plants);
    }

    private static String text(String path, JsonElement element) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) throw mistake(path, "a block's id");
        return element.getAsString();
    }
}
