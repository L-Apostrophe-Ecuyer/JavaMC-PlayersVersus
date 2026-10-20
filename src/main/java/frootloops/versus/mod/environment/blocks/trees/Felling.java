package frootloops.versus.mod.environment.blocks.trees;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** How a tree falls (a stub until the tests are seen to fail). */
public final class Felling {

    private Felling() {
    }

    public record Fall(Set<BlockPos> blocks, Set<BlockPos> crown, Direction direction) {}

    public static List<Fall> afterCut(LevelReader level, BlockPos cut, @Nullable Vec3 cutter, RandomSource random) {
        return List.of();
    }

    public static double cutTimeFactor(LevelReader level, BlockPos pos) {
        return 1.0;
    }
}
