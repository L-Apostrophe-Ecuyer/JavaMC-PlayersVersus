package frootloops.versus.mixin.client.enchantments;

import frootloops.versus.VersusMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.Enchantment;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantingScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {
    public EnchantingScreenMixin(EnchantmentMenu handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    private int index = -1;


    @Redirect(method = "extractBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/EnchantmentNames;getRandomName(Lnet/minecraft/client/gui/Font;I)Lnet/minecraft/network/chat/FormattedText;"))
    private FormattedText replaceGlyphPhrases(EnchantmentNames enchantingPhrases, Font textRenderer, int width) {
        int originalIndex = index;
        for(int i = 0; i < 3; i++) {
            index = (index + 1) % 3;
            if(this.menu.costs[index] != 0) break;
        }
        if(this.menu.costs[index] == 0) {
            VersusMod.MOD_LOGGER.error("[ ENCHANTING SCREEN ] Error when trying to display the enchanting name of index " + index + " -> No Enchanting Power! Index is invalid. Original was " + originalIndex + ", and enchantmentPower = [" + this.menu.costs[0] + ", " + this.menu.costs[1] + ", " + this.menu.costs[2] + "]");
            return FormattedText.EMPTY;
        }

        // Get the enchantment name and level:
        int level = this.menu.levelClue[index];
        if(level < 1) {
            VersusMod.MOD_LOGGER.error("[ ENCHANTING SCREEN ] Error when trying to display the enchanting name of index " + index + " -> Level is " + level);
            return FormattedText.EMPTY;
        }
        Optional<Holder.Reference<Enchantment>> enchantmentReference = this.minecraft
                .level
                .registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(this.menu.enchantClue[index]);

        // Return it:
        if(enchantmentReference.isEmpty()) {
            VersusMod.MOD_LOGGER.error("[ ENCHANTING SCREEN ] Error when trying to display the enchanting name of index " + index + " -> Enchantment ID was " + this.menu.enchantClue[index]);
            return FormattedText.EMPTY;
        }
        return textRenderer.getSplitter().headByWidth(Enchantment.getFullname(enchantmentReference.get(), level).plainCopy(), width, Style.EMPTY);
    }
}