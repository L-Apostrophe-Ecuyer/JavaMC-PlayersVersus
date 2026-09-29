package frootloops.versus.mixin.enchantments;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.enchantments.EnchantRegistryHelper;
import net.minecraft.util.Util;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.IdMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
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

@Mixin(EnchantmentMenu.class)
public abstract class EnchantingTableMixin extends AbstractContainerMenu {

    protected EnchantingTableMixin(@Nullable MenuType<?> type, int syncId, RandomSource random, Container inventory, int[] enchantmentPower, ContainerLevelAccess context, DataSlot seed, int[] enchantmentId, int[] enchantmentLevel) {
        super(type, syncId);
        this.random = random;
        this.enchantSlots = inventory;
        this.costs = enchantmentPower;
        this.access = context;
        this.enchantmentSeed = seed;
        this.enchantClue = enchantmentId;
        this.levelClue = enchantmentLevel;
    }

    @Shadow private final RandomSource random;
    @Shadow private final Container enchantSlots;
    @Shadow public final int[] costs;
    @Shadow public final int[] enchantClue;
    @Shadow public final int[] levelClue;
    @Shadow private final ContainerLevelAccess access;
    @Shadow private final DataSlot enchantmentSeed;

    @Overwrite
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < this.costs.length) {
            ItemStack inputStack = this.enchantSlots.getItem(0);
            ItemStack lapisStack = this.enchantSlots.getItem(1);
            int lapisCost = id + 1;
            if ((lapisStack.isEmpty() || lapisStack.getCount() < lapisCost) && !player.getAbilities().instabuild) {
                return false;
            }

            if (this.costs[id] > 0 && !inputStack.isEmpty() && (player.experienceLevel >= lapisCost && player.experienceLevel >= this.costs[id] || player.getAbilities().instabuild)) {
                this.access.execute((world, pos) -> {
                    ItemStack stack = inputStack;
                    List<EnchantmentInstance> listCandidateEnchantments = this.generateEnchantments(world.registryAccess(), stack, id, this.costs[id]);
                    if (listCandidateEnchantments.isEmpty())
                        listCandidateEnchantments = this.generateEnchantments(world.registryAccess(), stack, id + 1, this.costs[id]);
                    if (listCandidateEnchantments.isEmpty())
                        listCandidateEnchantments = this.generateEnchantments(world.registryAccess(), stack, id + 2, this.costs[id]);
                    if (!listCandidateEnchantments.isEmpty()) {

                        // Apply costs:
                        player.onEnchantmentPerformed(stack, lapisCost);

                        // Switching to a book:
                        if (stack.is(Items.BOOK)) {
                            stack = stack.transmuteCopy(Items.ENCHANTED_BOOK, 1);
                            this.enchantSlots.setItem(0, stack);
                        }

                        // Add new enchantments, or improve old ones:
                        Iterator enchantmentLevelEntryIterator = listCandidateEnchantments.iterator();
                        while (enchantmentLevelEntryIterator.hasNext()) {
                            EnchantmentInstance entry = (EnchantmentInstance) enchantmentLevelEntryIterator.next();
                            stack.enchant(entry.enchantment(), entry.level());
                        }

                        // Update the item:
                        if (!player.getAbilities().instabuild) {
                            lapisStack.shrink(lapisCost);
                            if (lapisStack.isEmpty()) {
                                this.enchantSlots.setItem(1, ItemStack.EMPTY);
                            }
                        }

                        // Update player and client effects:
                        player.awardStat(Stats.ENCHANT_ITEM);
                        if (player instanceof ServerPlayer)
                            CriteriaTriggers.ENCHANTED_ITEM.trigger((ServerPlayer) player, stack, lapisCost);
                        this.enchantSlots.setChanged();
                        this.enchantmentSeed.set(player.getEnchantmentSeed());
                        this.slotsChanged(this.enchantSlots);
                        world.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0f, world.random.nextFloat() * 0.1f + 0.9f);
                    }
                });
            }
            return true;
        } else {
            Util.logAndPauseIfInIde(player.getName() + " pressed invalid button id: " + id);
            return false;
        }
    }

    @Inject(method = "slotsChanged", at = @At(value = "TAIL"))
    private void updateUnavailableEnchantments(Container inventory, CallbackInfo ci) {
        if(inventory != this.enchantSlots) return;
        this.access.execute((world, pos) -> {
            List<EnchantmentInstance> list;
            ItemStack itemStack = inventory.getItem(0);
            if(!itemStack.isEmpty() && itemStack.isEnchantable()) {
                IdMap<Holder<Enchantment>> indexedIterable = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
                for (int slotID = 0; slotID < 3; ++slotID) {
                    list = this.generateEnchantments(world.registryAccess(), itemStack, slotID, this.costs[slotID]);
                    if (list.isEmpty())
                        list = this.generateEnchantments(world.registryAccess(), itemStack, slotID + 1, this.costs[slotID]);
                    if (list.isEmpty())
                        list = this.generateEnchantments(world.registryAccess(), itemStack, slotID + 2, this.costs[slotID]);
                    if (this.costs[slotID] <= 0 || list.isEmpty()) {
                        this.costs[slotID] = 0;
                        this.levelClue[slotID] = -1;
                        this.enchantClue[slotID] = -1;
                    }
                    else {
                        EnchantmentInstance enchantmentLevelEntry = EnchantRegistryHelper.getMostImportantEnchant(list);
                        this.enchantClue[slotID] = indexedIterable.getId(enchantmentLevelEntry.enchantment());
                        this.levelClue[slotID] = enchantmentLevelEntry.level();
                    }
                }
            }
            this.broadcastChanges();
        });
    }

    private List<EnchantmentInstance> generateEnchantments(RegistryAccess registryManager, ItemStack stack, int seedOffset, int level) {
        this.random.setSeed((long)(this.enchantmentSeed.get() + seedOffset));
        Optional<HolderSet.Named<Enchantment>> optional = registryManager.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE);
        if (optional.isEmpty()) {
            return List.of();
        } else {
            List<EnchantmentInstance> list = EnchantmentHelper.selectEnchantment(this.random, stack, level, ((HolderSet.Named)optional.get()).stream());
            if (stack.is(Items.BOOK) && list.size() > 1) {
                return List.of(EnchantRegistryHelper.getMostImportantEnchant(list));
            }
            return list;
        }
    }
}
