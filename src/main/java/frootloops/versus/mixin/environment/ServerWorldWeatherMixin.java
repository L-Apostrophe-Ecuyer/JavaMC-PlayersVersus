package frootloops.versus.mixin.environment;

import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.ServerWorldProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerWorld.class)
public abstract class ServerWorldWeatherMixin extends World {

    private static final IntProvider CLEAR_WEATHER_DURATION_PROVIDER = UniformIntProvider.create(24000, 72000);
    private static final IntProvider RAIN_WEATHER_DURATION_PROVIDER = UniformIntProvider.create(3000, 9000);
    private static final IntProvider CLEAR_THUNDER_WEATHER_DURATION_PROVIDER = UniformIntProvider.create(36000, 120000);
    private static final IntProvider THUNDER_WEATHER_DURATION_PROVIDER = UniformIntProvider.create(1000, 6000);

    @Shadow
    private final MinecraftServer server;

    @Shadow
    private final ServerWorldProperties worldProperties;

    protected ServerWorldWeatherMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates, MinecraftServer server, ServerWorldProperties worldProperties) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
        this.server = server;
        this.worldProperties = worldProperties;
    }

    @Override
    public boolean hasRain(BlockPos pos) {
        if (!this.isThundering()) {
            return false;
        } else return super.hasRain(pos);
    }

    @Overwrite
    private void tickWeather() {
        boolean isWorldRaining = this.isRaining();
        if (this.getDimension().hasSkyLight()) {
            if (this.worldProperties.getGameRules().getBoolean(GameRules.DO_WEATHER_CYCLE)) {
                int clearWeatherTime = this.worldProperties.getClearWeatherTime();
                int thunderTime = this.worldProperties.getThunderTime();
                int rainTime = this.worldProperties.getRainTime();
                boolean isThundering = this.properties.isThundering();
                boolean isRaining = this.properties.isRaining();


                if (clearWeatherTime > 0) {
                    clearWeatherTime--;
                    thunderTime = isThundering ? 0 : 1;
                    rainTime = isRaining ? 0 : 1;
                    isThundering = false;
                    isRaining = false;


                } else {
                    if (thunderTime > 0) {
                        if (--thunderTime == 0) isThundering = !isThundering;
                    } else {
                        thunderTime = isThundering ?
                                THUNDER_WEATHER_DURATION_PROVIDER.get(this.random) :
                                CLEAR_THUNDER_WEATHER_DURATION_PROVIDER.get(this.random);
                    }

                    if (rainTime > 0) {
                        if (--rainTime == 0) isRaining = !isRaining;
                    } else {
                        rainTime = isRaining ?
                                RAIN_WEATHER_DURATION_PROVIDER.get(this.random) :
                                CLEAR_WEATHER_DURATION_PROVIDER.get(this.random);
                    }
                }

                this.worldProperties.setThunderTime(thunderTime);
                this.worldProperties.setRainTime(rainTime);
                this.worldProperties.setClearWeatherTime(clearWeatherTime);
                this.worldProperties.setThundering(isThundering);
                this.worldProperties.setRaining(isRaining);
            }

            this.thunderGradientPrev = this.thunderGradient;
            if (this.properties.isThundering()) this.thunderGradient += 0.0025F;
            else this.thunderGradient -= 0.0025F;
            this.thunderGradient = MathHelper.clamp(this.thunderGradient, 0.0F, 1.0F);

            this.rainGradientPrev = this.rainGradient;
            if (this.properties.isRaining()) this.rainGradient += 0.0025F;
            else this.rainGradient -= 0.0025F;
            this.rainGradient = MathHelper.clamp(this.rainGradient, 0.0F, 1.0F);
        }

        if (this.rainGradientPrev != this.rainGradient) {
            this.server.getPlayerManager().sendToDimension(
                    new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, this.rainGradient), this.getRegistryKey()
            );
        }

        if (this.thunderGradientPrev != this.thunderGradient) {
            this.server.getPlayerManager().sendToDimension(
                    new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, this.thunderGradient), this.getRegistryKey()
            );
        }

        if (isWorldRaining != this.isRaining()) {
            this.server.getPlayerManager().sendToAll(new GameStateChangeS2CPacket(isWorldRaining ? GameStateChangeS2CPacket.RAIN_STOPPED : GameStateChangeS2CPacket.RAIN_STARTED, GameStateChangeS2CPacket.DEMO_OPEN_SCREEN));
            this.server.getPlayerManager().sendToAll(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED, this.rainGradient));
            this.server.getPlayerManager().sendToAll(new GameStateChangeS2CPacket(GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED, this.thunderGradient));
        }
    }
}
