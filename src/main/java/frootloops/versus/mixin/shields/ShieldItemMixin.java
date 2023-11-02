package frootloops.versus.mixin.shields;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ShieldItem.class)
public abstract class ShieldItemMixin extends Item {
    public ShieldItemMixin(Settings settings) { super(settings); }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if(!user.isSneaking() && user instanceof PlayerEntity player)
            player.getItemCooldownManager().set(this, 4);
    }

    @Override
    public boolean isEnchantable(ItemStack item) {
        return true;
    }

    @Override
    public int getEnchantability() {
        return 1;
    }
}
