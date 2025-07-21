package frootloops.versus.mixin.enchantments;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Util;
import net.minecraft.util.collection.IndexedIterable;
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
import java.util.Optional;

@Mixin(EnchantmentScreenHandler.class)
public abstract class EnchantingTableMixin extends ScreenHandler {

    protected EnchantingTableMixin(@Nullable ScreenHandlerType<?> type, int syncId, Random random, Inventory inventory, int[] enchantmentPower, ScreenHandlerContext context, Property seed, int[] enchantmentId, int[] enchantmentLevel) {
        super(type, syncId);
        this.random = random;
        this.inventory = inventory;
        this.enchantmentPower = enchantmentPower;
        this.context = context;
        this.seed = seed;
        this.enchantmentId = enchantmentId;
        this.enchantmentLevel = enchantmentLevel;
    }

    @Shadow private final Random random;
    @Shadow private final Inventory inventory;
    @Shadow public final int[] enchantmentPower;
    @Shadow public final int[] enchantmentId;
    @Shadow public final int[] enchantmentLevel;
    @Shadow private final ScreenHandlerContext context;
    @Shadow private final Property seed;

    @Overwrite
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= 0 && id < this.enchantmentPower.length) {
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
                    if (listCandidateEnchantments.isEmpty())
                        listCandidateEnchantments = this.generateEnchantments(world.getRegistryManager(), stack, id + 1, this.enchantmentPower[id]);
                    if (listCandidateEnchantments.isEmpty())
                        listCandidateEnchantments = this.generateEnchantments(world.getRegistryManager(), stack, id + 2, this.enchantmentPower[id]);
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
                        while (enchantmentLevelEntryIterator.hasNext()) {
                            EnchantmentLevelEntry entry = (EnchantmentLevelEntry) enchantmentLevelEntryIterator.next();
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
                        if (player instanceof ServerPlayerEntity)
                            Criteria.ENCHANTED_ITEM.trigger((ServerPlayerEntity) player, stack, lapisCost);
                        this.inventory.markDirty();
                        this.seed.set(player.getEnchantingTableSeed());
                        this.onContentChanged(this.inventory);
                        world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0f, world.random.nextFloat() * 0.1f + 0.9f);
                    }
                });
            }
            return true;
        } else {
            Util.logErrorOrPause(player.getName() + " pressed invalid button id: " + id);
            return false;
        }
    }

    @Inject(method = "onContentChanged", at = @At(value = "TAIL"))
    private void updateUnavailableEnchantments(Inventory inventory, CallbackInfo ci) {
        if(inventory != this.inventory) return;
        VersusMod.MOD_LOGGER.warn("[ ENCHANTING SCREEN HANDLER ] Content changed!");

        this.context.run((world, pos) -> {
            List<EnchantmentLevelEntry> list;
            ItemStack itemStack = inventory.getStack(0);
            if(!itemStack.isEmpty() && itemStack.isEnchantable()) {
                IndexedIterable<RegistryEntry<Enchantment>> indexedIterable = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getIndexedEntries();
                for (int slotID = 0; slotID < 3; ++slotID) {
                    list = this.generateEnchantments(world.getRegistryManager(), itemStack, slotID, this.enchantmentPower[slotID]);
                    if (list.isEmpty())
                        list = this.generateEnchantments(world.getRegistryManager(), itemStack, slotID + 1, this.enchantmentPower[slotID]);
                    if (list.isEmpty())
                        list = this.generateEnchantments(world.getRegistryManager(), itemStack, slotID + 2, this.enchantmentPower[slotID]);
                    if (this.enchantmentPower[slotID] <= 0 || list.isEmpty()) {
                        this.enchantmentPower[slotID] = 0;
                        this.enchantmentLevel[slotID] = -1;
                        this.enchantmentId[slotID] = -1;
                    }
                    else {
                        EnchantmentLevelEntry enchantmentLevelEntry = EnchantRegistryHelper.getMostImportantEnchant(list);
                        this.enchantmentId[slotID] = indexedIterable.getRawId(enchantmentLevelEntry.enchantment);
                        this.enchantmentLevel[slotID] = enchantmentLevelEntry.level;
                    }
                }
            }
            this.sendContentUpdates();
        });
    }

    private List<EnchantmentLevelEntry> generateEnchantments(DynamicRegistryManager registryManager, ItemStack stack, int seedOffset, int level) {
        this.random.setSeed((long)(this.seed.get() + seedOffset));
        Optional<RegistryEntryList.Named<Enchantment>> optional = registryManager.getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(EnchantmentTags.IN_ENCHANTING_TABLE);
        if (optional.isEmpty()) {
            return List.of();
        } else {
            List<EnchantmentLevelEntry> list = EnchantmentHelper.generateEnchantments(this.random, stack, level, ((RegistryEntryList.Named)optional.get()).stream());
            if (stack.isOf(Items.BOOK) && list.size() > 1) {
                return List.of(EnchantRegistryHelper.getMostImportantEnchant(list));
            }
            return list;
        }
    }
}
