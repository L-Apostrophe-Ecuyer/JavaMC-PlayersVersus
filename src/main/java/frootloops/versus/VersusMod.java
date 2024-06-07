package frootloops.versus;

import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.CustomWorldgen;
import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.items.Items;
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
		MOD_LOGGER.info("Launching Players Versus!");

		MOD_LOGGER.info("Setting up combat parameters...");
		Combat.onInitialize();

		MOD_LOGGER.info("Initializing custom blocks...");
		CustomBlocks.onInitialize();

		MOD_LOGGER.info("Initializing custom worldgen features...");
		CustomWorldgen.onInitialize();

		MOD_LOGGER.info("Initializing custom entities and mobs...");
		ModEntities.onInitialize();

		MOD_LOGGER.info("Implementing item changes and adding new ones...");
		Items.onInitialize();

		MOD_LOGGER.info("Registering networking packets...");
		PayloadTypeRegistry.playC2S().register(RespawnNearbyPayload.ID, RespawnNearbyPayload.CODEC);
		VersusModServer.addPacketRecievers();

		MOD_LOGGER.info("Done! This mod is ready to party.");
	}
}
