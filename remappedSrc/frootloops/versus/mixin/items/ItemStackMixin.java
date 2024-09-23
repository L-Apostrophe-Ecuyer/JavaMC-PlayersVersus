package frootloops.versus.mixin.items;

import Item;
import frootloops.versus.mod.items.ItemsAndStacks;
import net.minecraft.block.BlockState;
import net.minecraft.component.ComponentHolder;
import net.minecraft.component.ComponentMapImpl;
import net.minecraft.component.DataComponentTypes;
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
        if(!cir.getReturnValue()) {
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
                int maxLevel = 50;
                if(item instanceof ArmorItem armorItem) maxLevel = 4 * armorItem.getEnchantability();
                else if(item instanceof ToolItem toolItem) maxLevel = 4 * toolItem.getEnchantability();
                if(enchantmentPower < maxLevel) cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "onClicked", at = @At("HEAD"), cancellable = false)
    public void onClicked(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference, CallbackInfoReturnable<Boolean> cir) {
        if(ItemsAndStacks.hasReplacementItem(item)) {
            ItemStack newStack = new ItemStack(ItemsAndStacks.getReplacementItem(item).getRegistryEntry(), count, components.copy().getChanges());
            slot.setStack(newStack);
        }
    }

    @Inject(method = "getMaxCount", at = @At("HEAD"), cancellable = true)
    public void getMaxCount(CallbackInfoReturnable<Integer> cir) {
        int customMaxCount = ItemsAndStacks.getOverhauledMaxStackSize(item);
        if(customMaxCount > 0) {
            cir.setReturnValue(ItemsAndStacks.getOverhauledMaxStackSize(item));
            cir.cancel();
        }
    }

    @Inject(method = "getMiningSpeedMultiplier", at = @At("RETURN"), cancellable = true)
    public void getMiningSpeedMultiplier(BlockState state, CallbackInfoReturnable<Float> cir) {
        float miningSpeed = cir.getReturnValue();
        if(miningSpeed != 1.0f && this.item instanceof MiningToolItem miningToolItem) {
            ToolMaterial material = miningToolItem.getMaterial();
            if(material == ToolMaterials.WOOD) miningSpeed *= 1.4f;
            else if(material == ToolMaterials.STONE) miningSpeed *= 1.2f;
            else if(state.getSoundGroup() == BlockSoundGroup.DEEPSLATE) {
                if(material.getInverseTag() == BlockTags.INCORRECT_FOR_STONE_TOOL) miningSpeed *= 0.6f;
                else if(material == ToolMaterials.NETHERITE) miningSpeed *= 1.25f;
            }
            cir.setReturnValue(miningSpeed);
        }
    }
}
