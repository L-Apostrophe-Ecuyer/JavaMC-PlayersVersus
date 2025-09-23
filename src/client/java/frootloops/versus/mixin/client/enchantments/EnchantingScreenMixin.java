package frootloops.versus.mixin.client.enchantments;

import frootloops.versus.VersusMod;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.ingame.EnchantingPhrases;
import net.minecraft.client.gui.screen.ingame.EnchantmentScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.EnchantmentScreenHandler;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantingScreenMixin extends HandledScreen<EnchantmentScreenHandler> {
    public EnchantingScreenMixin(EnchantmentScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    private int index = -1;


    @Redirect(method = "drawBackground", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/EnchantingPhrases;generatePhrase(Lnet/minecraft/client/font/TextRenderer;I)Lnet/minecraft/text/StringVisitable;"))
    private StringVisitable replaceGlyphPhrases(EnchantingPhrases enchantingPhrases, TextRenderer textRenderer, int width) {
        int originalIndex = index;
        for(int i = 0; i < 3; i++) {
            index = (index + 1) % 3;
            if(this.handler.enchantmentPower[index] != 0) break;
        }
        if(this.handler.enchantmentPower[index] == 0) {
            VersusMod.MOD_LOGGER.error("[ ENCHANTING SCREEN ] Error when trying to display the enchanting name of index " + index + " -> No Enchanting Power! Index is invalid. Original was " + originalIndex + ", and enchantmentPower = [" + this.handler.enchantmentPower[0] + ", " + this.handler.enchantmentPower[1] + ", " + this.handler.enchantmentPower[2] + "]");
            return StringVisitable.EMPTY;
        }

        // Get the enchantment name and level:
        int level = this.handler.enchantmentLevel[index];
        if(level < 1) {
            VersusMod.MOD_LOGGER.error("[ ENCHANTING SCREEN ] Error when trying to display the enchanting name of index " + index + " -> Level is " + level);
            return StringVisitable.EMPTY;
        }
        Optional<RegistryEntry.Reference<Enchantment>> enchantmentReference = this.client
                .world
                .getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getEntry(this.handler.enchantmentId[index]);

        // Return it:
        if(enchantmentReference.isEmpty()) {
            VersusMod.MOD_LOGGER.error("[ ENCHANTING SCREEN ] Error when trying to display the enchanting name of index " + index + " -> Enchantment ID was " + this.handler.enchantmentId[index]);
            return StringVisitable.EMPTY;
        }
        return textRenderer.getTextHandler().trimToWidth(Enchantment.getName(enchantmentReference.get(), level).copyContentOnly(), width, Style.EMPTY);
    }
}