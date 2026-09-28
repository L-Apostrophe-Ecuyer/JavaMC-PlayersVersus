package frootloops.versus.mod.environment.worldgen;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Vanilla's terrain pass over one chunk, driven the way {@code NoiseChunkGenerator.populateNoise} drives it: a
 * {@link ChunkNoiseSampler} (so this mod's mixin gives it its aquifer), then its interpolation loop (x cells; in each,
 * z cells, then y cells from the top; the blocks of a cell in the same order) with a callback at every block, where
 * the position is the sampler itself, as for the final density and the aquifer in the game.
 *
 * <p>Other functions can join the pass ({@link #register}): they go through the sampler's own wrapping, so their
 * {@code interpolated}, {@code flat_cache} and {@code cache_once} parts behave as in the chunk's router.
 *
 * <p>It calls the sampler's constructor and loop methods reflectively, by their Yarn names (tests run on the named
 * game jar), so it doesn't depend on their access modifiers, which the mappings don't record. No blending, no
 * structures (vanilla's beardifier marker, found the same way).
 */
public final class TerrainPass {

    /** Called at every block of the pass; {@code pos} is the sampler, valid until the callback returns. */
    @FunctionalInterface
    public interface BlockVisitor {
        void visit(int x, int y, int z, DensityFunction.NoisePos pos);
    }

    private static final Constructor<?> CONSTRUCTOR = constructor();
    private static final Object NO_BEARDIFIER = noBeardifier();
    private static final Method GET_ACTUAL_DENSITY_FUNCTION = method("getActualDensityFunction", DensityFunction.class);
    private static final Method GET_AQUIFER_SAMPLER = method("getAquiferSampler");
    private static final Method SAMPLE_START_DENSITY = method("sampleStartDensity");
    private static final Method SAMPLE_END_DENSITY = method("sampleEndDensity", int.class);
    private static final Method ON_SAMPLED_CELL_CORNERS = method("onSampledCellCorners", int.class, int.class);
    private static final Method INTERPOLATE_Y = method("interpolateY", int.class, double.class);
    private static final Method INTERPOLATE_X = method("interpolateX", int.class, double.class);
    private static final Method INTERPOLATE_Z = method("interpolateZ", int.class, double.class);
    private static final Method SWAP_BUFFERS = method("swapBuffers");
    private static final Method STOP_INTERPOLATION = method("stopInterpolation");

    private final ChunkNoiseSampler sampler;
    private final ChunkPos chunk;
    private final GenerationShapeConfig shape;
    private boolean ran;

    public TerrainPass(NoiseConfig config, ChunkGeneratorSettings settings, ChunkPos chunk, AquiferSampler.FluidLevelSampler fluidLevels) {
        this.chunk = chunk;
        this.shape = settings.generationShapeConfig();
        try {
            this.sampler = (ChunkNoiseSampler) CONSTRUCTOR.newInstance(16 / this.shape.horizontalCellBlockCount(), config,
                    chunk.getStartX(), chunk.getStartZ(), this.shape, NO_BEARDIFIER, settings, fluidLevels, Blender.getNoBlending());
        } catch (InvocationTargetException exception) {
            throw new IllegalStateException("the chunk noise sampler failed", exception.getCause());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** The aquifer the sampler's constructor made. */
    public AquiferSampler aquifer() {
        return (AquiferSampler) this.invoke(GET_AQUIFER_SAMPLER);
    }

    /** {@code function} as the sampler wraps its router's functions; call before {@link #run}. */
    public DensityFunction register(DensityFunction function) {
        if (this.ran) throw new IllegalStateException("register functions before the pass runs");
        return function.apply(part -> (DensityFunction) this.invoke(GET_ACTUAL_DENSITY_FUNCTION, part));
    }

    /** Runs the pass once, over every block of the chunk's noise height. */
    public void run(BlockVisitor visitor) {
        if (this.ran) throw new IllegalStateException("a pass runs once");
        this.ran = true;
        int cellWidth = this.shape.horizontalCellBlockCount(), cellHeight = this.shape.verticalCellBlockCount();
        int cells = 16 / cellWidth;
        int minCellY = Math.floorDiv(this.shape.minimumY(), cellHeight), cellCountY = Math.floorDiv(this.shape.height(), cellHeight);
        DensityFunction.NoisePos pos = (DensityFunction.NoisePos) this.sampler;
        this.invoke(SAMPLE_START_DENSITY);
        for (int cellX = 0; cellX < cells; cellX++) {
            this.invoke(SAMPLE_END_DENSITY, cellX);
            for (int cellZ = 0; cellZ < cells; cellZ++) {
                for (int cellY = cellCountY - 1; cellY >= 0; cellY--) {
                    this.invoke(ON_SAMPLED_CELL_CORNERS, cellY, cellZ);
                    for (int localY = cellHeight - 1; localY >= 0; localY--) {
                        int y = (minCellY + cellY) * cellHeight + localY;
                        this.invoke(INTERPOLATE_Y, y, (double) localY / cellHeight);
                        for (int localX = 0; localX < cellWidth; localX++) {
                            int x = this.chunk.getStartX() + cellX * cellWidth + localX;
                            this.invoke(INTERPOLATE_X, x, (double) localX / cellWidth);
                            for (int localZ = 0; localZ < cellWidth; localZ++) {
                                int z = this.chunk.getStartZ() + cellZ * cellWidth + localZ;
                                this.invoke(INTERPOLATE_Z, z, (double) localZ / cellWidth);
                                visitor.visit(x, y, z, pos);
                            }
                        }
                    }
                }
            }
            this.invoke(SWAP_BUFFERS);
        }
        this.invoke(STOP_INTERPOLATION);
    }

    private Object invoke(Method method, Object... arguments) {
        try {
            return method.invoke(this.sampler, arguments);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof RuntimeException runtime) throw runtime;
            if (exception.getCause() instanceof Error error) throw error;
            throw new IllegalStateException(exception.getCause());
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Constructor<?> constructor() {
        Constructor<?> constructor = Arrays.stream(ChunkNoiseSampler.class.getDeclaredConstructors())
                .filter(candidate -> candidate.getParameterCount() == 9).findFirst()
                .orElseThrow(() -> new IllegalStateException("no 9-argument ChunkNoiseSampler constructor: "
                        + Arrays.toString(ChunkNoiseSampler.class.getDeclaredConstructors())));
        constructor.setAccessible(true);
        return constructor;
    }

    /** Vanilla's "no structures" beardifier, an enum constant of a class that isn't public. */
    private static Object noBeardifier() {
        Class<?> beardifying = constructor().getParameterTypes()[5];
        return Arrays.stream(DensityFunctionTypes.class.getDeclaredClasses())
                .filter(type -> type.isEnum() && beardifying.isAssignableFrom(type))
                .map(type -> type.getEnumConstants()[0]).findFirst()
                .orElseThrow(() -> new IllegalStateException("no beardifier marker implementing " + beardifying));
    }

    private static Method method(String name, Class<?>... parameters) {
        try {
            Method method = ChunkNoiseSampler.class.getDeclaredMethod(name, parameters);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("ChunkNoiseSampler has no " + name + Arrays.toString(parameters) + "; it has "
                    + Arrays.stream(ChunkNoiseSampler.class.getDeclaredMethods()).map(Method::getName).sorted().collect(Collectors.joining(", ")),
                    exception);
        }
    }
}
