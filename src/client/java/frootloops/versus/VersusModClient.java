package frootloops.versus;

import frootloops.versus.mod.environment.CustomParticles;
import frootloops.versus.mod.environment.SparksParticle;
import frootloops.versus.mod.mobs.ModEntitiesRenderers;
import frootloops.versus.mod.environment.CustomBlocksClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.EndRodParticle;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;

import static frootloops.versus.VersusMod.MOD_ID;

@Environment(EnvType.CLIENT)
public class VersusModClient implements ClientModInitializer {

	public static final Identifier HUD_TEXTURE_DISABLED_FOOD = Identifier.of(MOD_ID, "hud/disabled_food_empty");
	public static final Identifier HUD_TEXTURE_DISABLED_FOOD_HALF = Identifier.of(MOD_ID, "hud/disabled_food_empty_half");
	public static final Identifier HUD_TEXTURE_DISABLED_FOOD_HUNGER = Identifier.of(MOD_ID, "hud/disabled_food_empty_hunger");
	public static final Identifier HUD_TEXTURE_DISABLED_FOOD_HALF_HUNGER = Identifier.of(MOD_ID, "hud/disabled_food_empty_half_hunger");

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ModEntitiesRenderers.onInitialize();
		CustomBlocksClient.onInitialize();

		ParticleFactoryRegistry.getInstance().register(CustomParticles.SPARKS, SparksParticle.Factory::new);
	}
}