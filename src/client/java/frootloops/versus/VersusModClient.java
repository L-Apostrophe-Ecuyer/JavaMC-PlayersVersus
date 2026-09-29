package frootloops.versus;

import frootloops.versus.mod.environment.CustomSpecialEffects;
import frootloops.versus.mod.environment.SparksParticle;
import frootloops.versus.mod.mobs.ModEntitiesRenderers;
import frootloops.versus.mod.environment.CustomBlocksClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.resources.ResourceLocation;

import static frootloops.versus.VersusMod.MOD_ID;

@Environment(EnvType.CLIENT)
public class VersusModClient implements ClientModInitializer {
	public static final ResourceLocation RECIPE_BOOK_CRAFTABLE_GROUP_OVERLAY = ResourceLocation.withDefaultNamespace("recipe_book/many_craftable_overlay");
	public static final ResourceLocation RECIPE_BOOK_UNCRAFTABLE_GROUP_OVERLAY = ResourceLocation.withDefaultNamespace("recipe_book/many_uncraftable_overlay");
	public static final ResourceLocation RECIPE_BOOK_CRAFTABLE_SINGLE_OVERLAY = ResourceLocation.withDefaultNamespace("recipe_book/single_uncraftable_overlay");

	public static final ResourceLocation HUD_TEXTURE_DISABLED_FOOD = ResourceLocation.fromNamespaceAndPath(MOD_ID, "hud/disabled_food_empty");
	public static final ResourceLocation HUD_TEXTURE_DISABLED_FOOD_HALF = ResourceLocation.fromNamespaceAndPath(MOD_ID, "hud/disabled_food_empty_half");
	public static final ResourceLocation HUD_TEXTURE_DISABLED_FOOD_HUNGER = ResourceLocation.fromNamespaceAndPath(MOD_ID, "hud/disabled_food_empty_hunger");
	public static final ResourceLocation HUD_TEXTURE_DISABLED_FOOD_HALF_HUNGER = ResourceLocation.fromNamespaceAndPath(MOD_ID, "hud/disabled_food_empty_half_hunger");

	@Override
	public void onInitializeClient() {

		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ModEntitiesRenderers.onInitialize();
		CustomBlocksClient.onInitialize();

		ParticleFactoryRegistry.getInstance().register(CustomSpecialEffects.SPARKS_PARTICLE, SparksParticle.Factory::new);
	}
}