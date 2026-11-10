package frootloops.versus.mod.environment.worldgen.rocks;

import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Draws rocks without the game, for tuning their shapes in seconds: stamps a shape or a template on rolling grass and
 * casts a ray for every pixel, with the game's block textures (the mod's own where it has them), the game's shading of
 * faces, its smooth lighting at edges and its light under overhangs. Run by {@code tools/rock-preview.sh}:
 * <pre>
 *   out=rock.png shape=monolith height=30 radius=9 seed=5 orbit=40,48,3 look=12
 *   out=rock.png template=some.json seed=5 size=960x540 water=2
 * </pre>
 * {@code orbit} is the camera's place: the angle around the rock, the distance from its middle, and the height of the
 * eye over the ground; {@code look} is the height over the ground it looks at. {@code textures} lists the folders and
 * jars to take block textures from, first found first, separated by {@code :}.
 */
public final class RockPreview {

    private static final int HALF = 96, BOTTOM = 40, HEIGHT = 150, SEA = 64;
    private static final int GRASS_TINT = 0x91BD59, FOLIAGE_TINT = 0x77AB2F, WATER_TINT = 0x3F76E4;

    private enum Kind { AIR, CUBE, SLAB, CROSS, VINE, WATER }

    /** The scene: blocks by number, the ground it started as, and how to draw each block. */
    private static final class Scene implements RockCanvas {
        final short[] blocks = new short[(2 * HALF) * (2 * HALF) * HEIGHT];
        final int[] ground = new int[(2 * HALF) * (2 * HALF)];
        final byte[] sky = new byte[this.blocks.length];
        final List<String> names = new ArrayList<>(List.of("air"));
        final List<Kind> kinds = new ArrayList<>(List.of(Kind.AIR));
        final Map<String, Short> ids = new HashMap<>();
        final RockPalette palette;
        final int waterLevel;

        Scene(RockPalette palette, long seed, int waterDepth) {
            this.palette = palette;
            this.waterLevel = waterDepth > 0 ? SEA - 1 : Integer.MIN_VALUE;
            short grass = this.id("minecraft:grass_block"), dirt = this.id("minecraft:dirt"), stone = this.id("minecraft:stone"), water = this.id("water");
            short tuft = this.id("minecraft:short_grass"), gravel = this.id("minecraft:gravel");
            for (int x = -HALF; x < HALF; x++) {
                for (int z = -HALF; z < HALF; z++) {
                    // Rolling ground in low terraces; with water asked for, a pond around the middle
                    double roll = 2.6 * RockNoise.noise(seed + 900, x / 37.0, z / 37.0) + 1.1 * RockNoise.noise(seed + 901, x / 11.0, z / 11.0);
                    int top = SEA + (int) Math.round(roll);
                    if (waterDepth > 0) top = Math.min(top, SEA - 1 - waterDepth + (int) (Math.hypot(x, z) / 9));
                    boolean wet = top <= this.waterLevel;
                    for (int y = BOTTOM; y < top; y++) this.set(x, y, z, y == top - 1 ? (wet ? gravel : grass) : y >= top - 4 ? dirt : stone);
                    for (int y = top; y <= this.waterLevel; y++) this.set(x, y, z, water);
                    this.ground[(x + HALF) * 2 * HALF + z + HALF] = top;
                    if (!wet && RockNoise.unit(RockNoise.hash(seed + 902, x, 0, z)) < 0.14) this.set(x, top, z, tuft);
                }
            }
        }

        short id(String name) {
            return this.ids.computeIfAbsent(name, key -> {
                this.names.add(key);
                this.kinds.add(key.equals("water") ? Kind.WATER : key.contains("vine") || key.contains("lichen") ? Kind.VINE : key.endsWith("_slab") ? Kind.SLAB
                        : CROSSES.contains(key.substring(key.indexOf(':') + 1)) ? Kind.CROSS : Kind.CUBE);
                return (short) (this.names.size() - 1);
            });
        }

