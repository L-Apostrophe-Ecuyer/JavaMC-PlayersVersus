package frootloops.versus;

import frootloops.versus.mod.mobs.ModEntitiesRenderers;
import frootloops.versus.mod.environment.CustomBlocksClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class VersusModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ModEntitiesRenderers.onInitialize();
		CustomBlocksClient.onInitialize();
	}
}