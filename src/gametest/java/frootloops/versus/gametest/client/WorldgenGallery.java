package frootloops.versus.gametest.client;

import frootloops.versus.mod.environment.worldgen.debug.WorldgenCensus;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * The worldgen gallery: the real client creates a world from the Create World screen, flies a camera to what was asked
 * for and saves screenshots, for looking at what the generator makes without playing ({@code runClientGameTest}; on a
 * machine without a display, under {@code xvfb-run} with a software Vulkan driver).
 *
 * <p>Does nothing unless {@code -Dpv.gallery=<shots>} is set ({@code -Ppv.gallery=...} on Gradle's command line), or
 * {@code -Dpv.gallery=@<file>} for a file with one shot a line. The shots, separated by {@code ;}:
 * <ul>
 *   <li>{@code biome:<id>}: the nearest patch of the biome: one view from the air, one from the ground;</li>
 *   <li>{@code at:<x>,<z>[,<yaw>[,<pitch>[,<height above ground>]]]}: one view from there;</li>
 *   <li>{@code cmd:<command>}: runs a command (without the slash), as the server;</li>
 *   <li>{@code name:<text>}: the name of the screenshots that follow (they are numbered).</li>
 * </ul>
 * {@code -Dpv.gallery.seed} (default 8675309), {@code -Dpv.gallery.preset} (default {@code players-versus:better_world}),
 * {@code -Dpv.gallery.distance} (render distance, default 10), {@code -Dpv.gallery.size} (default {@code 1280x720}).
 * Screenshots go to the run directory's {@code screenshots} folder.
 */
public class WorldgenGallery implements FabricClientGameTest {

    private static final Logger LOGGER = LoggerFactory.getLogger("pv-gallery");
    /** Chunks take long to generate and draw on a software renderer: ten minutes of ticks. */
    private static final int CHUNK_TIMEOUT = 12_000;
    private static final int SEARCH_RANGE = 8000;

    private int width = 1280, height = 720;
    private String name = "shot";
    private int count;

    @Override
    public void runTest(ClientGameTestContext context) {
        String shots = System.getProperty("pv.gallery");
        if (shots == null || shots.isBlank()) {
            LOGGER.info("[gallery] no shots asked for (-Dpv.gallery=...): nothing to do");
            return;
        }
        if (shots.startsWith("@")) {
            // A file of shots, one a line; lines starting with # are comments
            try {
                StringBuilder lines = new StringBuilder();
                for (String line : Files.readAllLines(Path.of(shots.substring(1)))) {
                    if (!line.isBlank() && !line.startsWith("#")) lines.append(line.trim()).append(';');
                }
                shots = lines.toString();
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        }
        String[] size = System.getProperty("pv.gallery.size", "1280x720").split("x");
        this.width = Integer.parseInt(size[0]);
        this.height = Integer.parseInt(size[1]);
        int distance = Integer.getInteger("pv.gallery.distance", 10);
        String preset = System.getProperty("pv.gallery.preset", "players-versus:better_world");
        String seed = System.getProperty("pv.gallery.seed", "8675309");

        // A small window while chunks load: every frame is drawn on the processor; screenshots are taken at their own size
        context.getInput().resizeWindow(320, 180);
        context.runOnClient(client -> {
            client.options.renderDistance().set(distance);
            client.options.simulationDistance().set(5);
            client.options.cloudStatus().set(CloudStatus.FANCY);
            client.options.graphicsPreset().set(GraphicsPreset.FANCY);
            client.options.bobView().set(false);
        });
        try (TestSingleplayerContext world = context.worldBuilder().adjustSettings(creator -> {
            var presets = creator.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET);
            creator.setWorldType(new WorldCreationUiState.WorldTypeEntry(presets.getOrThrow(
                    ResourceKey.create(Registries.WORLD_PRESET, Identifier.parse(preset)))));
            creator.setSeed(seed);
            creator.setGenerateStructures(true);
            creator.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
            creator.setAllowCommands(true);
        }).create()) {
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("gamemode spectator @p");
            context.getInput().pressKey(options -> options.keyToggleGui);
            for (String shot : shots.split(";")) {
                shot = shot.trim();
                if (shot.isEmpty()) continue;
                int colon = shot.indexOf(':');
                String kind = shot.substring(0, colon), argument = shot.substring(colon + 1).trim();
                switch (kind) {
                    case "biome" -> this.biome(context, world, argument.contains(":") ? argument : "minecraft:" + argument);
                    case "at" -> this.at(context, world, argument);
                    case "cmd" -> world.getServer().runCommand(argument);
                    case "name" -> {
                        this.name = argument;
                        this.count = 0;
                    }
                    default -> throw new IllegalArgumentException("Unknown gallery shot: " + shot);
                }
            }
        }
    }

    /** The nearest patch of a biome: from the air, looking down across its middle, then from the ground in its middle. */
    private void biome(ClientGameTestContext context, TestSingleplayerContext world, String id) {
        BlockPos middle = world.getServer().computeOnServer(server ->
                WorldgenCensus.find(server.overworld(), id, SEARCH_RANGE, LOGGER::info));
        if (middle == null) {
            LOGGER.warn("[gallery] no {} within {} blocks: no screenshot", id, SEARCH_RANGE);
            return;
        }
        this.name = id.substring(id.indexOf(':') + 1);
        this.count = 0;
        int ground = this.ground(world, middle.getX(), middle.getZ());
        // From the south-west, 45 blocks out and 36 up, looking north-east and down
        this.view(context, world, middle.getX() - 32, ground + 36, middle.getZ() + 32, -135, 30);
        // On the ground: from the clearest of a few spots around the middle, looking north
        BlockPos stand = world.getServer().computeOnServer(server -> clearest(server.overworld(), middle));
        this.view(context, world, stand.getX(), stand.getY() + 3, stand.getZ(), 180, 4);
        this.view(context, world, stand.getX(), stand.getY() + 3, stand.getZ(), -60, 4);
    }

    private void at(ClientGameTestContext context, TestSingleplayerContext world, String argument) {
        String[] parts = argument.split(",");
        int x = Integer.parseInt(parts[0].trim()), z = Integer.parseInt(parts[1].trim());
        float yaw = parts.length > 2 ? Float.parseFloat(parts[2].trim()) : 180;
        float pitch = parts.length > 3 ? Float.parseFloat(parts[3].trim()) : 20;
        int above = parts.length > 4 ? Integer.parseInt(parts[4].trim()) : 30;
        this.view(context, world, x, this.ground(world, x, z) + above, z, yaw, pitch);
    }

    private int ground(TestSingleplayerContext world, int x, int z) {
        return world.getServer().computeOnServer(server -> {
            ServerLevel level = server.overworld();
            level.getChunk(x >> 4, z >> 4);
            return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        });
    }

    /** Of a few spots around the middle, the one with the most open sky around it (not under a tree, not in water). */
    private static BlockPos clearest(ServerLevel level, BlockPos middle) {
        BlockPos best = null;
        int bestScore = Integer.MIN_VALUE;
        for (int dx = -24; dx <= 24; dx += 8) {
            for (int dz = -24; dz <= 24; dz += 8) {
                int x = middle.getX() + dx, z = middle.getZ() + dz;
                level.getChunk(x >> 4, z >> 4);
                int floor = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                int score = -Math.abs(dx) - Math.abs(dz);
                for (int ox = -3; ox <= 3; ox += 3) {
                    for (int oz = -3; oz <= 3; oz += 3) {
                        level.getChunk((x + ox) >> 4, (z + oz) >> 4);
                        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x + ox, z + oz);
                        if (top <= floor + 1) score += 20;
                    }
                }
                if (!level.getFluidState(new BlockPos(x, floor - 1, z)).isEmpty()) score -= 100;
                if (score > bestScore) {
                    bestScore = score;
                    best = new BlockPos(x, floor, z);
                }
            }
        }
        return best;
    }

    /**
     * Whether every chunk the server sends this player has arrived. (The test API's own wait asks for the whole square
     * around the player, whose corners the server never sends from a render distance of 7.)
     */
    private static boolean chunksArrived(Minecraft client) {
        if (client.level == null || client.player == null) return false;
        int distance = client.options.getEffectiveRenderDistance();
        ChunkPos center = client.player.chunkPosition();
        for (int dx = -distance; dx <= distance; dx++) {
            for (int dz = -distance; dz <= distance; dz++) {
                int x = center.x() + dx, z = center.z() + dz;
                if (ChunkTrackingView.isWithinDistance(center.x(), center.z(), distance, x, z, true)
                        && client.level.getChunkSource().getChunk(x, z, ChunkStatus.FULL, false) == null) return false;
            }
        }
        return true;
    }

    private void view(ClientGameTestContext context, TestSingleplayerContext world, int x, int y, int z, float yaw, float pitch) {
        long start = System.currentTimeMillis();
        // Looking straight up while the chunks are made and sent: a frame with nothing in view costs next to nothing, where
        // a software renderer takes seconds for one of the landscape
        world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %d %d %d %.1f -90", x, y, z, yaw));
        context.waitTicks(5);
        context.waitFor(WorldgenGallery::chunksArrived, CHUNK_TIMEOUT);
        long downloaded = System.currentTimeMillis();
        world.getServer().runCommand(String.format(Locale.ROOT, "tp @p %d %d %d %.1f %.1f", x, y, z, yaw, pitch));
        context.waitTicks(5);
        context.waitFor(client -> client.levelRenderer.hasRenderedAllSections(), CHUNK_TIMEOUT);
        context.waitTicks(5);
        String file = this.name + (this.count == 0 ? "" : "_" + this.count);
        this.count++;
        context.takeScreenshot(TestScreenshotOptions.of(file).disableCounterPrefix().withSize(this.width, this.height));
        long done = System.currentTimeMillis();
        LOGGER.info("[gallery] {} at {} {} {} facing {} {}: chunks after {} s, drawn after {} s more", file, x, y, z, yaw, pitch,
                (downloaded - start) / 1000, (done - downloaded) / 1000);
    }
}
