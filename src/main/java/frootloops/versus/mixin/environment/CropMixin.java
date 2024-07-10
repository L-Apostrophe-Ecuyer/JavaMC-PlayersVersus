package frootloops.versus.mixin.environment;

import frootloops.versus.mod.environment.CustomBlocks;
import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockView;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
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
        return (floor.getBlock() instanceof FarmlandBlock);
    }
}
