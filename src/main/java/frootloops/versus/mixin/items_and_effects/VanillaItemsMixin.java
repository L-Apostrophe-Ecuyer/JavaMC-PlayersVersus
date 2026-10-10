package frootloops.versus.mixin.items_and_effects;

import frootloops.versus.mod.items_and_effects.VanillaItems;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(Item.class)
public abstract class VanillaItemsMixin implements FeatureElement {

        @Override
        public boolean isEnabled(FeatureFlagSet enabledFeatures) {
                if(VanillaItems.hasReplacementItem((Item)((Object)this))) return false;
                return this.requiredFeatures().isSubsetOf(enabledFeatures);
        }
}