        boolean inside(int x, int y, int z) {
            return x >= -HALF && x < HALF && z >= -HALF && z < HALF && y >= BOTTOM && y < BOTTOM + HEIGHT;
        }

        int index(int x, int y, int z) {
            return ((x + HALF) * 2 * HALF + z + HALF) * HEIGHT + y - BOTTOM;
        }

        short get(int x, int y, int z) {
            return this.inside(x, y, z) ? this.blocks[this.index(x, y, z)] : 0;
        }

        void set(int x, int y, int z, short block) {
            if (this.inside(x, y, z)) this.blocks[this.index(x, y, z)] = block;
        }

        Kind kind(int x, int y, int z) {
            return this.kinds.get(this.get(x, y, z));
        }

        boolean opaque(int x, int y, int z) {
            return this.kind(x, y, z) == Kind.CUBE;
        }

        @Override
        public int ground(int x, int z) {
            if (x < -HALF || x >= HALF || z < -HALF || z >= HALF) return SEA;
            int y = BOTTOM + HEIGHT - 1;
            while (y > BOTTOM && !this.opaque(x, y - 1, z) && this.kind(x, y - 1, z) != Kind.SLAB) y--;
            return y;
        }

        @Override
        public boolean water(int x, int y, int z) {
            return this.kind(x, y, z) == Kind.WATER;
        }

        @Override
        public boolean replaceable(int x, int y, int z) {
            return true;
        }

        @Override
        public void rock(int x, int y, int z, RockMaterial material, boolean slab) {
            String block = slab ? this.palette.slab(material) : null;
            this.set(x, y, z, this.id(block != null ? block : this.palette.block(material)));
            // What stood on the ground here goes
            if (this.kind(x, y + 1, z) == Kind.CROSS) this.set(x, y + 1, z, (short) 0);
        }

        @Override
        public void raise(int x, int z, int from, int to) {
            boolean wet = to - 1 <= this.waterLevel;
            for (int y = from - 1; y < to; y++) {
                this.set(x, y, z, this.id(y == to - 1 ? (wet ? "minecraft:gravel" : "minecraft:grass_block") : "minecraft:dirt"));
            }
            if (this.kind(x, to, z) == Kind.CROSS) this.set(x, to, z, (short) 0);
        }

        @Override
        public void vine(int x, int y, int z, int dx, int dz) {
            if (this.get(x, y, z) == 0) this.set(x, y, z, this.id(this.palette.vine() + "|" + dx + "," + dz));
        }

        @Override
        public void plant(int x, int y, int z, int data) {
            this.set(x, y, z, this.id(this.palette.plants().get(Math.floorMod(data, this.palette.plants().size()))));
        }

        /** The sky's light in every open cell: 15 under the open sky, one less for each block from it, as the game has it. */
        void light() {
            ArrayDeque<int[]> queue = new ArrayDeque<>();
            for (int x = -HALF; x < HALF; x++) {
                for (int z = -HALF; z < HALF; z++) {
                    for (int y = BOTTOM + HEIGHT - 1; y >= BOTTOM && !this.opaque(x, y, z); y--) {
                        this.sky[this.index(x, y, z)] = 15;
                        queue.add(new int[]{x, y, z});
                    }
                }
            }
            int[][] steps = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            while (!queue.isEmpty()) {
                int[] cell = queue.poll();
                int light = this.sky[this.index(cell[0], cell[1], cell[2])];
                if (light <= 1) continue;
                for (int[] step : steps) {
                    int x = cell[0] + step[0], y = cell[1] + step[1], z = cell[2] + step[2];
                    if (!this.inside(x, y, z) || this.opaque(x, y, z) || this.sky[this.index(x, y, z)] >= light - 1) continue;
                    this.sky[this.index(x, y, z)] = (byte) (light - 1);
                    queue.add(new int[]{x, y, z});
                }
            }
        }

