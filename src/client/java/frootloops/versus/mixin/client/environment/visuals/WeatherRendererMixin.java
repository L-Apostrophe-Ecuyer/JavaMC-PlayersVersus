package frootloops.versus.mixin.client.environment.visuals;

import frootloops.versus.VersusMod;
import frootloops.versus.VersusModServer;
import frootloops.versus.mod.environment.CustomSpecialEffects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightSourceView;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
@Mixin(WeatherRendering.class)
public abstract class WeatherRendererMixin {

    @Shadow private int field_53154;

    @Overwrite
    private Biome.Precipitation getPrecipitationAt(World world, BlockPos pos) {
        if (!world.getChunkManager().isChunkLoaded(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()))) {
            return Biome.Precipitation.NONE;
        }
        else if(world.getThunderGradient(1.0F) == 0.0F) {
            return Biome.Precipitation.NONE;
        }
        else {
            Biome biome = (Biome)world.getBiome(pos).value();
            return biome.getPrecipitation(pos, world.getSeaLevel());
        }
    }

    @Redirect(method = "renderPrecipitation", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getRainGradient(F)F"))
    private float getRainAmount(World world, float delta) {
        return world.getThunderGradient(delta);
    }

    @Overwrite
    public void addParticlesAndSound(ClientWorld world, Camera camera, int ticks, ParticlesMode particlesMode) {
        float amountOfFog = world.getRainGradient(1.0F);
        float amountOfRain = world.getThunderGradient(1.0F) / 2.0F;
        VersusMod.MOD_LOGGER.warn("[ CLIENT ]  Is Raining? " + world.isRaining() +  "  -  Thunder amount: " + amountOfRain  + "  -  Fog amount: " + amountOfFog);

        if(amountOfFog < 0.1f) return;

        BlockPos cameraPos = BlockPos.ofFloored(camera.getPos());
        Random random = Random.create((long)ticks * 312987231L);
        if(world.getLightLevel(LightType.SKY, cameraPos) > 1 && random.nextInt(384) < this.field_53154++) {
            this.field_53154 = 0;
            world.playSoundAtBlockCenter(cameraPos.up(3), CustomSpecialEffects.FOG_WIND_SOUND, SoundCategory.WEATHER, 0.2F, 1.0F, false);
        }

        if (amountOfRain > 0.1F) {
            BlockPos blockPos2 = null;

            int numParticles = (int)(100.0F * amountOfRain * amountOfRain) / (particlesMode == ParticlesMode.DECREASED ? 3 : 2);
            for(int j = 0; j < numParticles; ++j) {
                int k = random.nextInt(21) - 10;
                int l = random.nextInt(21) - 10;
                BlockPos heightmapPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, cameraPos.add(k, 0, l));
                if (heightmapPos.getY() > world.getBottomY() && heightmapPos.getY() <= cameraPos.getY() + 10 && heightmapPos.getY() >= cameraPos.getY() - 10 && this.getPrecipitationAt(world, heightmapPos) == Biome.Precipitation.RAIN) {
                    blockPos2 = heightmapPos.down();
                    if (particlesMode == ParticlesMode.MINIMAL) {
                        break;
                    }

                    double d = random.nextDouble();
                    double e = random.nextDouble();
                    BlockState blockState = world.getBlockState(blockPos2);
                    FluidState fluidState = world.getFluidState(blockPos2);
                    VoxelShape voxelShape = blockState.getCollisionShape(world, blockPos2);
                    double g = voxelShape.getEndingCoord(Direction.Axis.Y, d, e);
                    double h = (double)fluidState.getHeight(world, blockPos2);
                    double m = Math.max(g, h);
                    ParticleEffect particleEffect = !fluidState.isIn(FluidTags.LAVA) && !blockState.isOf(Blocks.MAGMA_BLOCK) && !CampfireBlock.isLitCampfire(blockState) ? ParticleTypes.RAIN : ParticleTypes.SMOKE;
                    world.addParticle(particleEffect, (double)blockPos2.getX() + d, (double)blockPos2.getY() + m, (double)blockPos2.getZ() + e, 0.0, 0.0, 0.0);
                }
            }

            if (blockPos2 != null && random.nextInt(6) < this.field_53154++) {
                this.field_53154 = 0;
                if (blockPos2.getY() > cameraPos.getY() + 1 && world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, cameraPos).getY() > MathHelper.floor((float)cameraPos.getY())) {
                    world.playSoundAtBlockCenter(blockPos2, SoundEvents.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, 0.075F, 0.5F, false);
                } else {
                    world.playSoundAtBlockCenter(blockPos2, SoundEvents.WEATHER_RAIN, SoundCategory.WEATHER, 0.1F, 1.0F, false);
                }
            }
        }
    }
}
