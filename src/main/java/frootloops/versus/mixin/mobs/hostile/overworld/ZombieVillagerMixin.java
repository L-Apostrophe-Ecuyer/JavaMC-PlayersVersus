package frootloops.versus.mixin.mobs.hostile.overworld;

import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.mob.ZombieVillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(ZombieVillagerEntity.class)
public abstract class ZombieVillagerMixin extends ZombieEntity {

    public ZombieVillagerMixin(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow private int conversionTimer;

    @Shadow
    private void setConverting(@Nullable UUID uuid, int delay) {}

    @Override
    protected boolean burnsInDaylight() {
        return false;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        if (itemStack.isOf(Items.GOLDEN_APPLE)) {
            if(conversionTimer < 1200 && !((ZombieVillagerEntity)((Object)this)).isConverting()) return ActionResult.FAIL;

            itemStack.decrementUnlessCreative(1, player);
            if (!this.getWorld().isClient) {
                int conversionTime = (conversionTimer > 0) ? (conversionTimer - 600 - this.random.nextInt(200)) : this.random.nextInt(2400) + 1200;
                this.setConverting(player.getUuid(), conversionTime);
            }
            return ActionResult.SUCCESS;
        }
        return super.interactMob(player, hand);
    }

    @Override
    public void initEquipment(Random random, LocalDifficulty localDifficulty) {
        return;
    }
}