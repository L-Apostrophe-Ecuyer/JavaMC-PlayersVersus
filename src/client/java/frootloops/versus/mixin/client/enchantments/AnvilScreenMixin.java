package frootloops.versus.mixin.client.enchantments;


import frootloops.versus.VersusMod;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.CyclingSlotIcon;
import net.minecraft.client.gui.screen.ingame.ForgingScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin extends ForgingScreen<AnvilScreenHandler> {

    @Shadow
    private final PlayerEntity player;

    private static final Text BOOK_COMBINE_MESSAGE = Text.translatable("container.repair.book_combine_message");
    private static final Text BOOK_INVALID_MESSAGE = Text.translatable("container.repair.book_invalid");



    private static final Text MSG_ERROR_CONFLICTING_ENCHANTMENTS = Text.translatable("container.repair.conflicting_enchants");
    private static final Text MSG_ERROR_INCOMPATIBLE_ITEMS = Text.translatable("container.repair.items_are_incompatible");
    private static final Text MSG_ERROR_INCOMPATIBLE_BOOK = Text.translatable("container.repair.book_invalid_for_item");
    private static final Text MSG_ADD_BOOK_TO_COMBINE = Text.translatable("container.repair.book_combine_message");
    private static final String TEXT_LVL_COST = "container.repair.book_cost";
    private static final String TEXT_LVL_REQUIRED = "container.repair.cost";

    private static final Identifier EMPTY_ARMOR_SLOT_HELMET_TEXTURE = Identifier.ofVanilla("container/slot/helmet");
    private static final Identifier EMPTY_ARMOR_SLOT_CHESTPLATE_TEXTURE = Identifier.ofVanilla("container/slot/chestplate");
    private static final Identifier EMPTY_ARMOR_SLOT_LEGGINGS_TEXTURE = Identifier.ofVanilla("container/slot/leggings");
    private static final Identifier EMPTY_ARMOR_SLOT_BOOTS_TEXTURE = Identifier.ofVanilla("container/slot/boots");
    private static final Identifier EMPTY_SLOT_HOE_TEXTURE = Identifier.ofVanilla("container/slot/hoe");
    private static final Identifier EMPTY_SLOT_AXE_TEXTURE = Identifier.ofVanilla("container/slot/axe");
    private static final Identifier EMPTY_SLOT_SWORD_TEXTURE = Identifier.ofVanilla("container/slot/sword");
    private static final Identifier EMPTY_SLOT_SHOVEL_TEXTURE = Identifier.ofVanilla("container/slot/shovel");
    private static final Identifier EMPTY_SLOT_PICKAXE_TEXTURE = Identifier.ofVanilla("container/slot/pickaxe");
    private static final Identifier EMPTY_SLOT_SHIELD_TEXTURE = Identifier.ofVanilla("container/slot/shield");
    private static final Identifier EMPTY_SLOT_TRIDENT_TEXTURE = Identifier.ofVanilla("container/slot/trident");
    private static final Identifier EMPTY_SLOT_BOW_TEXTURE = Identifier.ofVanilla("container/slot/bow");
    private static final Identifier EMPTY_SLOT_CROSSBOW_TEXTURE = Identifier.ofVanilla("container/slot/crossbow");
    private static final Identifier EMPTY_SLOT_BOOK_TEXTURE = Identifier.ofVanilla("container/slot/enchanted_book");
    private static final Identifier EMPTY_SLOT_DIAMOND_TEXTURE = Identifier.ofVanilla("container/slot/diamond");
    private static final Identifier EMPTY_SLOT_INGOT_TEXTURE = Identifier.ofVanilla("container/slot/ingot");
    private static final Identifier EMPTY_SLOT_PLANKS_TEXTURE = Identifier.ofVanilla("container/slot/planks");
    private static final Identifier EMPTY_SLOT_PRISMARINE_TEXTURE = Identifier.ofVanilla("container/slot/prismarine");

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

    private final CyclingSlotIcon toolSlotIcon = new CyclingSlotIcon(0);
    private final CyclingSlotIcon repairSlotIcon = new CyclingSlotIcon(1);

    public AnvilScreenMixin(AnvilScreenHandler handler, PlayerInventory playerInventory, Text title, Identifier texture, PlayerEntity player) {
        super(handler, playerInventory, title, texture);
        this.player = player;
    }

    @Override
    public void handledScreenTick() {
        super.handledScreenTick();
        ItemStack toolStack = this.handler.getSlot(0).getStack();
        if(toolStack.isEmpty()) {
            emptySlotRepairTextures.clear();
            this.toolSlotIcon.updateTexture(EMPTY_SLOT_TOOL_TEXTURE);
            this.repairSlotIcon.updateTexture(emptySlotRepairTextures);
        }
        else {
            emptySlotRepairTextures.clear();
            if(toolStack.isIn(ItemTags.SWORDS)) emptySlotRepairTextures.add(EMPTY_SLOT_SWORD_TEXTURE);
            else if(toolStack.isIn(ItemTags.PICKAXES)) emptySlotRepairTextures.add(EMPTY_SLOT_PICKAXE_TEXTURE);
            else if(toolStack.isIn(ItemTags.SHOVELS)) emptySlotRepairTextures.add(EMPTY_SLOT_SHOVEL_TEXTURE);
            else if(toolStack.isIn(ItemTags.HOES)) emptySlotRepairTextures.add(EMPTY_SLOT_HOE_TEXTURE);
            else if(toolStack.isIn(ItemTags.TRIDENT_ENCHANTABLE)) emptySlotRepairTextures.add(EMPTY_SLOT_TRIDENT_TEXTURE);
            else if(toolStack.isIn(ItemTags.BOW_ENCHANTABLE)) emptySlotRepairTextures.add(EMPTY_SLOT_BOW_TEXTURE);
            else if(toolStack.isIn(ItemTags.CROSSBOW_ENCHANTABLE)) emptySlotRepairTextures.add(EMPTY_SLOT_CROSSBOW_TEXTURE);
            else if(toolStack.getComponents().contains(DataComponentTypes.EQUIPPABLE)) {
                EquippableComponent equip = toolStack.getOrDefault(DataComponentTypes.EQUIPPABLE, null);
                switch (equip.slot()) {
                    case CHEST: emptySlotRepairTextures.add(EMPTY_ARMOR_SLOT_CHESTPLATE_TEXTURE); break;
                    case LEGS: emptySlotRepairTextures.add(EMPTY_ARMOR_SLOT_LEGGINGS_TEXTURE); break;
                    case FEET: emptySlotRepairTextures.add(EMPTY_ARMOR_SLOT_BOOTS_TEXTURE); break;
                    case HEAD: emptySlotRepairTextures.add(EMPTY_ARMOR_SLOT_HELMET_TEXTURE); break;
                }
            }
            if(toolStack.canRepairWith(Items.DIAMOND.getDefaultStack())) emptySlotRepairTextures.add(EMPTY_SLOT_DIAMOND_TEXTURE);
            else if(toolStack.canRepairWith(Items.PRISMARINE_SHARD.getDefaultStack())) emptySlotRepairTextures.add(EMPTY_SLOT_PRISMARINE_TEXTURE);
            else if(toolStack.canRepairWith(Items.OAK_PLANKS.getDefaultStack())) emptySlotRepairTextures.add(EMPTY_SLOT_PLANKS_TEXTURE);
            else if(toolStack.isDamageable()) emptySlotRepairTextures.add(EMPTY_SLOT_INGOT_TEXTURE);
            if(toolStack.isEnchantable()) emptySlotRepairTextures.add(EMPTY_SLOT_BOOK_TEXTURE);
            this.repairSlotIcon.updateTexture(emptySlotRepairTextures);
        }
    }

    @ModifyConstant(method = "drawForeground", constant = @Constant(intValue = 40))
    private int noMoreLimit(int levelLimit) {
        return 999;
    }

    @Inject(method = "drawForeground", at = @At(value = "HEAD"), cancellable = true)
    protected void showBookErrorMessage(DrawContext context, int mouseX, int mouseY, CallbackInfo ci) {
        boolean isFirstSlotBook = handler.getSlot(0).getStack().isOf(Items.ENCHANTED_BOOK);
        if(!handler.getSlot(0).hasStack() || (!isFirstSlotBook && !handler.getSlot(1).hasStack() && !handler.getSlot(2).hasStack())) return;

        boolean isSecondSlotBook = handler.getSlot(1).getStack().isOf(Items.ENCHANTED_BOOK);
        boolean hasResult = handler.getSlot(2).hasStack();
        boolean isError = handler.getSlot(0).hasStack() && handler.getSlot(1).hasStack() && !hasResult && handler.getLevelCost() == 0;

        int color;
        Text text;

        if(isError) {
            color = Colors.LIGHT_RED;
            if(isFirstSlotBook && isSecondSlotBook) text = MSG_ERROR_INCOMPATIBLE_BOOK;
            else if(!isFirstSlotBook && isSecondSlotBook) text = MSG_ERROR_INCOMPATIBLE_BOOK;
            else {
                if(handler.getSlot(0).getStack().isOf(handler.getSlot(1).getStack().getItem())) {
                    if(!isFirstSlotBook && handler.getSlot(0).getStack().getDamage() == 0 && !handler.getSlot(1).getStack().hasEnchantments()) return; // Nothing to repair
                    else text = MSG_ERROR_CONFLICTING_ENCHANTMENTS;
                }
                else text = MSG_ERROR_INCOMPATIBLE_ITEMS;
            }
        }
        else if(hasResult) {
            if(handler.getSlot(0).getStack().hasEnchantments() && !handler.getSlot(2).getStack().hasEnchantments()) {
                color = Colors.LIGHT_YELLOW;
                text = MSG_ERROR_CONFLICTING_ENCHANTMENTS;
            }
            else if(handler.getLevelCost() > 0) {
                color = isSecondSlotBook ? Colors.LIGHT_YELLOW : Colors.GREEN;
                text = Text.translatable(isSecondSlotBook ? TEXT_LVL_COST : TEXT_LVL_REQUIRED, new Object[]{this.handler.getLevelCost()});
            }
            else return;
        }
        else if(!hasResult && isFirstSlotBook && !handler.getSlot(1).hasStack()) {
            color = Colors.LIGHT_GRAY;
            text = MSG_ADD_BOOK_TO_COMBINE;
        }
        else return;

        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, Colors.DARK_GRAY, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, Colors.DARK_GRAY, false);

        int x = this.backgroundWidth - 8 - this.textRenderer.getWidth(text) - 2;
        context.fill(x - 2, 67, this.backgroundWidth - 8, 79, 1325400064);
        context.drawText(this.textRenderer, text, x, 69, color, true);
        ci.cancel();
    }

    @Inject(method = "drawBackground", at = @At(value = "TAIL"))
    protected void drawIcons(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo info) {
        this.toolSlotIcon.render(this.handler, context, delta, this.x, this.y);
        this.repairSlotIcon.render(this.handler, context, delta, this.x, this.y);
    }

}
