package frootloops.versus.mixin.items_and_effects.containers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Whether a shulker box's lid is free to open (vanilla's own check, before it opens one). */
@Mixin(ShulkerBoxBlock.class)
public interface ShulkerBoxBlockInvoker {

    @Invoker("canOpen")
    static boolean playersVersus$canOpen(BlockState state, Level level, BlockPos pos, ShulkerBoxBlockEntity blockEntity) {
        throw new AssertionError();
    }
}
