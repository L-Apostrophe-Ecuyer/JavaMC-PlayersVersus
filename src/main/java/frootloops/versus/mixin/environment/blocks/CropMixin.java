package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.tags.ItemTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

import static net.minecraft.world.level.block.FarmlandBlock.MOISTURE;

@Mixin(CropBlock.class)
public abstract class CropMixin extends VegetationBlock {

    protected CropMixin(Properties settings) {
        super(settings);
    }

    @Override
    public boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos) {
        return (floor.is(Blocks.FARMLAND) || floor.is(CustomBlocks.BROWN_MUD));
    }

    @Shadow
    public int getMaxAge() {
        return 7;
    }

    @Override
    public void playerDestroy(ServerLevel world, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(world, player, pos, state, blockEntity, tool);

        int age = state.getValue(CropBlock.AGE);
        boolean isUsingHoe = tool.is(ItemTags.HOES);
        boolean doOnlyPartialHarvest = (age >= 3 && age > this.getMaxAge() - (isUsingHoe ? 3 : 1));
        if(doOnlyPartialHarvest) {
            world.setBlockAndUpdate(pos, state.setValue(CropBlock.AGE, 1));
        }
    }
}
