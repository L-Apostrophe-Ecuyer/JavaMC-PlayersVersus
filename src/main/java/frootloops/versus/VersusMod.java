package frootloops.versus;

import frootloops.versus.backported.entities.FutureEntities;
import frootloops.versus.backported.items.FutureItems;
import frootloops.versus.backported.particles.FutureParticles;
import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.CustomWorldgen;
import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.enchantments.Enchants;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.items.Items;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersusMod implements ModInitializer {
	public static final boolean DEBUG_MODE = false;
	public static final String MOD_ID = "players-versus";
	public static final String MOD_FOLDER = "data/" + MOD_ID;
	public static final Logger MOD_LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Identifier RESPAWN_NEAR_DEATH_PACKET_ID = new Identifier(MOD_ID, "respawn_near_death");
	public static final Identifier HUD_TEXTURE_DISABLED_FOOD = new Identifier(MOD_ID, "hud/disabled_food_empty");
	public static final Identifier HUD_TEXTURE_DISABLED_FOOD_HALF = new Identifier(MOD_ID, "hud/disabled_food_empty_half");
	public static final Identifier HUD_TEXTURE_DISABLED_FOOD_HUNGER = new Identifier(MOD_ID, "hud/disabled_food_empty_hunger");
	public static final Identifier HUD_TEXTURE_DISABLED_FOOD_HALF_HUNGER = new Identifier(MOD_ID, "hud/disabled_food_empty_half_hunger");

	@Override
	public void onInitialize() {
		MOD_LOGGER.info("Launching Players Versus!");

		MOD_LOGGER.info("Setting up combat parameters...");
		Combat.onInitialize();

		MOD_LOGGER.info("Initializing custom blocks...");
		CustomBlocks.onInitialize();

		MOD_LOGGER.info("Initializing custom worldgen features...");
		CustomWorldgen.onInitialize();

		MOD_LOGGER.info("Initializing custom enchantments...");
		Enchants.onInitialize();

		MOD_LOGGER.info("Initializing custom entities and mobs...");
		ModEntities.onInitialize();

		MOD_LOGGER.info("Implementing item changes and adding new ones...");
		Items.onInitialize();

		FutureEntities.onInitialize();
		FutureParticles.onInitialize();
		FutureItems.onInitialize();

		MOD_LOGGER.info("Done! This mod is ready to party.");
		VersusModServer.addPacketRecievers();
	}
}
