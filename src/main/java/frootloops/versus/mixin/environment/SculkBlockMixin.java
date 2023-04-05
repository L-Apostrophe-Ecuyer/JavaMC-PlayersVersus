package frootloops.versus.mixin.environment;

import net.minecraft.block.BlockState;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.block.SculkBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SculkBlock.class)
public class SculkBlockMixin extends ExperienceDroppingBlock {

    public SculkBlockMixin(Settings settings) {
        super(settings);
    }

    final IntProvider moreXp =  UniformIntProvider.create(1, 2);

    @Override
    public void onStacksDropped(BlockState state, ServerWorld world, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.onStacksDropped(state, world, pos, tool, dropExperience);
        if (dropExperience) {
            this.dropExperienceWhenMined(world, pos, tool, moreXp);
        }
    }
}
