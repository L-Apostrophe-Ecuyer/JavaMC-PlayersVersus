package frootloops.versus.mod.items_and_effects.containers;

import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.minecraft.world.inventory.tooltip.BundleTooltip;

/** The client's half of bundles and shulker boxes: their tooltips, the peek (and, with the mixins, the wheel). */
public final class ContainersClient {

    private ContainersClient() {
    }

    public static void register() {
        ClientTooltipComponentCallback.EVENT.register(component -> switch (component) {
            case BundleTooltip bundle -> new WideBundleTooltip(bundle.contents());
            case ShulkerTooltip shulker -> new ClientShulkerTooltip(shulker);
            default -> null;
        });
        ShulkerPeek.register();
    }
}
