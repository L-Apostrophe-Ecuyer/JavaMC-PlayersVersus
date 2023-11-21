package frootloops.versus.mixin.items;

import frootloops.versus.VersusMod;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

import static net.minecraft.item.ItemStack.MODIFIER_FORMAT;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    public ItemStackMixin(Item item) {
        this.item = item;
    }

    @Shadow
    private static boolean isSectionVisible(int flags, ItemStack.TooltipSection tooltipSection) {
        return (flags & tooltipSection.getFlag()) == 0;
    }

    @Shadow
    private int getHideFlags() {
        return 0;
    }

    @Shadow
    private final Item item;

    @Shadow
    public Text getName() { return null; }

    @Shadow
    public boolean hasEnchantments() {return false;}

    @Inject(method = "isEnchantable", at = @At("RETURN"), cancellable = true)
    public void itemsCanBeReEnchanted(CallbackInfoReturnable<Boolean> cir) {
        if(this.hasEnchantments()) {
            cir.setReturnValue(!EnchantmentHelper.getPossibleEntries(10, (ItemStack) ((Object)this), false).isEmpty());
        }
    }

    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private void addAttackReachTooltip(CallbackInfoReturnable<List<Text>> cir) {
        if(isSectionVisible(this.getHideFlags(), ItemStack.TooltipSection.MODIFIERS)) {
            List<Text> list = cir.getReturnValue();

            int i = 0;
            for (i = 0; i < list.size(); i++) {
                if(list.get(i) instanceof MutableText) {
                    if(list.get(i).getStyle().equals(list.get(i).getStyle().withFormatting(Formatting.DARK_GREEN))) break;
                }
            }

            i += 2;
            if(i > 2 && i <= list.size()) {

                double value = -1d;


                if (item instanceof TridentItem || item instanceof HoeItem) {
                    value = 3.5d;
                } else if (item instanceof SwordItem) {
                    value = 3.0d;
                } else if (item instanceof ToolItem) {
                    value = 2.5d;
                }

                if(value != -1d) {
                    Text text = ScreenTexts.space().append(Text.translatable("attribute.modifier.equals.0", MODIFIER_FORMAT.format(value), Text.translatable("attribute.name.generic."+ VersusMod.MOD_ID + ".attack_reach"))).formatted(Formatting.DARK_GREEN);
                    if(i == list.size()) list.add(text);
                    else list.add(i, text);
                    cir.setReturnValue(list);
                }
            }
        }
    }

}
