package frootloops.versus.mixin.client.environment.visuals;

import frootloops.versus.VersusMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Environment(EnvType.CLIENT)
@Mixin(ClientLevel.class)
public abstract class ClientWorldSkyMixin extends Level {
    protected ClientWorldSkyMixin(WritableLevelData properties, ResourceKey<Level> registryRef, RegistryAccess registryManager, Holder<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    @ModifyConstant(method = "getSkyDarken(F)F", constant = @Constant(floatValue = 16.0F))
    private static float lessDepressingWeather(float f) {
        return 24.0f;
    }

    @ModifyVariable(method = "getSkyColor", at = @At("STORE"), ordinal = 3)
    private float moreColorfulSkyDuringRain(float rainGradient) {return rainGradient/1.25f;}

    @Override
    public boolean isRainingAt(BlockPos pos) {
        if (!this.isThundering()) return false;
        else return super.isRainingAt(pos);
    }
}
