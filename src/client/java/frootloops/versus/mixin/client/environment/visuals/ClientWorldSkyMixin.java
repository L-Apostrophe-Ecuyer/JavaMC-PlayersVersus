package frootloops.versus.mixin.client.environment.visuals;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Environment(EnvType.CLIENT)
@Mixin(ClientWorld.class)
public abstract class ClientWorldSkyMixin extends World {
    protected ClientWorldSkyMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    @ModifyConstant(method = "getSkyBrightness", constant = @Constant(floatValue = 16.0F))
    private static float lessDepressingWeather(float f) {
        return 24.0f;
    }

    @ModifyVariable(method = "getSkyColor", at = @At("STORE"), ordinal = 3)
    private float moreColorfulSkyDuringRain(float rainGradient) {return rainGradient/1.25f;}

    @Override
    public boolean hasRain(BlockPos pos) {
        if (!this.isThundering()) return false;
        else return super.hasRain(pos);
    }
}
