package frootloops.versus;

import frootloops.versus.util.Enchants;
import frootloops.versus.util.Combat;
import frootloops.versus.util.Items;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ModInitializer {

	public static final String MOD_ID = "players-versus";
	public static final Logger MOD_LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		MOD_LOGGER.info("Launching " + MOD_ID);
		Combat.init();
		Enchants.init();
		Items.init();
	}
}
