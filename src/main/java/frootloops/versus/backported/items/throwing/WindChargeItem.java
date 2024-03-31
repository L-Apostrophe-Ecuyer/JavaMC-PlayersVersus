
package frootloops.versus.backported.items.throwing;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class WindChargeItem
extends Item {
    public WindChargeItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_BREEZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.4f / (world.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!world.isClient) {
            WindChargeEntity windChargeEntity = new WindChargeEntity(EntityType.WIND_CHARGE, world);
            windChargeEntity.setOwner(user);
            windChargeEntity.setPosition(user.getEyePos());
            windChargeEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 1.5f, 0.35f);
            world.spawnEntity(windChargeEntity);
        }
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        if (!user.getAbilities().creativeMode) {
            itemStack.decrement(1);
        }
        user.getItemCooldownManager().set(this, 20);
        return TypedActionResult.success(itemStack, world.isClient());
    }
}

