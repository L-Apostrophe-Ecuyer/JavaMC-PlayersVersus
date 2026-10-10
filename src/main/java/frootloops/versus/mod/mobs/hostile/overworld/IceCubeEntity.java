package frootloops.versus.mod.mobs.hostile.overworld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * The Ice Cube: a blue slime of the ice caves. It hops, splits and drops like a slime (ice balls instead of slime balls,
 * from the smallest), and what it hits freezes: the cube mobs' attack ({@code SlimeMixin}) calls {@link #freeze}. Its
 * own frost doesn't bother it (it's in {@code freeze_immune_entity_types} too).
 */
public class IceCubeEntity extends Slime {

    /** How long a hit freezes for, per size: a big cube's (size 4) as long as two frostbite punches (60 ticks each). */
    private static final int FREEZING_TICKS_PER_SIZE = 30;

    public IceCubeEntity(EntityType<? extends IceCubeEntity> entityType, Level world) {
        super(entityType, world);
    }

    /** Slimes' attributes; their health, speed and attack come from their size ({@link Slime#setSize}). */
    public static AttributeSupplier.Builder createIceCubeAttributes() {
        return Monster.createMonsterAttributes();
    }

    /** Freezes a target this cube has just hit. */
    public void freeze(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.FREEZING, FREEZING_TICKS_PER_SIZE * this.getSize()), this);
    }

    @Override
    protected ParticleOptions getParticleType() {
        return ParticleTypes.SNOWFLAKE;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.FREEZING) && super.canBeAffected(effect);
    }

    /** In the dark, where its biome spawns it (the ice caves, {@code MobSpawning}); anywhere from a spawner or egg. */
    public static boolean checkIceCubeSpawnRules(EntityType<IceCubeEntity> type, ServerLevelAccessor world, EntitySpawnReason spawnReason, BlockPos pos, RandomSource random) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) return false;
        if (!EntitySpawnReason.isSpawner(spawnReason) && !Monster.isDarkEnoughToSpawn(world, pos, random)) return false;
        return checkMobSpawnRules(type, world, spawnReason, pos, random);
    }
}
