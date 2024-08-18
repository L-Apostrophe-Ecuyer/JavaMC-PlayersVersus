package frootloops.versus.mixin.items.throwing;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LingeringPotionItem.class)
public class LingeringPotionMixin extends ThrowablePotionItem {

    public LingeringPotionMixin(Item.Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        return super.use(world, user, hand);
    }

    @Override
    public void onStoppedUsing(ItemStack itemStack, World world, LivingEntity user, int remainingUseTicks) {
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_LINGERING_POTION_THROW, SoundCategory.PLAYERS, 1.0f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        super.onStoppedUsing(itemStack, world, user, remainingUseTicks);
    }
}