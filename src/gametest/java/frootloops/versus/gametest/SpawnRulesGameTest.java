package frootloops.versus.gametest;

import frootloops.versus.mod.environment.PaleLine;
import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.environment.worldgen.CustomOverworldBiomes;
import frootloops.versus.mod.mobs.hostile.end.WatchlingEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The monsters' natural spawn rules, run where they should and shouldn't spawn, as the game's natural spawner runs them
 * (the placement's checks, then the mob's own): in a dark room of stone, on a built floor, in a corridor the sky's light
 * creeps into, and below the pale line, where a zombie, creeper or spider about to spawn is its pale kin. Watchlings:
 * where they are listed, and never two within reach of each other.
 */
@SuppressWarnings("removal")
public class SpawnRulesGameTest {

    // 48 x 40 x 48, a grass floor, open to the sky
    private static final String GROVE = "players-versus-gametest:leaves_grove";
    private static final int TRIES = 60;

    /**
     * A closed box of {@code shell} (its floor {@code floor}), inside {@code size} by {@code height} by {@code size} from
     * {@code corner} (the inside's lowest corner, relative to the grove): no light gets in. Returns the inside floor's
     * middle, absolute.
     */
    private static BlockPos room(GameTestHelper helper, BlockPos corner, int size, int height, Block floor, Block shell) {
        ServerLevel level = helper.getLevel();
        for (int x = -1; x <= size; x++) {
            for (int z = -1; z <= size; z++) {
                for (int y = -1; y <= height; y++) {
                    boolean inside = x >= 0 && x < size && z >= 0 && z < size && y >= 0 && y < height;
                    Block block = inside ? Blocks.AIR : y == -1 ? floor : shell;
                    level.setBlock(helper.absolutePos(corner.offset(x, y, z)), block.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        return helper.absolutePos(corner.offset(size / 2, 0, size / 2));
    }

    /** Whether the type may spawn naturally there: the placement's checks and the mob's own, as the natural spawner runs them. */
    private static boolean maySpawn(ServerLevel level, EntityType<?> type, BlockPos pos) {
        if (!SpawnPlacements.isSpawnPositionOk(type, level, pos)) return false;
        if (!SpawnPlacements.checkSpawnRules(type, level, EntitySpawnReason.NATURAL, pos, level.getRandom())) return false;
        if (!level.noCollision(type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) return false;
        Entity entity = type.create(level, EntitySpawnReason.NATURAL);
        if (!(entity instanceof Mob mob)) return false;
        mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        boolean may = mob.checkSpawnRules(level, EntitySpawnReason.NATURAL) && mob.checkSpawnObstruction(level);
        mob.discard();
        return may;
    }

    /** Of {@link #TRIES} tries, how many times the type may spawn there (the darkness's test is random). */
    private static int times(ServerLevel level, EntityType<?> type, BlockPos pos) {
        int times = 0;
        for (int i = 0; i < TRIES; i++) if (maySpawn(level, type, pos)) times++;
        return times;
    }

    /** In a dark room of stone (no sky, no light): zombies, skeletons, creepers and spiders all may spawn. */
    @GameTest(structure = GROVE, maxTicks = 100, skyAccess = true)
    public void monstersSpawnInADarkRoomOfStone(GameTestHelper helper) {
        BlockPos middle = room(helper, new BlockPos(4, 2, 4), 7, 3, Blocks.STONE, Blocks.STONE);
        helper.runAfterDelay(20, () -> {
            ServerLevel level = helper.getLevel();
            helper.assertTrue(level.getBrightness(LightLayer.SKY, middle) == 0 && level.getBrightness(LightLayer.BLOCK, middle) == 0, "the room isn't dark");
            for (EntityType<?> type : List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.CREEPER, EntityTypes.SPIDER)) {
                helper.assertTrue(times(level, type, middle) > 0, type.getDescription().getString() + " never may spawn in a dark room of stone");
            }
            helper.succeed();
        });
    }

    /** On a floor someone built (planks), in the dark: no zombie, skeleton or creeper. */
    @GameTest(structure = GROVE, maxTicks = 100, skyAccess = true)
    public void noUndeadNorCreeperOnABuiltFloor(GameTestHelper helper) {
        BlockPos middle = room(helper, new BlockPos(4, 2, 4), 7, 3, Blocks.OAK_PLANKS, Blocks.STONE);
        helper.runAfterDelay(20, () -> {
            ServerLevel level = helper.getLevel();
            for (EntityType<?> type : List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.CREEPER)) {
                helper.assertTrue(times(level, type, middle) == 0, type.getDescription().getString() + " may spawn on planks");
            }
            helper.succeed();
        });
    }

    /**
     * In a corridor of stone the sky's light creeps into (a shaft open to the sky at one end), where the light is dim
     * enough for a zombie: no creeper, which wants no sky light at all.
     */
    @GameTest(structure = GROVE, maxTicks = 100, skyAccess = true)
    public void noCreeperWhereTheSkysLightCreepsIn(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int length = 16;
        // The corridor: x 2 to 17, y 2 and 3, z 20; stone all round, its roof open over x 2
        for (int x = 1; x <= length + 2; x++) {
            for (int y = 1; y <= 4; y++) {
                for (int z = 19; z <= 21; z++) {
                    boolean inside = x >= 2 && x <= length + 1 && (y == 2 || y == 3) && z == 20;
                    boolean shaft = x == 2 && y == 4 && z == 20;
                    level.setBlock(helper.absolutePos(new BlockPos(x, y, z)), (inside || shaft ? Blocks.AIR : Blocks.STONE).defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
        helper.runAfterDelay(20, () -> {
            BlockPos dim = null;
            for (int x = 2; x <= length + 1 && dim == null; x++) {
                BlockPos at = helper.absolutePos(new BlockPos(x, 2, 20));
                int sky = level.getBrightness(LightLayer.SKY, at);
                if (sky >= 1 && sky <= 5) dim = at;
            }
            helper.assertTrue(dim != null, "no spot in the corridor with a little sky light");
            helper.assertTrue(times(level, EntityTypes.ZOMBIE, dim) > 0, "the dim spot isn't dark enough for a zombie (sky light " + level.getBrightness(LightLayer.SKY, dim) + ")");
            helper.assertTrue(times(level, EntityTypes.CREEPER, dim) == 0, "a creeper may spawn with sky light " + level.getBrightness(LightLayer.SKY, dim));
            helper.succeed();
        });
    }

    private static final Set<EntityType<?>> PALE_KIN = Set.of(ModEntities.PALE_ZOMBIE, ModEntities.PALE_CREEPER, ModEntities.PALE_SPIDER);
    // The kinds whose pale kin spawn below the pale line (the test world's desert has zombies, husks, creepers and cave spiders)
    private static final Set<EntityType<?>> PALE_KINDS = Set.of(EntityTypes.ZOMBIE, EntityTypes.HUSK, EntityTypes.ZOMBIE_VILLAGER,
            EntityTypes.CREEPER, EntityTypes.SPIDER, EntityTypes.CAVE_SPIDER);

    /**
     * The monsters the natural spawner spawns around {@code middle} (a closed dark room's floor), as the game runs it: a
     * player far enough away (40 blocks up), {@code tries} tries. The mobs are counted by kind, then taken away with the
     * player.
     */
    private static Map<EntityType<?>, Integer> naturalSpawns(GameTestHelper helper, BlockPos middle, int tries) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.snapTo(middle.getX() + 0.5, middle.getY() + 40, middle.getZ() + 0.5, 0.0F, 0.0F);
        for (int i = 0; i < tries; i++) NaturalSpawner.spawnCategoryForPosition(MobCategory.MONSTER, level, middle);
        Map<EntityType<?>, Integer> spawned = new HashMap<>();
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(middle).inflate(9, 3, 9))) {
            spawned.merge(mob.getType(), 1, Integer::sum);
            mob.discard();
        }
        level.getServer().getPlayerList().remove(player);
        return spawned;
    }

    private static int count(Map<EntityType<?>, Integer> spawned, Set<EntityType<?>> kinds) {
        return spawned.entrySet().stream().filter(entry -> kinds.contains(entry.getKey())).mapToInt(Map.Entry::getValue).sum();
    }

    /**
     * Below the pale line, in a closed dark room of deepslate, the natural spawner's zombies, creepers and spiders (husks
     * and cave spiders too) are their pale kin.
     */
    @GameTest(structure = GROVE, maxTicks = 100, skyAccess = true)
    public void belowThePaleLineTheyArePale(GameTestHelper helper) {
        // The room is built, its light settled (none at all), then the spawner runs
        BlockPos middle = room(helper, new BlockPos(10, PaleLine.Y - 8 - helper.absolutePos(BlockPos.ZERO).getY(), 10), 15, 3, Blocks.DEEPSLATE, Blocks.DEEPSLATE);
        helper.runAfterDelay(20, () -> {
            Map<EntityType<?>, Integer> spawned = naturalSpawns(helper, middle, 400);
            helper.assertTrue(count(spawned, PALE_KIN) > 0, "no pale kin spawned below the pale line: " + spawned);
            helper.assertTrue(count(spawned, PALE_KINDS) == 0, "zombies, creepers or spiders spawned below the pale line: " + spawned);
            helper.succeed();
        });
    }

    /** Above it, in the same room of stone, they are themselves: no pale kin where the biome has none. */
    @GameTest(structure = GROVE, maxTicks = 100, skyAccess = true)
    public void aboveThePaleLineTheyAreThemselves(GameTestHelper helper) {
        BlockPos middle = room(helper, new BlockPos(10, 2, 10), 15, 3, Blocks.STONE, Blocks.STONE);
        helper.runAfterDelay(20, () -> {
            Map<EntityType<?>, Integer> spawned = naturalSpawns(helper, middle, 400);
            helper.assertTrue(count(spawned, PALE_KINDS) > 0, "no zombies, creepers or spiders spawned above the pale line: " + spawned);
            helper.assertTrue(count(spawned, PALE_KIN) == 0, "pale kin spawned above the pale line: " + spawned);
            helper.succeed();
        });
    }

    /**
     * Watchlings now and then: every pale biome lists them at 15 (the Pale Garden, the pale taiga, the pale swamp), the
     * caves at 12 (the deep caves, the pale grotto and hollow, the regular cave).
     */
    @GameTest(structure = GROVE, maxTicks = 20)
    public void watchlingsAreListedInThePaleBiomesAndTheCaves(GameTestHelper helper) {
        Registry<Biome> biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        Map<ResourceKey<Biome>, Integer> weights = Map.of(Biomes.PALE_GARDEN, 15, CustomOverworldBiomes.PALE_TAIGA, 15,
                CustomOverworldBiomes.PALE_SWAMP, 15, CustomOverworldBiomes.DEEP_CAVES, 12, CustomOverworldBiomes.PALE_GROTTO, 12,
                CustomOverworldBiomes.PALE_HOLLOW, 12,
                CustomOverworldBiomes.REGULAR_CAVE, 12);
        weights.forEach((key, weight) -> {
            int listed = biomes.getValueOrThrow(key).getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY)
                    .getMobsToSpawn(MobCategory.MONSTER).unwrap().stream()
                    .filter(entry -> entry.value().type() == ModEntities.WATCHLING).mapToInt(Weighted::weight).sum();
            helper.assertTrue(listed == weight, key.identifier() + " lists watchlings at " + listed + ", not " + weight);
        });
        helper.succeed();
    }

    /**
     * In a closed dark room below the pale line a watchling may spawn, unless another is within 128 blocks (a watchling
     * haunts one player at a time). (Another test's watchling may be about: then none may spawn here either.) The room is
     * four blocks high: a watchling stands over three.
     */
    @GameTest(structure = GROVE, maxTicks = 100, skyAccess = true)
    public void noWatchlingSpawnsWithinReachOfAnother(GameTestHelper helper) {
        BlockPos middle = room(helper, new BlockPos(10, PaleLine.Y - 8 - helper.absolutePos(BlockPos.ZERO).getY(), 10), 15, 4, Blocks.DEEPSLATE, Blocks.DEEPSLATE);
        helper.runAfterDelay(20, () -> {
            ServerLevel level = helper.getLevel();
            Vec3 at = Vec3.atBottomCenterOf(middle);
            boolean alone = level.getEntitiesOfClass(WatchlingEntity.class, new AABB(middle).inflate(128), watchling -> watchling.distanceToSqr(at) < 128 * 128).isEmpty();
            int times = times(level, ModEntities.WATCHLING, middle);
            helper.assertTrue(alone ? times > 0 : times == 0, alone ? "a watchling never may spawn in the dark below the pale line" : "a watchling may spawn with another about");
            WatchlingEntity other = helper.spawnWithNoFreeWill(ModEntities.WATCHLING, new Vec3(24.5, 2.0, 40.5));
            int near = times(level, ModEntities.WATCHLING, middle);
            int distance = (int) Math.sqrt(other.distanceToSqr(at));
            other.discard();
            helper.assertTrue(near == 0, "a watchling may spawn with another " + distance + " blocks away");
            helper.succeed();
        });
    }
}
