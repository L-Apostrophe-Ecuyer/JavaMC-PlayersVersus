package frootloops.versus.mixin.environment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerLevel.class)
public abstract class ServerWorldWeatherMixin extends Level {

    private static final IntProvider CLEAR_WEATHER_DURATION_PROVIDER = UniformInt.of(24000, 72000);
    private static final IntProvider RAIN_WEATHER_DURATION_PROVIDER = UniformInt.of(3000, 9000);
    private static final IntProvider CLEAR_THUNDER_WEATHER_DURATION_PROVIDER = UniformInt.of(36000, 120000);
    private static final IntProvider THUNDER_WEATHER_DURATION_PROVIDER = UniformInt.of(1000, 6000);

    @Shadow
    private final MinecraftServer server;

    // Since 26.1 the weather is the server's saved data, and the game rules are no longer part of the level data.
    @Shadow
    public abstract WeatherData getWeatherData();

    @Shadow
    public abstract GameRules getGameRules();

    protected ServerWorldWeatherMixin(WritableLevelData properties, ResourceKey<Level> registryRef, RegistryAccess registryManager, Holder<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, int maxChainedNeighborUpdates, MinecraftServer server) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, maxChainedNeighborUpdates);
        this.server = server;
    }

    @Override
    public boolean isRainingAt(BlockPos pos) {
        if (!this.isThundering()) return false;
        else return super.isRainingAt(pos);
    }

    @Overwrite
    private void advanceWeatherCycle() {
        boolean isWorldRaining = this.isRaining();
        if (this.dimensionType().hasSkyLight()) {
            WeatherData weather = this.getWeatherData();
            if (this.getGameRules().get(GameRules.ADVANCE_WEATHER)) {
                int clearWeatherTime = weather.getClearWeatherTime();
                int thunderTime = weather.getThunderTime();
                int rainTime = weather.getRainTime();
                boolean isThundering = weather.isThundering();
                boolean isRaining = weather.isRaining();


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
                                THUNDER_WEATHER_DURATION_PROVIDER.sample(this.random) :
                                CLEAR_THUNDER_WEATHER_DURATION_PROVIDER.sample(this.random);
                    }

                    if (rainTime > 0) {
                        if (--rainTime == 0) isRaining = !isRaining;
                    } else {
                        rainTime = isRaining ?
                                RAIN_WEATHER_DURATION_PROVIDER.sample(this.random) :
                                CLEAR_WEATHER_DURATION_PROVIDER.sample(this.random);
                    }
                }

                weather.setThunderTime(thunderTime);
                weather.setRainTime(rainTime);
                weather.setClearWeatherTime(clearWeatherTime);
                weather.setThundering(isThundering);
                weather.setRaining(isRaining);
                weather.setDirty();
            }

            this.oThunderLevel = this.thunderLevel;
            if (weather.isThundering()) this.thunderLevel += 0.0025F;
            else this.thunderLevel -= 0.0025F;
            this.thunderLevel = Math.clamp(this.thunderLevel, 0.0F, 1.0F);

            this.oRainLevel = this.rainLevel;
            if (weather.isRaining()) this.rainLevel += 0.0025F;
            else this.rainLevel -= 0.0025F;
            this.rainLevel = Math.clamp(this.rainLevel, 0.0F, 1.0F);
        }

        if (this.oRainLevel != this.rainLevel) {
            this.server.getPlayerList().broadcastAll(
                    new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.rainLevel), this.dimension()
            );
        }

        if (this.oThunderLevel != this.thunderLevel) {
            this.server.getPlayerList().broadcastAll(
                    new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, this.thunderLevel), this.dimension()
            );
        }

        if (isWorldRaining != this.isRaining()) {
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(isWorldRaining ? ClientboundGameEventPacket.STOP_RAINING : ClientboundGameEventPacket.START_RAINING, ClientboundGameEventPacket.DEMO_PARAM_INTRO));
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.rainLevel));
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, this.thunderLevel));
        }
    }
}
