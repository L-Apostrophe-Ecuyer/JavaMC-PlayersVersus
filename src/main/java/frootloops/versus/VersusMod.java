package frootloops.versus;

import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.CustomSpecialEffects;
import frootloops.versus.mod.environment.worldgen.CustomWorldgen;
import frootloops.versus.mod.items_and_effects.RegisteringCustomItems;
import frootloops.versus.mod.items_and_effects.VanillaItems;
import frootloops.versus.mod.items_and_effects.brewing.CustomPotions;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.mobs.melee.MobMeleePayload;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.players.death.RespawnNearbyPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.item.v1.DefaultItemComponentEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VersusMod implements ModInitializer {
	public static final boolean DEBUG_MODE = false;
	public static final String MOD_ID = "players-versus";
	public static final Logger MOD_LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		DefaultItemComponentEvents.MODIFY.addPhaseOrdering(Event.DEFAULT_PHASE, Identifier.fromNamespaceAndPath(MOD_ID, "late"));

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

		MOD_LOGGER.info("Registering new items...");
		RegisteringCustomItems.registerAllCustomItems();
		VanillaItems.onInitialize();

		MOD_LOGGER.info("Initializing custom entities and mobs...");
		ModEntities.onInitialize();

		MOD_LOGGER.info("Initializing custom worldgen features...");
		CustomWorldgen.onInitialize();

		MOD_LOGGER.info("Registering networking packets...");
		PayloadTypeRegistry.serverboundPlay().register(RespawnNearbyPayload.ID, RespawnNearbyPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(MobMeleePayload.TYPE, MobMeleePayload.CODEC);
		VersusModServer.addPacketRecievers();

		MOD_LOGGER.info("Done! This mod is ready to party.");
	}
}
