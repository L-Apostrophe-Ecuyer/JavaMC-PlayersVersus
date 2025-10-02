package frootloops.versus.mixin.mobs.hostile.overworld;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
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
    public boolean canSpawn(WorldAccess world, SpawnReason spawnReason) {
        if(spawnReason != SpawnReason.NATURAL) return super.canSpawn(world, spawnReason);
        BlockPos pos = this.getBlockPos();
        if(pos.getY() < 56) return false;
        if(this.getPathfindingFavor(pos, world) >= 0.0F) {
            BlockState downState = world.getBlockState(pos.down());
            return downState.isIn(BlockTags.WOLVES_SPAWNABLE_ON) || downState.isIn(BlockTags.SAND);
        }
        return false;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);
        if (itemStack.isOf(Items.GOLDEN_APPLE)) {
            if(conversionTimer < 1200 && !((ZombieVillagerEntity)((Object)this)).isConverting()) return ActionResult.FAIL;

            itemStack.decrementUnlessCreative(1, player);
            if (!this.getEntityWorld().isClient()) {
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