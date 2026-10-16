package frootloops.versus.mixin.items_and_effects.throwing;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SplashPotionItem.class)
public abstract class SplashPotionMixin extends ThrowablePotionItem {

    public SplashPotionMixin(Item.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
        return super.use(world, user, hand);
    }

    @Override
    public boolean releaseUsing(ItemStack itemStack, Level world, LivingEntity user, int remainingUseTicks) {
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 1.0f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        super.releaseUsing(itemStack, world, user, remainingUseTicks);
        return false;
    }
}