        double brightness(int x, int y, int z) {
            double light = (this.inside(x, y, z) ? this.sky[this.index(x, y, z)] : 15) / 15.0;
            return 0.06 + 0.94 * light / (4 - 3 * light);
        }
    }

    private static final List<String> CROSSES = List.of("short_grass", "fern", "bush", "dead_bush", "poppy", "dandelion", "azure_bluet", "oxeye_daisy",
            "cornflower", "short_dry_grass", "tall_dry_grass", "firefly_bush");

    /** Block textures, 16 by 16, from folders and jars. */
    private static final class Textures {
        private final List<Path> folders = new ArrayList<>();
        private final List<ZipFile> jars = new ArrayList<>();
        private final Map<String, int[]> loaded = new HashMap<>();

        Textures(String sources) throws IOException {
            for (String source : sources.split(":")) {
                if (source.isBlank()) continue;
                if (source.endsWith(".jar")) this.jars.add(new ZipFile(source));
                else this.folders.add(Path.of(source));
            }
        }

        /** The texture's pixels, or null when no source has it. */
        int[] get(String name) {
            return this.loaded.computeIfAbsent(name, key -> {
                try {
                    for (Path folder : this.folders) {
                        Path file = folder.resolve(key + ".png");
                        if (Files.exists(file)) return pixels(ImageIO.read(file.toFile()));
                    }
                    for (ZipFile jar : this.jars) {
                        ZipEntry entry = jar.getEntry("assets/minecraft/textures/block/" + key + ".png");
                        if (entry == null) continue;
                        try (InputStream stream = jar.getInputStream(entry)) {
                            return pixels(ImageIO.read(stream));
                        }
                    }
                } catch (IOException exception) {
                    throw new java.io.UncheckedIOException(exception);
                }
                return new int[0];
            });
        }

