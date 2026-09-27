package frootloops.versus.mod.environment.worldgen.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;

/**
 * {@code /pvwg} worldgen debugging commands (permission level 2):
 * <ul>
 *   <li>{@code /pvwg probe}: explains the generator's view of your position (biome, climate point, density
 *   functions, aquifer decision). See {@link WorldgenProbe}.</li>
 *   <li>{@code /pvwg bench <radius>}: generates the chunks around you stage by stage, then writes timings, metrics and
 *   map images to {@code <game dir>/pvwg/}. Blocks the server while it runs; use a fresh area. See
 *   {@link WorldgenBench}.</li>
 * </ul>
 */
public final class WorldgenDebugCommands {

    private WorldgenDebugCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("pvwg")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("probe")
                                .executes(context -> WorldgenProbe.run(context.getSource())))
                        .then(CommandManager.literal("bench")
                                .then(CommandManager.argument("radius", IntegerArgumentType.integer(1, WorldgenBench.MAX_RADIUS))
                                        .executes(context -> WorldgenBench.runCommand(
                                                context.getSource(), IntegerArgumentType.getInteger(context, "radius")))))));
    }
}
