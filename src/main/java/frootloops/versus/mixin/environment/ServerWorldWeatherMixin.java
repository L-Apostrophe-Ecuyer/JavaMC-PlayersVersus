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
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.ServerLevelData;
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

    @Shadow
    private final ServerLevelData serverLevelData;

    protected ServerWorldWeatherMixin(WritableLevelData properties, ResourceKey<Level> registryRef, RegistryAccess registryManager, Holder<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates, MinecraftServer server, ServerLevelData worldProperties) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
        this.server = server;
        this.serverLevelData = worldProperties;
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
            if (this.serverLevelData.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) {
                int clearWeatherTime = this.serverLevelData.getClearWeatherTime();
                int thunderTime = this.serverLevelData.getThunderTime();
                int rainTime = this.serverLevelData.getRainTime();
                boolean isThundering = this.levelData.isThundering();
                boolean isRaining = this.levelData.isRaining();


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

                this.serverLevelData.setThunderTime(thunderTime);
                this.serverLevelData.setRainTime(rainTime);
                this.serverLevelData.setClearWeatherTime(clearWeatherTime);
                this.serverLevelData.setThundering(isThundering);
                this.serverLevelData.setRaining(isRaining);
            }

            this.oThunderLevel = this.thunderLevel;
            if (this.levelData.isThundering()) this.thunderLevel += 0.0025F;
            else this.thunderLevel -= 0.0025F;
            this.thunderLevel = Mth.clamp(this.thunderLevel, 0.0F, 1.0F);

            this.oRainLevel = this.rainLevel;
            if (this.levelData.isRaining()) this.rainLevel += 0.0025F;
            else this.rainLevel -= 0.0025F;
            this.rainLevel = Mth.clamp(this.rainLevel, 0.0F, 1.0F);
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