        private static int[] pixels(BufferedImage image) {
            // A gray picture's values are taken as they are: getRGB would brighten them, as if they were linear light
            boolean gray = image.getColorModel().getColorSpace().getType() == java.awt.color.ColorSpace.TYPE_GRAY && image.getRaster().getNumBands() <= 2;
            int[] pixels = new int[256];
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    int px = x * image.getWidth() / 16, py = y * image.getWidth() / 16;
                    if (gray) {
                        int bits = image.getRaster().getSampleModel().getSampleSize(0);
                        int value = image.getRaster().getSample(px, py, 0) >> (bits - 8);
                        int alpha = image.getRaster().getNumBands() == 2 ? image.getRaster().getSample(px, py, 1) >> (bits - 8) : 255;
                        pixels[y * 16 + x] = alpha << 24 | value << 16 | value << 8 | value;
                    } else {
                        pixels[y * 16 + x] = image.getRGB(px, py);
                    }
                }
            }
            return pixels;
        }

        int sample(String name, double u, double v) {
            int[] pixels = this.get(name);
            if (pixels.length == 0) return 0xFFFF00FF;
            return pixels[Math.clamp((int) (v * 16), 0, 15) * 16 + Math.clamp((int) (u * 16), 0, 15)];
        }

        boolean has(String name) {
            return this.get(name).length > 0;
        }
    }

    private final Scene scene;
    private final Textures textures;

    private RockPreview(Scene scene, Textures textures) {
        this.scene = scene;
        this.textures = textures;
    }

    public static void main(String[] arguments) throws IOException {
        Map<String, String> options = new HashMap<>();
        for (String argument : arguments) {
            int equals = argument.indexOf('=');
            if (equals < 0) throw new IllegalArgumentException("Options are key=value: " + argument);
            options.put(argument.substring(0, equals), argument.substring(equals + 1));
        }
        long seed = Long.parseLong(options.getOrDefault("seed", "1"));
        RockPlan plan;
        if (options.containsKey("template")) {
            plan = RockTemplate.fromJson(JsonParser.parseString(Files.readString(Path.of(options.get("template")))).getAsJsonObject()).plan(seed);
        } else {
            RockShape shape = RockShape.valueOf(options.getOrDefault("shape", "monolith").toUpperCase(java.util.Locale.ROOT));
            double height = Double.parseDouble(options.getOrDefault("height", String.valueOf((shape.minHeight + shape.maxHeight) / 2)));
            double radius = Double.parseDouble(options.getOrDefault("radius", String.valueOf((shape.minRadius + shape.maxRadius) / 2)));
            RockPlan.Body body = shape.body(seed, height, radius);
            if (options.containsKey("cover")) body = body.withCover(Double.parseDouble(options.get("cover")));
            plan = new RockPlan(List.of(body), RockPalette.GRAY);
        }
        Scene scene = new Scene(plan.palette(), Long.parseLong(options.getOrDefault("ground", "3")), Integer.parseInt(options.getOrDefault("water", "0")));
        long start = System.currentTimeMillis();
        RockStamper.stamp(plan, 0, 0, scene, -HALF, -HALF, HALF - 1, HALF - 1);
        long stamped = System.currentTimeMillis();
        scene.light();

        double top = 0;
        for (RockPlan.Body body : plan.bodies()) {
            for (RockPlan.Lobe lobe : body.lobes()) top = Math.max(top, lobe.blob() ? lobe.centerY() + lobe.height() / 2 : lobe.height());
        }
        String[] orbit = options.getOrDefault("orbit", "40," + Math.round(1.5 * top + 16) + ",3").split(",");
        double angle = Math.toRadians(Double.parseDouble(orbit[0])), distance = Double.parseDouble(orbit[1]), eye = Double.parseDouble(orbit[2]);
        double look = Double.parseDouble(options.getOrDefault("look", String.valueOf(0.42 * top)));
        int base = scene.ground[(HALF) * 2 * HALF + HALF];
        double cameraX = distance * Math.sin(angle), cameraZ = -distance * Math.cos(angle);
        int under = scene.ground[(Math.clamp((int) cameraX, -HALF, HALF - 1) + HALF) * 2 * HALF + Math.clamp((int) cameraZ, -HALF, HALF - 1) + HALF];
        double cameraY = Math.max(under, base) + eye;
        String[] size = options.getOrDefault("size", "960x540").split("x");
        RockPreview preview = new RockPreview(scene, new Textures(options.getOrDefault("textures", "")));
        BufferedImage image = preview.render(cameraX, cameraY, cameraZ, 0.5, base + look, 0.5, Integer.parseInt(size[0]), Integer.parseInt(size[1]),
                Double.parseDouble(options.getOrDefault("fov", "70")));
        File out = new File(options.getOrDefault("out", "rock.png"));
        ImageIO.write(image, "png", out);
        System.out.println(out + ": " + plan.bodies().size() + " bodies, top " + Math.round(top) + ", stamped in " + (stamped - start) + " ms, drawn in "
                + (System.currentTimeMillis() - stamped) + " ms");
    }

    /** A picture from the eye at ({@code ex}, {@code ey}, {@code ez}) looking at ({@code lx}, {@code ly}, {@code lz}). */
    private BufferedImage render(double ex, double ey, double ez, double lx, double ly, double lz, int width, int height, double fov) {
        double fx = lx - ex, fy = ly - ey, fz = lz - ez;
        double length = Math.sqrt(fx * fx + fy * fy + fz * fz);
        fx /= length;
        fy /= length;
        fz /= length;
        // To the viewer's right, level; and up, square to both
        double rx = -fz, rz = fx, rl = Math.hypot(rx, rz);
        rx /= rl;
        rz /= rl;
        double ux = -rz * fy, uy = rz * fx - rx * fz, uz = rx * fy;
        if (uy < 0) {
            ux = -ux;
            uy = -uy;
            uz = -uz;
            rx = -rx;
            rz = -rz;
        }
        double tan = Math.tan(Math.toRadians(fov) / 2);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                // Four rays a pixel, for edges that don't crawl
                int red = 0, green = 0, blue = 0;
                for (int sample = 0; sample < 4; sample++) {
                    double sx = (2 * (px + 0.25 + 0.5 * (sample & 1)) / width - 1) * tan * width / height;
                    double sy = (1 - 2 * (py + 0.25 + 0.5 * (sample >> 1)) / height) * tan;
                    double dx = fx + rx * sx + ux * sy, dy = fy + uy * sy, dz = fz + rz * sx + uz * sy;
                    double norm = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    int colour = this.trace(ex, ey, ez, dx / norm, dy / norm, dz / norm);
                    red += colour >> 16 & 255;
                    green += colour >> 8 & 255;
                    blue += colour & 255;
                }
                image.setRGB(px, py, (red / 4) << 16 | (green / 4) << 8 | blue / 4);
            }
        }
        return image;
    }

    private static int sky(double dy) {
        double high = Math.clamp(dy * 1.6 + 0.1, 0, 1);
        return mix(0xB9D3FF, 0x7CA8FF, high);
    }

    private static int mix(int from, int to, double share) {
        int red = (int) ((from >> 16 & 255) + share * ((to >> 16 & 255) - (from >> 16 & 255)));
        int green = (int) ((from >> 8 & 255) + share * ((to >> 8 & 255) - (from >> 8 & 255)));
        int blue = (int) ((from & 255) + share * ((to & 255) - (from & 255)));
        return red << 16 | green << 8 | blue;
    }

    private static int shade(int colour, int tint, double brightness) {
        double red = (colour >> 16 & 255) * (tint >> 16 & 255) / 255.0 * brightness, green = (colour >> 8 & 255) * (tint >> 8 & 255) / 255.0 * brightness;
        double blue = (colour & 255) * (tint & 255) / 255.0 * brightness;
        return Math.min((int) red, 255) << 16 | Math.min((int) green, 255) << 8 | Math.min((int) blue, 255);
    }

    private int trace(double ox, double oy, double oz, double dx, double dy, double dz) {
        Scene scene = this.scene;
        int x = RockNoise.floor(ox), y = RockNoise.floor(oy), z = RockNoise.floor(oz);
        int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1, stepZ = dz > 0 ? 1 : -1;
        double deltaX = Math.abs(1 / dx), deltaY = Math.abs(1 / dy), deltaZ = Math.abs(1 / dz);
        double nextX = ((dx > 0 ? x + 1 : x) - ox) / dx, nextY = ((dy > 0 ? y + 1 : y) - oy) / dy, nextZ = ((dz > 0 ? z + 1 : z) - oz) / dz;
        double t = 0, water = -1;
        int axis = 1;
        int result = -1;
        for (int step = 0; step < 700; step++) {
            Kind kind = scene.kind(x, y, z);
            if (kind != Kind.AIR) {
                String name = scene.names.get(scene.get(x, y, z));
                double hx = ox + dx * t - x, hy = oy + dy * t - y, hz = oz + dz * t - z;
                if (kind == Kind.CUBE) {
                    result = this.cube(name, x, y, z, axis, axis == 0 ? -stepX : axis == 1 ? -stepY : -stepZ, hx, hy, hz, 1);
                } else if (kind == Kind.SLAB) {
                    // The lower half of the cell: where the ray enters it, if it does before leaving the cell
                    double leave = Math.min(nextX, Math.min(nextY, nextZ));
                    double enter = t;
                    int face = axis, sign = axis == 0 ? -stepX : axis == 1 ? -stepY : -stepZ;
                    if (hy > 0.5 + 1e-9) {
                        if (dy >= 0) enter = Double.MAX_VALUE;
                        else {
                            enter = t + (hy - 0.5) / -dy;
                            face = 1;
                            sign = 1;
                        }
                    }
                    if (enter <= leave) {
                        result = this.cube(name.replace("_slab", ""), x, y, z, face, sign,
                                ox + dx * enter - x, oy + dy * enter - y, oz + dz * enter - z, 0.5);
                    }
                } else if (kind == Kind.WATER) {
                    if (water < 0) water = t;
                } else {
                    result = this.thin(name, kind, x, y, z, ox, oy, oz, dx, dy, dz, t, Math.min(nextX, Math.min(nextY, nextZ)));
                }
                if (result >= 0) break;
            }
            if (nextX < nextY && nextX < nextZ) {
                t = nextX;
                nextX += deltaX;
                x += stepX;
                axis = 0;
            } else if (nextY < nextZ) {
                t = nextY;
                nextY += deltaY;
                y += stepY;
                axis = 1;
            } else {
                t = nextZ;
                nextZ += deltaZ;
                z += stepZ;
                axis = 2;
            }
            if (y < BOTTOM || y >= BOTTOM + HEIGHT + 40 || Math.abs(x) > HALF + 300 || Math.abs(z) > HALF + 300) break;
        }
        int colour = result >= 0 ? result : sky(dy);
        if (water >= 0) colour = mix(colour, shade(0xFFFFFF, WATER_TINT, 0.8), Math.min(0.5 + 0.06 * (t - water), 0.85));
        // A little haze with distance
        return result >= 0 ? mix(colour, sky(0.05), 1 - Math.exp(-t / 420)) : colour;
    }

    /** The colour of a cube's face where the ray met it: its texture, the face's own shade, and the light at that corner. */
    private int cube(String name, int x, int y, int z, int axis, int sign, double hx, double hy, double hz, double tall) {
        Scene scene = this.scene;
        String block = name.substring(name.indexOf(':') + 1);
        double u, v;
        String texture = block;
        int tint = 0xFFFFFF;
        if (axis == 1) {
            u = hx;
            v = hz;
            if (block.equals("grass_block")) {
                texture = sign > 0 ? "grass_block_top" : "dirt";
                if (sign > 0) tint = GRASS_TINT;
            } else if (block.equals("tuff") && this.textures.has("tuff_top")) {
                texture = "tuff_top";
            }
        } else {
            u = axis == 0 ? hz : hx;
            v = 1 - hy;
            if (block.equals("grass_block")) texture = "grass_block_side";
        }
        if (block.equals("cobblestone") && this.textures.has("cobblestone0")) texture = "cobblestone" + Math.floorMod(RockNoise.hash(7, x, y, z), 5);
        int colour = this.textures.sample(texture, u, v);
        if (block.equals("grass_block") && axis != 1) {
            int overlay = this.textures.sample("grass_block_side_overlay", u, v);
            if (overlay >>> 24 > 0) colour = shade(overlay, GRASS_TINT, 1);
        }
        // The cell in front of the face lights it; the three cells around each of its corners darken that corner
        int nx = axis == 0 ? sign : 0, ny = axis == 1 ? sign : 0, nz = axis == 2 ? sign : 0;
        double light = tall < 1 && axis == 1 && sign > 0 ? scene.brightness(x, y, z) : scene.brightness(x + nx, y + ny, z + nz);
        double side = axis == 1 ? (sign > 0 ? 1.0 : 0.5) : axis == 2 ? 0.8 : 0.6;
        double corners = 1;
        if (tall >= 1) {
            double a, b;
            if (axis == 1) {
                a = hx;
                b = hz;
            } else if (axis == 0) {
                a = hy;
                b = hz;
            } else {
                a = hx;
                b = hy;
            }
            double low = RockNoise.lerp(a, this.corner(x, y, z, axis, sign, 0, 0), this.corner(x, y, z, axis, sign, 1, 0));
            double high = RockNoise.lerp(a, this.corner(x, y, z, axis, sign, 0, 1), this.corner(x, y, z, axis, sign, 1, 1));
            corners = RockNoise.lerp(b, low, high);
        }
        return shade(colour, tint, light * side * corners);
    }

    /** How lit a face's corner is, by the blocks beside it in front of the face: the game's smooth lighting, near enough. */
    private double corner(int x, int y, int z, int axis, int sign, int cornerA, int cornerB) {
        int a = 2 * cornerA - 1, b = 2 * cornerB - 1;
        int bx = x + (axis == 0 ? sign : 0), by = y + (axis == 1 ? sign : 0), bz = z + (axis == 2 ? sign : 0);
        int ax, ay, az, cx, cy, cz;
        if (axis == 1) {
            ax = a; ay = 0; az = 0; cx = 0; cy = 0; cz = b;
        } else if (axis == 0) {
            ax = 0; ay = a; az = 0; cx = 0; cy = 0; cz = b;
        } else {
            ax = a; ay = 0; az = 0; cx = 0; cy = b; cz = 0;
        }
        boolean first = this.scene.opaque(bx + ax, by + ay, bz + az), second = this.scene.opaque(bx + cx, by + cy, bz + cz);
        boolean diagonal = this.scene.opaque(bx + ax + cx, by + ay + cy, bz + az + cz);
        int open = first && second ? 0 : 3 - (first ? 1 : 0) - (second ? 1 : 0) - (diagonal ? 1 : 0);
        return 0.52 + 0.16 * open;
    }

    /** A plant (two crossed planes) or a vine (a plane against the block it hangs on): the texel the ray meets, if any. */
    private int thin(String name, Kind kind, int x, int y, int z, double ox, double oy, double oz, double dx, double dy, double dz, double enter, double leave) {
        String block = name.substring(name.indexOf(':') + 1);
        double best = Double.MAX_VALUE;
        int colour = -1;
        double light = this.scene.brightness(x, y, z);
        if (kind == Kind.CROSS) {
            int tint = block.equals("short_grass") || block.equals("fern") || block.equals("bush") ? GRASS_TINT : 0xFFFFFF;
            for (int plane = 0; plane < 2; plane++) {
                // The plane x - z = 0 or x + z = 1, in the cell
                double normalX = 1, normalZ = plane == 0 ? -1 : 1, offset = plane == 0 ? 0 : 1;
                double denominator = dx * normalX + dz * normalZ;
                if (Math.abs(denominator) < 1e-9) continue;
                double t = (offset - ((ox - x) * normalX + (oz - z) * normalZ)) / denominator;
                if (t < enter - 1e-9 || t > leave + 1e-9 || t >= best) continue;
                double hx = ox + dx * t - x, hy = oy + dy * t - y;
                if (hx < 0 || hx > 1 || hy < 0 || hy > 1) continue;
                int texel = this.textures.sample(block, hx, 1 - hy);
                if (texel >>> 24 < 128) continue;
                best = t;
                colour = shade(texel, tint, light);
            }
        } else {
            // "name|dx,dz": the side of the cell the vine hangs on
            String[] toward = name.substring(name.indexOf('|') + 1).split(",");
            int towardX = Integer.parseInt(toward[0]), towardZ = Integer.parseInt(toward[1]);
            double plane = towardX != 0 ? (towardX > 0 ? 0.94 : 0.06) : (towardZ > 0 ? 0.94 : 0.06);
            double direction = towardX != 0 ? dx : dz, origin = towardX != 0 ? ox - x : oz - z;
            if (Math.abs(direction) > 1e-9) {
                double t = (plane - origin) / direction;
                if (t >= enter - 1e-9 && t <= leave + 1e-9) {
                    double across = towardX != 0 ? oz + dz * t - z : ox + dx * t - x, hy = oy + dy * t - y;
                    if (across >= 0 && across <= 1 && hy >= 0 && hy <= 1) {
                        int texel = this.textures.sample(block.substring(0, block.indexOf('|')), across, 1 - hy);
                        if (texel >>> 24 >= 128) colour = shade(texel, FOLIAGE_TINT, light * (towardX != 0 ? 0.6 : 0.8));
                    }
                }
            }
        }
        return colour;
    }
}
