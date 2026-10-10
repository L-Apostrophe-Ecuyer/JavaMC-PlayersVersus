package frootloops.versus.mixin.items_and_effects;

import frootloops.versus.mod.items_and_effects.VanillaItems;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import frootloops.versus.mod.environment.blocks.BlockSounds;
import net.minecraft.world.level.block.sounds.BlockSoundSets;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements DataComponentHolder {

    @Shadow private final Holder<Item> item;
    @Shadow private int count;
    @Shadow final PatchedDataComponentMap components;

    protected ItemStackMixin(Holder<Item> item, int count, PatchedDataComponentMap components) {
        this.item = item;
        this.count = count;
        this.components = components;
    }

    @Shadow
    public Component getHoverName() { return null; }

    @Shadow
    public boolean isEnchanted() {return false;}

    @Shadow public abstract boolean isEmpty();
    @Shadow public abstract Item getItem();

    @ModifyVariable(method = "<init>(Lnet/minecraft/core/Holder;I)V", at = @At("HEAD"), argsOnly = true)
    private static Holder<Item> modifyItemRegistry(Holder<Item> item) {
        if(VanillaItems.hasReplacementItem(item.value())) return BuiltInRegistries.ITEM.wrapAsHolder(VanillaItems.getReplacementItem(item.value()));
        return item;
    }

    @ModifyVariable(method = "<init>(Lnet/minecraft/world/level/ItemLike;I)V", at = @At("HEAD"), argsOnly = true)
    private static ItemLike modifyItemConvertible(ItemLike item) {
        return VanillaItems.getReplacementItem(item.asItem());
    }

    @Inject(method = "isEnchantable", at = @At("RETURN"), cancellable = true)
    public void isEnchantable(CallbackInfoReturnable<Boolean> cir) {
        if(!cir.getReturnValue() && !this.isEmpty() && item.value().components().has(DataComponents.ENCHANTABLE)) {
            ItemEnchantments itemEnchantmentsComponent = this.get(DataComponents.ENCHANTMENTS);
            if(itemEnchantmentsComponent == null || itemEnchantmentsComponent.isEmpty()) {
                cir.setReturnValue(true);
                return;
            }
            else {
                int enchantmentPower = 1;
                for (Holder<Enchantment> enchant : itemEnchantmentsComponent.keySet()) {
                    enchantmentPower += enchant.value().getMinCost(itemEnchantmentsComponent.getLevel(enchant));
                }

                Enchantable enchantabilityComponent = item.value().components().get(DataComponents.ENCHANTABLE);
                int maxLevel = 4 * enchantabilityComponent.value();
                if(enchantmentPower < maxLevel) cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
    public void getMiningSpeedMultiplier(BlockState state, CallbackInfoReturnable<Float> cir) {
        float miningSpeed = cir.getReturnValue();
        if(BlockSounds.is(state, BlockSoundSets.DEEPSLATE)) {
            if(this.getItem() == Items.NETHERITE_PICKAXE) miningSpeed *= 1.3f;
            else if(this.getItem() == Items.DIAMOND_PICKAXE) miningSpeed *= 1.1f;
            else if(this.getItem() == Items.IRON_PICKAXE) return;
            else miningSpeed *= 0.6f;
            cir.setReturnValue(miningSpeed);
        }
    }
}
