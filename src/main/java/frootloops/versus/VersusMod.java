package frootloops.versus;

import frootloops.versus.mod.CustomBlocks;
import frootloops.versus.mod.Enchants;
import frootloops.versus.mod.Combat;
import frootloops.versus.mod.Items;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.SwordItem;
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
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		MOD_LOGGER.info("Launching " + MOD_ID);
		CustomBlocks.init();
		Combat.init();
		Enchants.init();
		Items.init();
	}
}
