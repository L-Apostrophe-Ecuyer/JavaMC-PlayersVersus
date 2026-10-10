package frootloops.versus;

import frootloops.versus.mod.environment.CustomSpecialEffects;
import frootloops.versus.mod.environment.SparksParticle;
import frootloops.versus.mod.mobs.ModEntitiesRenderers;
import frootloops.versus.mod.mobs.melee.MeleeAnimation;
import frootloops.versus.mod.environment.CustomBlocksClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.resources.Identifier;

import static frootloops.versus.VersusMod.MOD_ID;

@Environment(EnvType.CLIENT)
public class VersusModClient implements ClientModInitializer {
	public static final Identifier RECIPE_BOOK_CRAFTABLE_GROUP_OVERLAY = Identifier.withDefaultNamespace("recipe_book/many_craftable_overlay");
	public static final Identifier RECIPE_BOOK_UNCRAFTABLE_GROUP_OVERLAY = Identifier.withDefaultNamespace("recipe_book/many_uncraftable_overlay");
	public static final Identifier RECIPE_BOOK_CRAFTABLE_SINGLE_OVERLAY = Identifier.withDefaultNamespace("recipe_book/single_uncraftable_overlay");

	public static final Identifier HUD_TEXTURE_DISABLED_FOOD = Identifier.fromNamespaceAndPath(MOD_ID, "hud/disabled_food_empty");
	public static final Identifier HUD_TEXTURE_DISABLED_FOOD_HALF = Identifier.fromNamespaceAndPath(MOD_ID, "hud/disabled_food_empty_half");
	public static final Identifier HUD_TEXTURE_DISABLED_FOOD_HUNGER = Identifier.fromNamespaceAndPath(MOD_ID, "hud/disabled_food_empty_hunger");
	public static final Identifier HUD_TEXTURE_DISABLED_FOOD_HALF_HUNGER = Identifier.fromNamespaceAndPath(MOD_ID, "hud/disabled_food_empty_half_hunger");

	@Override
	public void onInitializeClient() {

		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ModEntitiesRenderers.onInitialize();
		MeleeAnimation.register();
		CustomBlocksClient.onInitialize();

		ParticleProviderRegistry.getInstance().register(CustomSpecialEffects.SPARKS_PARTICLE, SparksParticle.Factory::new);
	}
}