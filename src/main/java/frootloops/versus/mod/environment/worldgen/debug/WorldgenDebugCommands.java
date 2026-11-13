package frootloops.versus.mod.environment.worldgen.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;

/**
 * {@code /pvwg} worldgen debugging commands (permission level 2):
 * <ul>
 *   <li>{@code /pvwg probe}: explains the generator's view of your position (biome, climate point, density
 *   functions, aquifer decision). See {@link WorldgenProbe}.</li>
 *   <li>{@code /pvwg bench <radius>}: generates the chunks around you stage by stage, then writes timings, metrics and
 *   map images to {@code <game dir>/pvwg/}. Blocks the server while it runs; use a fresh area. See
 *   {@link WorldgenBench}.</li>
 *   <li>{@code /pvwg rock <id> [seed]}: places the rock of a rock feature or rock formation where it is run. See
 *   {@link RockCommand}.</li>
 * </ul>
 */
public final class WorldgenDebugCommands {

    private WorldgenDebugCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("pvwg")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("probe")
                                .executes(context -> WorldgenProbe.run(context.getSource())))
                        .then(Commands.literal("bench")
                                .then(Commands.argument("radius", IntegerArgumentType.integer(1, WorldgenBench.MAX_RADIUS))
                                        .executes(context -> WorldgenBench.runCommand(
                                                context.getSource(), IntegerArgumentType.getInteger(context, "radius")))))
                        .then(Commands.literal("rock")
                                .then(Commands.argument("id", IdentifierArgument.id()).suggests(RockCommand::suggest)
                                        .executes(context -> RockCommand.run(context.getSource(), IdentifierArgument.getId(context, "id"), null))
                                        .then(Commands.argument("seed", LongArgumentType.longArg())
                                                .executes(context -> RockCommand.run(context.getSource(), IdentifierArgument.getId(context, "id"),
                                                        LongArgumentType.getLong(context, "seed"))))))));
    }
}
