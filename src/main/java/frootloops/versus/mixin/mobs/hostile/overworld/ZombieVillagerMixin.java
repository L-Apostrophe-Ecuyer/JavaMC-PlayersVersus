package frootloops.versus.mixin.mobs.hostile.overworld;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(ZombieVillager.class)
public abstract class ZombieVillagerMixin extends Zombie {

    public ZombieVillagerMixin(EntityType<? extends Zombie> entityType, Level world) {
        super(entityType, world);
    }

    @Shadow private int villagerConversionTime;

    @Shadow
    private void startConverting(@Nullable UUID uuid, int delay) {}

    @Override
    public boolean checkSpawnRules(LevelAccessor world, EntitySpawnReason spawnReason) {
        if(spawnReason != EntitySpawnReason.NATURAL) return super.checkSpawnRules(world, spawnReason);
        BlockPos pos = this.blockPosition();
        if(pos.getY() < 56) return false;
        if(this.getWalkTargetValue(pos, world) >= 0.0F) {
            BlockState downState = world.getBlockState(pos.below());
            return downState.is(BlockTags.WOLVES_SPAWNABLE_ON) || downState.is(BlockTags.SAND);
        }
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (itemStack.is(Items.GOLDEN_APPLE)) {
            if(villagerConversionTime < 1200 && !((ZombieVillager)((Object)this)).isConverting()) return InteractionResult.FAIL;

            itemStack.consume(1, player);
            if (!this.level().isClientSide()) {
                int conversionTime = (villagerConversionTime > 0) ? (villagerConversionTime - 600 - this.random.nextInt(200)) : this.random.nextInt(2400) + 1200;
                this.startConverting(player.getUUID(), conversionTime);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance localDifficulty) {
        return;
    }
}