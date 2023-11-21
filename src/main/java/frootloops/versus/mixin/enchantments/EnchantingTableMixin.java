package frootloops.versus.mixin.enchantments;

import frootloops.versus.VersusMod;
import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnchantmentLevelEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(EnchantmentScreenHandler.class)
public abstract class EnchantingTableMixin extends ScreenHandler {

    protected EnchantingTableMixin(@Nullable ScreenHandlerType<?> type, int syncId, Inventory inventory, int[] enchantmentPower, ScreenHandlerContext context, Property seed) {
        super(type, syncId);
        this.inventory = inventory;
        this.enchantmentPower = enchantmentPower;
        this.context = context;
        this.seed = seed;
    }

    @Shadow private final Inventory inventory;
    @Shadow public final int[] enchantmentPower;
    @Shadow private final ScreenHandlerContext context;
    @Shadow private final Property seed;
    @Shadow private List<EnchantmentLevelEntry> generateEnchantments(ItemStack stack, int slot, int level) { return null;}

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
                List<EnchantmentLevelEntry> listCandidateEnchantments = this.generateEnchantments(stack, id, 1 + this.enchantmentPower[id]);
                if (!listCandidateEnchantments.isEmpty()) {

                    player.applyEnchantmentCosts(stack, lapisCost);

                    boolean isBook = stack.isOf(Items.BOOK);
                    if (isBook) {
                        stack = new ItemStack(Items.ENCHANTED_BOOK);
                        NbtCompound nbtCompound = inputStack.getNbt();
                        if (nbtCompound != null) {
                            stack.setNbt(nbtCompound.copy());
                        }
                        this.inventory.setStack(0, stack);
                    }

                    // Add new enchantments, or improve old ones:
                    Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(inputStack);
                    for (EnchantmentLevelEntry entry : listCandidateEnchantments) {
                        if(!enchantments.containsKey(entry.enchantment) || entry.level > enchantments.get(entry.enchantment)) {
                            enchantments.put(entry.enchantment, entry.level);
                        }
                    }

                    stack.removeSubNbt("Enchantments");
                    stack.removeSubNbt("StoredEnchantments");
                    EnchantmentHelper.set(enchantments, stack);
                    if (stack.isOf(Items.BOOK)) stack = new ItemStack(Items.ENCHANTED_BOOK);

                    if (!player.getAbilities().creativeMode) {
                        lapisStack.decrement(lapisCost);
                        if (lapisStack.isEmpty()) {
                            this.inventory.setStack(1, ItemStack.EMPTY);
                        }
                    }

                    player.incrementStat(Stats.ENCHANT_ITEM);
                    if (player instanceof ServerPlayerEntity) {
                        Criteria.ENCHANTED_ITEM.trigger((ServerPlayerEntity)player, stack, lapisCost);
                    }

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
}
