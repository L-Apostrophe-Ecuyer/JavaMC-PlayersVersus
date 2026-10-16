package frootloops.versus.mixin.items_and_effects;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;


@Mixin(PotionItem.class)
public abstract class PotionBottleUsageMixin extends Item {

    public PotionBottleUsageMixin(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        Player playerEntity = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();
        PotionContents potionContentsComponent = (PotionContents)itemStack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        BlockState blockState = world.getBlockState(blockPos);

        boolean canConvertToMud = blockState.is(BlockTags.CONVERTIBLE_TO_MUD);
        boolean canConvertToWetClay = !canConvertToMud && blockState.is(Blocks.CLAY);
        if (context.getClickedFace() != Direction.DOWN && potionContentsComponent.is(Potions.WATER) && (canConvertToMud || canConvertToWetClay)) {
            world.playSound((Player)null, blockPos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);
            playerEntity.setItemInHand(context.getHand(), ItemUtils.createFilledResult(itemStack, playerEntity, new ItemStack(Items.GLASS_BOTTLE)));
            playerEntity.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
            if (!world.isClientSide()) {
                ServerLevel serverWorld = (ServerLevel)world;

                for(int i = 0; i < 5; ++i) {
                    serverWorld.sendParticles(ParticleTypes.SPLASH, (double)blockPos.getX() + world.getRandom().nextDouble(), (double)(blockPos.getY() + 1), (double)blockPos.getZ() + world.getRandom().nextDouble(), 1, 0.0, 0.0, 0.0, 1.0);
                }
            }

            world.playSound((Player)null, blockPos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            world.gameEvent((Entity)null, GameEvent.FLUID_PLACE, blockPos);

            if(canConvertToMud) world.setBlockAndUpdate(blockPos, CustomBlocks.BROWN_MUD.defaultBlockState());
            else if(canConvertToWetClay) world.setBlockAndUpdate(blockPos, Blocks.MUD.defaultBlockState());
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }
}
