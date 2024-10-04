package frootloops.versus;

import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.CustomSpecialEffects;
import frootloops.versus.mod.environment.CustomWorldgen;
import frootloops.versus.mod.items.ItemsAndStacks;
import frootloops.versus.mod.items.brewing.CustomPotions;
import frootloops.versus.mod.items.brewing.CustomStatusEffects;
import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.players.death.RespawnNearbyPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VersusMod implements ModInitializer {
	public static final boolean DEBUG_MODE = false;
	public static final String MOD_ID = "players-versus";
	public static final Logger MOD_LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		// Order is important here
		MOD_LOGGER.info("Launching Players Versus!");

		MOD_LOGGER.info("Setting up combat parameters...");
		Combat.onInitialize();

		MOD_LOGGER.info("Registering new status effects...");
		CustomStatusEffects.registerCustomStatusEffects();

		MOD_LOGGER.info("Initializing custom blocks, sounds, and particles...");
		CustomBlocks.onInitialize();
		CustomSpecialEffects.onInitialize();

		MOD_LOGGER.info("Registering new potions...");
		CustomPotions.registerCustomPotions();

		MOD_LOGGER.info("Implementing item changes and adding new ones...");
		ItemsAndStacks.onInitialize();

		MOD_LOGGER.info("Initializing custom entities and mobs...");
		ModEntities.onInitialize();

		MOD_LOGGER.info("Initializing custom worldgen features...");
		CustomWorldgen.onInitialize();

		MOD_LOGGER.info("Registering networking packets...");
		PayloadTypeRegistry.playC2S().register(RespawnNearbyPayload.ID, RespawnNearbyPayload.CODEC);
		VersusModServer.addPacketRecievers();

		MOD_LOGGER.info("Done! This mod is ready to party.");
	}
}
