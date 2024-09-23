package frootloops.versus.mixin.enchantments;

import Property;
import ScreenHandlerContext;
import frootloops.versus.VersusMod;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.component.Component;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(EnchantmentScreenHandler.class)
public abstract class EnchantingTableMixin extends ScreenHandler {

    protected EnchantingTableMixin(@Nullable ScreenHandlerType<?> type, int syncId, Inventory inventory, int[] enchantmentPower, ScreenHandlerContext context, Property seed, int[] enchantmentId, int[] enchantmentLevel) {
        super(type, syncId);
        this.inventory = inventory;
        this.enchantmentPower = enchantmentPower;
        this.context = context;
        this.seed = seed;
        this.enchantmentId = enchantmentId;
        this.enchantmentLevel = enchantmentLevel;
    }

    @Shadow private final Inventory inventory;
    @Shadow public final int[] enchantmentPower;
    @Shadow public final int[] enchantmentId;
    @Shadow public final int[] enchantmentLevel;
    @Shadow private final ScreenHandlerContext context;
    @Shadow private final Property seed;

    @Shadow private List<EnchantmentLevelEntry> generateEnchantments(DynamicRegistryManager registryManager, ItemStack stack, int slot, int level) { return null;}

    @Overwrite
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id < 0 || id >= this.enchantmentPower.length) {
            Util.error(player.getName() + " pressed invalid button id: " + id);
            return false;
        }

        ItemStack inputStack = this.inventory.getStack(0);
        ItemStack lapisStack = this.inventory.getStack(1);
        int lapisCost = id + 1;
        if ((lapisStack.isEmpty() || lapisStack.getCount() < lapisCost) && !player.getAbilities().creativeMode) {
            return false;
        }

        if (this.enchantmentPower[id] > 0 && !inputStack.isEmpty() && (player.experienceLevel >= lapisCost && player.experienceLevel >= this.enchantmentPower[id] || player.getAbilities().creativeMode)) {
            this.context.run((world, pos) -> {
                ItemStack stack = inputStack;
                List<EnchantmentLevelEntry> listCandidateEnchantments = this.generateEnchantments(world.getRegistryManager(), stack, id, this.enchantmentPower[id]);
                if (!listCandidateEnchantments.isEmpty()) {

                    // Apply costs:
                    player.applyEnchantmentCosts(stack, lapisCost);

                    // Switching to a book:
                    if (stack.isOf(Items.BOOK)) {
                        stack = stack.copyComponentsToNewStack(Items.ENCHANTED_BOOK, 1);
                        this.inventory.setStack(0, stack);
                    }

                    // Add new enchantments, or improve old ones:
                    Iterator enchantmentLevelEntryIterator = listCandidateEnchantments.iterator();
                    while(enchantmentLevelEntryIterator.hasNext()) {
                        EnchantmentLevelEntry entry = (EnchantmentLevelEntry)enchantmentLevelEntryIterator.next();
                        stack.addEnchantment(entry.enchantment, entry.level);
                    }

                    // Update the item:
                    if (!player.getAbilities().creativeMode) {
                        lapisStack.decrement(lapisCost);
                        if (lapisStack.isEmpty()) {
                            this.inventory.setStack(1, ItemStack.EMPTY);
                        }
                    }

                    // Update player and client effects:
                    player.incrementStat(Stats.ENCHANT_ITEM);
                    if (player instanceof ServerPlayerEntity)  Criteria.ENCHANTED_ITEM.trigger((ServerPlayerEntity)player, stack, lapisCost);
                    this.inventory.markDirty();
                    this.seed.set(player.getEnchantmentTableSeed());
                    this.onContentChanged(this.inventory);
                    world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0f, world.random.nextFloat() * 0.1f + 0.9f);
                }
            });
            return true;
        }
        return false;
    }

    @Inject(method = "onContentChanged", at = @At(value = "TAIL"))
    private void updateUnavailableEnchantments(Inventory inventory, CallbackInfo ci) {
        this.context.run((world, pos) -> {
            List<EnchantmentLevelEntry> list;
            ItemStack itemStack = inventory.getStack(0);
            for (int j = 0; j < 3; ++j) {
                if (this.enchantmentPower[j] <= 0 || (list = this.generateEnchantments(world.getRegistryManager(), itemStack, j, this.enchantmentPower[j])) == null || list.isEmpty()) {
                    this.enchantmentPower[j] = 0;
                    this.enchantmentLevel[j] = -1;
                    this.enchantmentId[j] = -1;
                }
            }
            this.sendContentUpdates();
        });
    }
}
