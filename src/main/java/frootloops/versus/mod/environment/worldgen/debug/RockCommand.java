package frootloops.versus.mod.environment.worldgen.debug;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import frootloops.versus.mod.environment.worldgen.rocks.LevelRockCanvas;
import frootloops.versus.mod.environment.worldgen.rocks.RockFeature;
import frootloops.versus.mod.environment.worldgen.rocks.RockFormationStructure;
import frootloops.versus.mod.environment.worldgen.rocks.RockPlan;
import frootloops.versus.mod.environment.worldgen.rocks.RockStamper;
import frootloops.versus.mod.environment.worldgen.rocks.RockTemplate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * {@code /pvwg rock <id> [seed]}: places a rock where the command is run, as the world would: the rock of a rock
 * feature ({@code worldgen/feature}) or a rock formation ({@code worldgen/structure}), drawn from the seed given or a
 * new one, which it reports, so that a rock one likes can be placed again.
 */
public final class RockCommand {

    private RockCommand() {
    }

    static int run(CommandSourceStack source, Identifier id, @Nullable Long seed) {
        RockTemplate template = template(source.getServer().registryAccess(), id);
        if (template == null) {
            source.sendFailure(Component.literal("[pvwg] no rock feature or rock formation " + id));
            return 0;
        }
        long chosen = seed != null ? seed : source.getLevel().getRandom().nextLong();
        BlockPos at = BlockPos.containing(source.getPosition());
        RockPlan plan = template.plan(chosen);
        LevelRockCanvas canvas = new LevelRockCanvas(source.getLevel(), plan.palette());
        int reach = template.reach();
        RockStamper.stamp(plan, at.getX(), at.getZ(), canvas, at.getX() - reach, at.getZ() - reach, at.getX() + reach, at.getZ() + reach);
        source.sendSuccess(() -> Component.literal("[pvwg] " + id + " seed " + chosen + ": " + canvas.placed() + " blocks"), true);
        return Math.max(canvas.placed(), 1);
    }

    @Nullable
    static RockTemplate template(RegistryAccess registries, Identifier id) {
        if (registries.lookupOrThrow(Registries.FEATURE).getValue(id) instanceof RockFeature feature) return feature.rock();
        if (registries.lookupOrThrow(Registries.STRUCTURE).getValue(id) instanceof RockFormationStructure formation) return formation.rock();
        return null;
    }

    static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        RegistryAccess registries = context.getSource().getServer().registryAccess();
        List<String> ids = new ArrayList<>();
        registries.lookupOrThrow(Registries.FEATURE).entrySet().forEach(entry -> {
            if (entry.getValue() instanceof RockFeature) ids.add(entry.getKey().identifier().toString());
        });
        registries.lookupOrThrow(Registries.STRUCTURE).entrySet().forEach(entry -> {
            if (entry.getValue() instanceof RockFormationStructure) ids.add(entry.getKey().identifier().toString());
        });
        return SharedSuggestionProvider.suggest(ids, builder);
    }
}
