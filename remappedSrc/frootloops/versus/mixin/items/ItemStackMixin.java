package frootloops.versus.mixin.items;

import frootloops.versus.mod.items.ItemsAndStacks;
import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentHolder;
import net.minecraft.component.ComponentMapImpl;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EnchantableComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ComponentHolder {

    public ItemStackMixin(Item item, int count, ComponentMapImpl components) {
        this.item = item;
        this.count = count;
        this.components = components;
    }

    @Shadow private final Item item;
    @Shadow private int count;
    @Shadow final ComponentMapImpl components;

    @Shadow
    public Text getName() { return null; }

    @Shadow
    public boolean hasEnchantments() {return false;}


    @Inject(method = "isEnchantable", at = @At("RETURN"), cancellable = true)
    public void isEnchantable(CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue() && item.getComponents().contains(DataComponentTypes.ENCHANTABLE)) {
            ItemEnchantmentsComponent itemEnchantmentsComponent = this.get(DataComponentTypes.ENCHANTMENTS);
            if(itemEnchantmentsComponent == null || itemEnchantmentsComponent.isEmpty()) {
                cir.setReturnValue(true);
                return;
            }
            else {
                int enchantmentPower = 1;
                for (RegistryEntry<Enchantment> enchant : itemEnchantmentsComponent.getEnchantments()) {
                    enchantmentPower += enchant.value().getMinPower(itemEnchantmentsComponent.getLevel(enchant));
                }

                EnchantableComponent enchantabilityComponent = item.getComponents().get(DataComponentTypes.ENCHANTABLE);
                int maxLevel = 4 * enchantabilityComponent.value();
                if(enchantmentPower < maxLevel) cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getMiningSpeedMultiplier", at = @At("RETURN"), cancellable = true)
    public void getMiningSpeedMultiplier(BlockState state, CallbackInfoReturnable<Float> cir) {
        float miningSpeed = cir.getReturnValue();
        if(miningSpeed != 1.0f && this.item instanceof MiningToolItem miningToolItem) {
            if(state.getSoundGroup() == BlockSoundGroup.DEEPSLATE) {
                if(miningToolItem == Items.NETHERITE_PICKAXE) miningSpeed *= 1.3f;
                else if(miningToolItem == Items.DIAMOND_PICKAXE) miningSpeed *= 1.1f;
                else miningSpeed *= 0.6f;
                cir.setReturnValue(miningSpeed);
            }
        }
    }
}
