package frootloops.versus.mixin.environment.blocks;

import frootloops.versus.mod.environment.blocks.sulfur.SulfurSpikes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SpeleothemBlock;
import net.minecraft.world.level.block.SulfurSpikeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Landing on a sulfur spike: vanilla gives the fall no spike damage of its own (pointed dripstone and icicles have
 * theirs), so the fall's hurt is the spike's, and it poisons and nauseates (SulfurSpikes) by what was taken.
 */
@Mixin(SulfurSpikeBlock.class)
public abstract class SulfurSpikeBlockMixin extends SpeleothemBlock {

    protected SulfurSpikeBlockMixin(BlockState blockToGrowOn, Properties properties) {
        super(blockToGrowOn, properties);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (!(level instanceof ServerLevel) || !(entity instanceof LivingEntity living)) {
            super.fallOn(level, state, pos, entity, fallDistance);
            return;
        }
        float before = living.getHealth() + living.getAbsorptionAmount();
        super.fallOn(level, state, pos, entity, fallDistance);
        float dealt = before - living.getHealth() - living.getAbsorptionAmount();
        if (dealt > 0.0F && living.isAlive()) SulfurSpikes.afflict(living, dealt);
    }
}
