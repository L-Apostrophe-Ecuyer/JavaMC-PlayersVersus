package frootloops.versus.mixin.items.equipment.tools;


import frootloops.versus.mod.items.equipment.RebalancedTools;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterial;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(HoeItem.class)
public abstract class HoeItemMixin extends Item {

    public HoeItemMixin(net.minecraft.item.Item.Settings settings) {
        super(settings);
    }

    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 0)
    private static ToolMaterial modifyToolMaterial(ToolMaterial vanillaMaterial) {
        return RebalancedTools.getRebalancedToolMaterial(vanillaMaterial);
    }

    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 0)
    private static float modifyAttackDamage(float dmg) {
        return RebalancedTools.getHoeDamageModifier();
    }

    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 1)
    private static float modifyAttackSpeed(float speed) {
        return RebalancedTools.getHoeSpeedModifier();
    }
}
