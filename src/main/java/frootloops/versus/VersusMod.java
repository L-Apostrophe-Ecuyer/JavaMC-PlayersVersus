package frootloops.versus;

import frootloops.versus.mod.CustomBlocks;
import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.enchantments.Enchants;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.items.Items;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VersusMod implements ModInitializer {

	public static final String MOD_ID = "players-versus";
	public static final String MOD_FOLDER = "data/" + MOD_ID;
	public static final Logger MOD_LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Identifier CROSSHAIR_BLOCK_ICONS_TEXTURE = Identifier.of(MOD_ID, "textures/gui/block_placement_icons.png");

	@Override
	public void onInitialize() {
		MOD_LOGGER.info("Launching Players Versus!");

		MOD_LOGGER.info("Setting up combat parameters...");
		Combat.onInitialize();

		MOD_LOGGER.info("Initializing custom blocks...");
		CustomBlocks.onInitialize();

		MOD_LOGGER.info("Initializing custom enchantments...");
		Enchants.onInitialize();

		MOD_LOGGER.info("Initializing custom entities and mobs...");
		ModEntities.onInitialize();

		MOD_LOGGER.info("Implementing item changes and adding new ones...");
		Items.onInitialize();

		MOD_LOGGER.info("Setting up server settings...");
		ServerSettings.onInitialize();

		MOD_LOGGER.info("Done! This mod is ready to party.");
	}
}
