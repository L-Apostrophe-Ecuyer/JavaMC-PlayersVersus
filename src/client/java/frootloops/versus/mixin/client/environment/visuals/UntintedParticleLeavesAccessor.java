package frootloops.versus.mixin.client.environment.visuals;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The falling-leaf particle of leaves that aren't tinted by their biome (cherry, pale oak, poplars), for LeavesRustling. */
@Mixin(UntintedParticleLeavesBlock.class)
public interface UntintedParticleLeavesAccessor {
    @Accessor("leafParticle")
    ParticleOptions versus$getLeafParticle();
}
