/*
 * Decompiled with CFR 0.2.2 (FabricMC 7c48b8c4).
 */
package frootloops.versus.mod.environment.worldgen;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.jetbrains.annotations.Nullable;

public class SimpleWaterAquifer implements AquiferSampler {

    private final FluidLevelSampler fluidLevelSampler;
    private final ChunkNoiseSampler chunkNoiseSampler;
    private final DensityFunction fluidLevelFloodednessFunction;
    private final DensityFunction fluidLevelSpreadFunction;
    private final DensityFunction continentalnessDensityFunction;
    private final DensityFunction depthDensityFunction;
    private final DensityFunction ridgesDensityFunction;

    private final static int SEA_LEVEL = 64;
    private final static int MIN_Y_LEVEL_FOR_WATERFALLS = SEA_LEVEL + 16;

    private final static double MAX_DENSITY_FOR_WATERFALLS = 0.05;

    private int fluidLevelY;

    private boolean needsFluidTick;

    public SimpleWaterAquifer(ChunkNoiseSampler chunkNoiseSampler, ChunkPos chunkPos, NoiseRouter noiseRouter, RandomSplitter randomSplitter, int minimumY, int height, FluidLevelSampler fluidLevelSampler) {

        // Constants:
        int fluidLevelBottomY = 22;
        int fluidLevelsPerBlocksY = 6;

        // Set noise and density samplers:
        this.fluidLevelSampler = fluidLevelSampler;
        this.chunkNoiseSampler = chunkNoiseSampler;
        this.fluidLevelFloodednessFunction = noiseRouter.fluidLevelFloodednessNoise();
        this.fluidLevelSpreadFunction = noiseRouter.fluidLevelSpreadNoise();
        this.continentalnessDensityFunction = noiseRouter.continents();
        this.depthDensityFunction = noiseRouter.depth();
        this.ridgesDensityFunction = noiseRouter.ridges();

        // Fluid tick:
        this.needsFluidTick = false;

        // Get the Y level for the water height in this chunk:
        int x = chunkPos.x;
        int z = chunkPos.z;
        double fluidFloodedness = fluidLevelFloodednessFunction.sample( new DensityFunction.UnblendedNoisePos(x, 0, z));
        this.fluidLevelY = (int)(8.0 * fluidFloodedness) * fluidLevelsPerBlocksY + fluidLevelBottomY;

        // To make sure that aquifers at cave bottoms link together:
        if(fluidLevelY == fluidLevelBottomY + fluidLevelsPerBlocksY) fluidLevelY = fluidLevelBottomY;

        // To make sure that aquifers near sea level link together, and possibly make waterfalls:
        else if(fluidLevelY >= SEA_LEVEL - 2 * fluidLevelsPerBlocksY) fluidLevelY = SEA_LEVEL + fluidLevelsPerBlocksY;

        //VersusMod.MOD_LOGGER.warn("WATER LEVEL for Chunk " + chunkPos.toString() + " is " + fluidLevelY);
    }

    @Override
    @Nullable
    public BlockState apply(DensityFunction.NoisePos pos, double density) {
        if (density > MAX_DENSITY_FOR_WATERFALLS) {
            this.needsFluidTick = false;
            return null;
        }

        int x = pos.blockX();
        int y = pos.blockY();
        int z = pos.blockZ();
        if (y < MIN_Y_LEVEL_FOR_WATERFALLS && density > 0.0) {
            this.needsFluidTick = false;
            return null;
        }

        /*
        ------------------------------------------------------------------------------
            1. LAVA
            If lava level is > y, return lava
         */
        FluidLevel fluidLevel = this.fluidLevelSampler.getFluidLevel(x, y, z);
        if (fluidLevel.getBlockState(y).isOf(Blocks.LAVA)) {
            this.needsFluidTick = false;
            return Blocks.LAVA.getDefaultState();
        }

        // Get the depth: when positive, the block pos is below the terrain's surface (in a cave). When negative, it's above.
        //double depth = depthDensityFunction.sample(pos);
        //boolean isInCave = depth > 0.065;

        /*
        ------------------------------------------------------------------------------
            2. OCEANS AND RIVERS
            if y is near sea level, and continentalness/ridges tell us that we're near water, then flood
        */
        boolean isNearSeaLevel = (y > -32 && y < MIN_Y_LEVEL_FOR_WATERFALLS);
        if(isNearSeaLevel) {

            // Anything above sea level here should stay air:
            if(y > SEA_LEVEL - 1) return Blocks.AIR.getDefaultState();

            // Antyhing else follows the floodedness function:
            double seaLevelFloodedness = this.fluidLevelFloodednessFunction.sample(pos);

            // Set the noise thresholds for when floodedness turns into a stone barrier, versus when it's water
            double maxFloodednessForBarrier = (y < 48 ? 0.34 : 0.34 - (double)(y - 48) * 0.0065);
            double minFloodednessForBarrier = (y < 56 ? 0.0001 : (double)(y - 56) * 0.0285);

            if(seaLevelFloodedness > maxFloodednessForBarrier) {
                this.needsFluidTick = (seaLevelFloodedness < maxFloodednessForBarrier + 0.2);
                return Blocks.WATER.getDefaultState();
            }
            else if(seaLevelFloodedness > minFloodednessForBarrier){
                return Blocks.STONE.getDefaultState();
            }
        }


        /*
        ------------------------------------------------------------------------------
            3. CAVE BASINS
            If we're in a cave, we should check aquifer floodedness noise to see what the local water height should be
        */
        if(y > -4 && y < 32) {
            double caveBasinFloodedness = this.fluidLevelSpreadFunction.sample(pos);
            double maxFloodednessForBarrier = (y > 8 ? 0.5 : 0.5 - (double)(8 - y) * 0.08);//(y < 21 ? (y > 0 ? 0.4 : 0.4 - (double) (8 - y) * 0.03) : 0.4 - (double) (y - 21) * 0.02);
            double minFloodednessForBarrier = (y < 12 ? 0.0001 : (double)(y - 12) * 0.06);
            if (caveBasinFloodedness > maxFloodednessForBarrier) {
                this.needsFluidTick = (caveBasinFloodedness < maxFloodednessForBarrier + 0.2) || density < 0.08;
                return Blocks.WATER.getDefaultState();
            } else if (caveBasinFloodedness > minFloodednessForBarrier && y < 23) {
                return Blocks.STONE.getDefaultState();
            }
        }

        /*
        ------------------------------------------------------------------------------
            4. WATERFALLS
            If y is higher up, we can create water bodies to generate mountain lakes and waterfalls
        */
        if(y > MIN_Y_LEVEL_FOR_WATERFALLS) {

        }

        // Finally, if no match for aquifer flooding, return air:
        return Blocks.AIR.getDefaultState();
    }

    @Override
    public boolean needsFluidTick() {
        return this.needsFluidTick;
    }

    private int getLocalX(int x) {
        return Math.floorDiv(x, 16);
    }

    private int getLocalY(int y) {
        return Math.floorDiv(y, 12);
    }

    private int getLocalZ(int z) {
        return Math.floorDiv(z, 16);
    }
}

