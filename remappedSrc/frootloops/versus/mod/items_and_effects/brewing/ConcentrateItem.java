package frootloops.versus.mod.items_and_effects.brewing;

import net.minecraft.block.Block;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.ConsumableComponents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.util.List;


public class ConcentrateItem extends BlockItem {

    public static final ConsumableComponent CONCENTRATE_COMPONENT = ConsumableComponents.food().consumeSeconds(2.0f).consumeEffect(new ApplyEffectsConsumeEffect(List.of(new StatusEffectInstance(StatusEffects.HUNGER, 120, 2),new StatusEffectInstance(StatusEffects.NAUSEA, 140, 2)))).build();

    protected RegistryEntry<StatusEffect> effect;
    protected int amplifier;
    protected int duration;

    public ConcentrateItem(net.minecraft.item.Item.Settings settings, RegistryEntry<StatusEffect> registeredEffect, Block block) {
        super(block, settings);
        if(registeredEffect != null) {
            this.effect = registeredEffect;
            this.amplifier = 0;
            this.duration = registeredEffect.value().isInstant() ? 1 : 30;
        }
    }

    public ConcentrateItem(net.minecraft.item.Item.Settings settings, RegistryEntry<StatusEffect> registeredEffect, int amplifier, int duration, Block block) {
        super(block, settings);
        if(registeredEffect != null) {
            this.effect = registeredEffect;
            this.amplifier = amplifier;
            this.duration = duration;
        }
    }

    public RegistryEntry<StatusEffect> getEffect() {
        return effect;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.EAT;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 60;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if(remainingUseTicks % 8 == 0) user.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 16, 4));
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ConcentrateItem item = (ConcentrateItem) stack.getItem();
        if(user.getRandom().nextInt(10) < 7 && item.effect != null) {
            user.addStatusEffect(new StatusEffectInstance(item.effect, item.duration, item.amplifier));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 40, 1));
        }
        else user.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 80, 1));
        return super.finishUsing(stack,world,user);
    }


    /**
     * Called when the item at {@code slot} gets clicked by the cursor
     * holding {@code otherStack}.
     *
     * <p>While this method is usually called on the logical server, it can also be called on
     * the logical client, so take caution when overriding this method. The logical side can be
     * checked using {@link World#isClient}.
     *
     * <p>For example, this is called on {@link BundleItem} when the cursor holds
     * an item and the player clicks on the slot that has a bundle.
     *
     * @return whether the action was successful
     *
     * @param slot the clicked slot
     * @param stack the slot's stack
     * @param otherStack the stack the cursor holds
     */
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        return false; // TODO: Combine stacks
    }
}