package frootloops.versus.mixin.items_and_effects;

import frootloops.versus.mod.items_and_effects.VanillaItems;
import net.minecraft.item.*;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.resource.featuretoggle.ToggleableFeature;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(Item.class)
public abstract class VanillaItemsMixin implements ToggleableFeature {

        @Override
        public boolean isEnabled(FeatureSet enabledFeatures) {
                if(VanillaItems.hasReplacementItem((Item)((Object)this))) return false;
                return this.getRequiredFeatures().isSubsetOf(enabledFeatures);
        }
}
