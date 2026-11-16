package frootloops.versus.gametest;

import frootloops.versus.VersusMod;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PotentSulfurBlock;
import net.minecraft.world.level.block.SpeleothemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.PotentSulfurState;
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Sulfur: every sulfur pool has many geysers on its floor, potent sulfur with a magma block beneath; sulfur spikes
 * poison and nauseate what they hurt.
 */
public class SulfurGameTest {

    // 48 x 40 x 48, a grass floor, with sky access
    private static final String GROVE = "players-versus-gametest:leaves_grove";
    private static final int SEEDS = 8, FROM = 14, TO = 34, TOP = 12;

    /** A slab of sulfur from the floor up to TOP, around the grove's middle (24, 24), with air over it. */
    private static void slab(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (BlockPos pos : BlockPos.betweenClosed(FROM, 1, FROM, TO, TOP + 8, TO)) {
            BlockState want = pos.getY() <= TOP ? Blocks.SULFUR.defaultBlockState() : Blocks.AIR.defaultBlockState();
            level.setBlock(helper.absolutePos(pos), want, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    /**
     * A sulfur pool always has many geysers on its floor (the solid blocks under its water): a third of it at least, and
     * never fewer than six where it has as many, each potent sulfur with a magma block beneath, dormant till it erupts.
     * Grown on a slab of sulfur, as the sulfur caves' floors are, over several seeds.
     */
    @GameTest(structure = GROVE, maxTicks = 40, skyAccess = true)
    public void everySulfurPoolHasManyGeysersOnItsFloor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Feature pool = level.registryAccess().lookupOrThrow(Registries.FEATURE).getValue(Identifier.withDefaultNamespace("sulfur_pool"));
        helper.assertTrue(pool != null, "minecraft:sulfur_pool is not a feature");
        List<String> pools = new ArrayList<>();
        for (int seed = 0; seed < SEEDS; seed++) {
            slab(helper);
            pool.place(level, level.getChunkSource().getGenerator(), RandomSource.create((seed + 1) * 0x9E3779B97F4A7C15L), helper.absolutePos(new BlockPos(24, TOP, 24)));
            int floor = 0, geysers = 0;
            for (BlockPos pos : BlockPos.betweenClosed(FROM, 1, FROM, TO, TOP, TO)) {
                BlockState state = helper.getBlockState(pos);
                if (state.isAir() || !state.getFluidState().isEmpty() || !helper.getBlockState(pos.above()).getFluidState().isSourceOfType(Fluids.WATER)) continue;
                floor++;
                if (!state.is(Blocks.POTENT_SULFUR)) continue;
                geysers++;
                helper.assertTrue(helper.getBlockState(pos.below()).is(Blocks.MAGMA_BLOCK),
                        "seed " + seed + ": potent sulfur at " + pos.toShortString() + " over " + helper.getBlockState(pos.below()) + ", not magma");
                helper.assertTrue(state.getValue(PotentSulfurBlock.STATE) == PotentSulfurState.DORMANT, "seed " + seed + ": a geyser that isn't dormant: " + state);
            }
            helper.assertTrue(floor > 0, "seed " + seed + ": no pool grew");
            helper.assertTrue(geysers >= Math.min(6, floor) && 3 * geysers >= floor, "seed " + seed + ": " + geysers + " geysers on a pool floor of " + floor + " blocks");
            pools.add(geysers + " of " + floor);
        }
        VersusMod.MOD_LOGGER.info("Sulfur pools: geysers of their floor's blocks: {}", String.join(", ", pools));
        slab(helper);
        helper.succeed();
    }

    /** A sulfur spike, its tip pointing this way. */
    private static BlockState spike(Direction tip) {
        return Blocks.SULFUR_SPIKE.defaultBlockState().setValue(SpeleothemBlock.TIP_DIRECTION, tip).setValue(SpeleothemBlock.THICKNESS, SpeleothemThickness.TIP);
    }

    /** A pig that stays put, with 40 health so that no hurt here kills it. */
    private static LivingEntity sturdyPig(GameTestHelper helper, double x, double y, double z) {
        LivingEntity pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(x, y, z));
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
        pig.setHealth(40.0F);
        return pig;
    }

    /**
     * That this pig was hurt, and is poisoned for at most a second a point of damage dealt and nauseous for five seconds
     * up to a second a point (checked as soon as it is: its poison may have hurt it a point more since).
     */
    private static void afflictedForItsHurt(GameTestHelper helper, LivingEntity pig, String what) {
        float dealt = pig.getMaxHealth() - pig.getHealth();
        helper.assertTrue(dealt > 0, what + " didn't hurt the pig");
        MobEffectInstance poison = pig.getEffect(MobEffects.POISON), nausea = pig.getEffect(MobEffects.NAUSEA);
        helper.assertTrue(poison != null && poison.getAmplifier() == 0 && poison.getDuration() <= 20 * Math.max(1, Mth.floor(dealt)),
                what + " dealt " + dealt + " and left poison " + poison);
        helper.assertTrue(nausea != null && nausea.getAmplifier() == 0 && nausea.getDuration() > 90 && nausea.getDuration() <= 20 * Math.max(5, Mth.floor(dealt)),
                what + " dealt " + dealt + " and left nausea " + nausea);
    }

    /** A sulfur spike falling onto a pig poisons it and makes it nauseous, for as long as the damage it dealt allows. */
    @GameTest(structure = GROVE, maxTicks = 60, skyAccess = true)
    public void aFallingSulfurSpikePoisonsAndNauseatesWhatItHurts(GameTestHelper helper) {
        LivingEntity pig = sturdyPig(helper, 10.5, 1.0, 10.5);
        helper.setBlock(new BlockPos(10, 4, 10), Blocks.STONE);
        helper.setBlock(new BlockPos(10, 3, 10), spike(Direction.DOWN));
        helper.getLevel().scheduleTick(helper.absolutePos(new BlockPos(10, 3, 10)), Blocks.SULFUR_SPIKE, 1);
        helper.succeedWhen(() -> afflictedForItsHurt(helper, pig, "the falling spike"));
    }

    /** Landing on a sulfur spike: the fall's hurt is the spike's, and it poisons and nauseates as one falling does. */
    @GameTest(structure = GROVE, maxTicks = 80, skyAccess = true)
    public void landingOnASulfurSpikePoisonsAndNauseates(GameTestHelper helper) {
        helper.setBlock(new BlockPos(20, 1, 10), spike(Direction.UP));
        LivingEntity pig = sturdyPig(helper, 20.5, 9.0, 10.5);
        helper.succeedWhen(() -> afflictedForItsHurt(helper, pig, "landing on the spike"));
    }

    /** A fall as long onto plain ground hurts too, but neither poisons nor nauseates. */
    @GameTest(structure = GROVE, maxTicks = 80, skyAccess = true)
    public void aFallOntoPlainGroundDoesNeither(GameTestHelper helper) {
        LivingEntity pig = sturdyPig(helper, 30.5, 9.0, 10.5);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "the fall didn't hurt the pig");
            helper.assertTrue(!pig.hasEffect(MobEffects.POISON) && !pig.hasEffect(MobEffects.NAUSEA), "a fall onto grass poisoned or nauseated the pig");
            helper.succeed();
        });
    }
}
