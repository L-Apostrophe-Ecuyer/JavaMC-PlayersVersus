package frootloops.versus.mixin.client.enchantments;


import frootloops.versus.VersusMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.CyclingSlotBackground;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends ItemCombinerScreen<AnvilMenu> {

    @Shadow
    private final Player player;

    private static final Component BOOK_COMBINE_MESSAGE = Component.translatable("container.repair.book_combine_message");
    private static final Component BOOK_INVALID_MESSAGE = Component.translatable("container.repair.book_invalid");



    private static final Component MSG_ERROR_CONFLICTING_ENCHANTMENTS = Component.translatable("container.repair.conflicting_enchants");
    private static final Component MSG_ERROR_INCOMPATIBLE_ITEMS = Component.translatable("container.repair.items_are_incompatible");
    private static final Component MSG_ERROR_INCOMPATIBLE_BOOK = Component.translatable("container.repair.book_invalid_for_item");
    private static final Component MSG_ADD_BOOK_TO_COMBINE = Component.translatable("container.repair.book_combine_message");
    private static final String TEXT_LVL_COST = "container.repair.book_cost";
    private static final String TEXT_LVL_REQUIRED = "container.repair.cost";

    private static final Identifier EMPTY_ARMOR_SLOT_HELMET_TEXTURE = Identifier.withDefaultNamespace("container/slot/helmet");
    private static final Identifier EMPTY_ARMOR_SLOT_CHESTPLATE_TEXTURE = Identifier.withDefaultNamespace("container/slot/chestplate");
    private static final Identifier EMPTY_ARMOR_SLOT_LEGGINGS_TEXTURE = Identifier.withDefaultNamespace("container/slot/leggings");
    private static final Identifier EMPTY_ARMOR_SLOT_BOOTS_TEXTURE = Identifier.withDefaultNamespace("container/slot/boots");
    private static final Identifier EMPTY_SLOT_HOE_TEXTURE = Identifier.withDefaultNamespace("container/slot/hoe");
    private static final Identifier EMPTY_SLOT_AXE_TEXTURE = Identifier.withDefaultNamespace("container/slot/axe");
    private static final Identifier EMPTY_SLOT_SWORD_TEXTURE = Identifier.withDefaultNamespace("container/slot/sword");
    private static final Identifier EMPTY_SLOT_SHOVEL_TEXTURE = Identifier.withDefaultNamespace("container/slot/shovel");
    private static final Identifier EMPTY_SLOT_PICKAXE_TEXTURE = Identifier.withDefaultNamespace("container/slot/pickaxe");
    private static final Identifier EMPTY_SLOT_SHIELD_TEXTURE = Identifier.withDefaultNamespace("container/slot/shield");
    private static final Identifier EMPTY_SLOT_TRIDENT_TEXTURE = Identifier.withDefaultNamespace("container/slot/trident");
    private static final Identifier EMPTY_SLOT_BOW_TEXTURE = Identifier.withDefaultNamespace("container/slot/bow");
    private static final Identifier EMPTY_SLOT_CROSSBOW_TEXTURE = Identifier.withDefaultNamespace("container/slot/crossbow");
    private static final Identifier EMPTY_SLOT_BOOK_TEXTURE = Identifier.withDefaultNamespace("container/slot/enchanted_book");
    private static final Identifier EMPTY_SLOT_DIAMOND_TEXTURE = Identifier.withDefaultNamespace("container/slot/diamond");
    private static final Identifier EMPTY_SLOT_INGOT_TEXTURE = Identifier.withDefaultNamespace("container/slot/ingot");
    private static final Identifier EMPTY_SLOT_PLANKS_TEXTURE = Identifier.withDefaultNamespace("container/slot/planks");
    private static final Identifier EMPTY_SLOT_PRISMARINE_TEXTURE = Identifier.withDefaultNamespace("container/slot/prismarine");

    private static List<Identifier> emptySlotRepairTextures = new ArrayList<>(3);
    private static final List<Identifier> EMPTY_SLOT_TOOL_TEXTURE = List.of(
            EMPTY_ARMOR_SLOT_HELMET_TEXTURE,
            EMPTY_SLOT_SWORD_TEXTURE,
            EMPTY_ARMOR_SLOT_CHESTPLATE_TEXTURE,
            EMPTY_SLOT_PICKAXE_TEXTURE,
            EMPTY_ARMOR_SLOT_LEGGINGS_TEXTURE,
            EMPTY_SLOT_AXE_TEXTURE,
            EMPTY_ARMOR_SLOT_BOOTS_TEXTURE,
            EMPTY_SLOT_HOE_TEXTURE,
            EMPTY_SLOT_SHOVEL_TEXTURE,
            EMPTY_SLOT_SHIELD_TEXTURE,
            EMPTY_SLOT_TRIDENT_TEXTURE,
            EMPTY_SLOT_BOW_TEXTURE,
            EMPTY_SLOT_CROSSBOW_TEXTURE
    );

    private final CyclingSlotBackground toolSlotIcon = new CyclingSlotBackground(0);
    private final CyclingSlotBackground repairSlotIcon = new CyclingSlotBackground(1);

    public AnvilScreenMixin(AnvilMenu handler, Inventory playerInventory, Component title, Identifier texture, Player player) {
        super(handler, playerInventory, title, texture);
        this.player = player;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        ItemStack toolStack = this.menu.getSlot(0).getItem();
        if(toolStack.isEmpty()) {
            emptySlotRepairTextures.clear();
            this.toolSlotIcon.tick(EMPTY_SLOT_TOOL_TEXTURE);
            this.repairSlotIcon.tick(emptySlotRepairTextures);
        }
        else {
            emptySlotRepairTextures.clear();
            if(toolStack.is(ItemTags.SWORDS)) emptySlotRepairTextures.add(EMPTY_SLOT_SWORD_TEXTURE);
            else if(toolStack.is(ItemTags.PICKAXES)) emptySlotRepairTextures.add(EMPTY_SLOT_PICKAXE_TEXTURE);
            else if(toolStack.is(ItemTags.SHOVELS)) emptySlotRepairTextures.add(EMPTY_SLOT_SHOVEL_TEXTURE);
            else if(toolStack.is(ItemTags.HOES)) emptySlotRepairTextures.add(EMPTY_SLOT_HOE_TEXTURE);
            else if(toolStack.is(ItemTags.TRIDENT_ENCHANTABLE)) emptySlotRepairTextures.add(EMPTY_SLOT_TRIDENT_TEXTURE);
            else if(toolStack.is(ItemTags.BOW_ENCHANTABLE)) emptySlotRepairTextures.add(EMPTY_SLOT_BOW_TEXTURE);
            else if(toolStack.is(ItemTags.CROSSBOW_ENCHANTABLE)) emptySlotRepairTextures.add(EMPTY_SLOT_CROSSBOW_TEXTURE);
            else if(toolStack.getComponents().has(DataComponents.EQUIPPABLE)) {
                Equippable equip = toolStack.getOrDefault(DataComponents.EQUIPPABLE, null);
                switch (equip.slot()) {
                    case CHEST: emptySlotRepairTextures.add(EMPTY_ARMOR_SLOT_CHESTPLATE_TEXTURE); break;
                    case LEGS: emptySlotRepairTextures.add(EMPTY_ARMOR_SLOT_LEGGINGS_TEXTURE); break;
                    case FEET: emptySlotRepairTextures.add(EMPTY_ARMOR_SLOT_BOOTS_TEXTURE); break;
                    case HEAD: emptySlotRepairTextures.add(EMPTY_ARMOR_SLOT_HELMET_TEXTURE); break;
                }
            }
            if(toolStack.isValidRepairItem(Items.DIAMOND.getDefaultInstance())) emptySlotRepairTextures.add(EMPTY_SLOT_DIAMOND_TEXTURE);
            else if(toolStack.isValidRepairItem(Items.PRISMARINE_SHARD.getDefaultInstance())) emptySlotRepairTextures.add(EMPTY_SLOT_PRISMARINE_TEXTURE);
            else if(toolStack.isValidRepairItem(Items.OAK_PLANKS.getDefaultInstance())) emptySlotRepairTextures.add(EMPTY_SLOT_PLANKS_TEXTURE);
            else if(toolStack.isDamageableItem()) emptySlotRepairTextures.add(EMPTY_SLOT_INGOT_TEXTURE);
            if(toolStack.isEnchantable()) emptySlotRepairTextures.add(EMPTY_SLOT_BOOK_TEXTURE);
            this.repairSlotIcon.tick(emptySlotRepairTextures);
        }
    }

    @ModifyConstant(method = "extractLabels", constant = @Constant(intValue = 40))
    private int noMoreLimit(int levelLimit) {
        return 999;
    }

    @Inject(method = "extractLabels", at = @At(value = "HEAD"), cancellable = true)
    protected void showBookErrorMessage(GuiGraphicsExtractor context, int mouseX, int mouseY, CallbackInfo ci) {
        boolean isFirstSlotBook = menu.getSlot(0).getItem().is(Items.ENCHANTED_BOOK);
        if(!menu.getSlot(0).hasItem() || (!isFirstSlotBook && !menu.getSlot(1).hasItem() && !menu.getSlot(2).hasItem())) return;

        boolean isSecondSlotBook = menu.getSlot(1).getItem().is(Items.ENCHANTED_BOOK);
        boolean hasResult = menu.getSlot(2).hasItem();
        boolean isError = menu.getSlot(0).hasItem() && menu.getSlot(1).hasItem() && !hasResult && menu.getCost() == 0;

        int color;
        Component text;

        if(isError) {
            color = CommonColors.SOFT_RED;
            if(isFirstSlotBook && isSecondSlotBook) text = MSG_ERROR_INCOMPATIBLE_BOOK;
            else if(!isFirstSlotBook && isSecondSlotBook) text = MSG_ERROR_INCOMPATIBLE_BOOK;
            else {
                if(menu.getSlot(0).getItem().is(menu.getSlot(1).getItem().getItem())) {
                    if(!isFirstSlotBook && menu.getSlot(0).getItem().getDamageValue() == 0 && !menu.getSlot(1).getItem().isEnchanted()) return; // Nothing to repair
                    else text = MSG_ERROR_CONFLICTING_ENCHANTMENTS;
                }
                else text = MSG_ERROR_INCOMPATIBLE_ITEMS;
            }
        }
        else if(hasResult) {
            if(menu.getSlot(0).getItem().isEnchanted() && !menu.getSlot(2).getItem().isEnchanted()) {
                color = CommonColors.SOFT_YELLOW;
                text = MSG_ERROR_CONFLICTING_ENCHANTMENTS;
            }
            else if(menu.getCost() > 0) {
                color = isSecondSlotBook ? CommonColors.SOFT_YELLOW : CommonColors.GREEN;
                text = Component.translatable(isSecondSlotBook ? TEXT_LVL_COST : TEXT_LVL_REQUIRED, new Object[]{this.menu.getCost()});
            }
            else return;
        }
        else if(!hasResult && isFirstSlotBook && !menu.getSlot(1).hasItem()) {
            color = CommonColors.LIGHT_GRAY;
            text = MSG_ADD_BOOK_TO_COMBINE;
        }
        else return;

        context.text(this.font, this.title, this.titleLabelX, this.titleLabelY, CommonColors.DARK_GRAY, false);
        context.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, CommonColors.DARK_GRAY, false);

        int x = this.imageWidth - 8 - this.font.width(text) - 2;
        context.fill(x - 2, 67, this.imageWidth - 8, 79, 1325400064);
        context.text(this.font, text, x, 69, color, true);
        ci.cancel();
    }

    @Inject(method = "extractBackground", at = @At(value = "TAIL"))
    protected void drawIcons(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, CallbackInfo info) {
        this.toolSlotIcon.render(this.menu, context, delta, this.leftPos, this.topPos);
        this.repairSlotIcon.render(this.menu, context, delta, this.leftPos, this.topPos);
    }

}
