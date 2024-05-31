package frootloops.versus.mixin.items;

import frootloops.versus.mod.items.Items;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.*;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    public ItemStackMixin(Item item) {
        this.item = item;
    }

    @Shadow private final Item item;

    @Shadow
    public Text getName() { return null; }

    @Shadow
    public boolean hasEnchantments() {return false;}

    @Inject(method = "getMaxCount", at = @At("HEAD"), cancellable = true)
    public void getMaxCount(CallbackInfoReturnable<Integer> cir) {
        int customMaxCount = Items.getDefaultMaxStackSize(item);
        if(customMaxCount > 0) {
            cir.setReturnValue(customMaxCount);
            cir.cancel();
        }
    }

    @Inject(method = "getMiningSpeedMultiplier", at = @At("RETURN"), cancellable = true)
    public void getMiningSpeedMultiplier(BlockState state, CallbackInfoReturnable<Float> cir) {
        float miningSpeed = cir.getReturnValue();
        if(miningSpeed != 1.0f && this.item instanceof MiningToolItem miningToolItem) {
            ToolMaterial material = miningToolItem.getMaterial();
            if(material == ToolMaterials.WOOD) miningSpeed *= 1.25f;
            else if(material == ToolMaterials.STONE) miningSpeed *= 1.1f;
            else if(state.getSoundGroup() == BlockSoundGroup.DEEPSLATE) {
                if(material.getInverseTag() == BlockTags.INCORRECT_FOR_STONE_TOOL) miningSpeed *= 0.6f;
                else if(material == ToolMaterials.NETHERITE) miningSpeed *= 1.25f;
            }
            cir.setReturnValue(miningSpeed);
        }
    }
}
