package frootloops.versus.mixin.environment.worldgen;

import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The splitter {@link NoiseConfig} seeds its router with, which it gives no getter for: {@code AquiferInputs.seeding}
 * seeds the 3D base noise ({@code old_blended_noise}) from it as NoiseConfig does, so the aquifer's copies of the
 * terrain give the router's values.
 */
@Mixin(NoiseConfig.class)
public interface NoiseConfigAccessor {

    @Accessor("randomDeriver")
    RandomSplitter playersVersus$randomDeriver();
}
