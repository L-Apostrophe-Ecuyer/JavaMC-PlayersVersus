package frootloops.versus.mod.environment.worldgen.terrain;

import frootloops.versus.mod.environment.worldgen.WorldgenTestData;
import java.util.Random;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import org.junit.jupiter.api.Test;

/** Throwaway: the climate's shares and the terrain's tops by climate. */
class TerrainScratch {

    static int top(RandomState state, NoiseRouter router, int x, int z) {
        for (int y = 255; y > 0; y -= 2) if (state.sampleBlockValueUncached(router.finalDensity(), x, y, z) > 0) return y;
        return 0;
    }

    @Test
    void bandTops() {
        RandomState state = WorldgenTestData.randomState(1234L);
        NoiseRouter router = WorldgenTestData.router();
        Random random = new Random(3);
        int n = 0;
        int[] tops = new int[1500];
        long overhang = 0;
        for (int tries = 0; tries < 2000000 && n < tops.length; tries++) {
            int x = random.nextInt(60000) - 30000, z = random.nextInt(60000) - 30000;
            double c = state.sampleBlockValueUncached(router.continents(), x, 0, z), e = state.sampleBlockValueUncached(router.erosion(), x, 0, z);
            if (c < 0.3 || e < 0.47 || e >= 0.53) continue;
            tops[n++] = top(state, router, x, z);
        }
        java.util.Arrays.sort(tops, 0, n);
        int above128 = 0, above160 = 0;
        for (int i = 0; i < n; i++) { if (tops[i] > 128) above128++; if (tops[i] > 160) above160++; }
        System.out.printf("[scratch] bump band (E 0.47..0.53, inland): n %d, top p10 %d p50 %d p90 %d p99 %d max %d; above 128: %d, above 160: %d%n", n, tops[n / 10], tops[n / 2], tops[n * 9 / 10], tops[n * 99 / 100], tops[n - 1], above128, above160);
    }

    @Test
    void tops() {
        RandomState state = WorldgenTestData.randomState(1234L);
        NoiseRouter router = WorldgenTestData.router();
        Random random = new Random(2);
        String[] names = {"E -0.1..0.1", "E 0.2..0.3", "E 0.4..0.44", "E 0.46..0.54 (bump)", "E 0.56..0.6", "E 0.65..0.9"};
        double[][] ranges = {{-0.1, 0.1}, {0.2, 0.3}, {0.4, 0.44}, {0.46, 0.54}, {0.56, 0.6}, {0.65, 0.9}};
        int[][] tops = new int[names.length][];
        int[] count = new int[names.length];
        for (int k = 0; k < names.length; k++) tops[k] = new int[60];
        for (int tries = 0; tries < 200000 && java.util.Arrays.stream(count).min().getAsInt() < 60; tries++) {
            int x = random.nextInt(60000) - 30000, z = random.nextInt(60000) - 30000;
            double c = state.sampleBlockValueUncached(router.continents(), x, 0, z), e = state.sampleBlockValueUncached(router.erosion(), x, 0, z);
            if (c < 0.3) continue;
            for (int k = 0; k < names.length; k++) {
                if (e >= ranges[k][0] && e < ranges[k][1] && count[k] < 60) {
                    tops[k][count[k]++] = top(state, router, x, z);
                }
            }
        }
        for (int k = 0; k < names.length; k++) {
            int[] t = java.util.Arrays.copyOf(tops[k], count[k]);
            java.util.Arrays.sort(t);
            System.out.printf("[scratch] %-20s n %d: top min %d, median %d, max %d%n", names[k], t.length, t[0], t[t.length / 2], t[t.length - 1]);
        }
    }
}
