package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

import static net.minecraft.block.FarmlandBlock.MOISTURE;

@Mixin(CropBlock.class)
public abstract class CropMixin extends PlantBlock {

    protected CropMixin(Settings settings) {
        super(settings);
    }

    @Override
    public boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        return (floor.isOf(Blocks.FARMLAND) || floor.isOf(CustomBlocks.BROWN_MUD));
    }

    @Shadow
    public int getMaxAge() {
        return 7;
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.afterBreak(world, player, pos, state, blockEntity, tool);

        int age = state.get(CropBlock.AGE);
        boolean isUsingHoe = tool.getItem() instanceof HoeItem;
        boolean doOnlyPartialHarvest = (age >= 3 && age > this.getMaxAge() - (isUsingHoe ? 3 : 1));
        if(doOnlyPartialHarvest) {
            world.setBlockState(pos, state.with(CropBlock.AGE, 1));
        }
    }
}
