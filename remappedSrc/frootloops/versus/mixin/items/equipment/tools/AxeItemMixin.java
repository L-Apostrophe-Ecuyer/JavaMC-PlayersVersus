package frootloops.versus.mixin.items_and_effects.equipment.tools;


import frootloops.versus.mod.items_and_effects.equipment.RebalancedTools;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AxeItem.class)
public abstract class AxeItemMixin extends Item {

    public AxeItemMixin(net.minecraft.item.Item.Settings settings) {
        super(settings);
    }

    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 0)
    private static ToolMaterial modifyToolMaterial(ToolMaterial vanillaMaterial) {
        return RebalancedTools.getRebalancedToolMaterial(vanillaMaterial);
    }

    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 0)
    private static float modifyAttackDamage(float dmg) {
        return RebalancedTools.getAxeDamageModifier();
    }

    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 1)
    private static float modifyAttackSpeed(float speed) {
        return RebalancedTools.getAxeSpeedModifier();
    }
}
