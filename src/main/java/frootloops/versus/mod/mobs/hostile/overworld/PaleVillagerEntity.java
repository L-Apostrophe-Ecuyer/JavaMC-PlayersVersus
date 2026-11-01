package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.mod.items_and_effects.conversion.Undeath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A villager the paleness took: withered (wither damage below 8 health, as zombies and creepers turn pale), or killed
 * by another pale villager. It looks a villager still, in its own clothes, but its skin has gone white. It keeps its
 * trade, its trades and what the village thought of it, as a zombie villager does, for when it's cured: a golden apple
 * fed to it (ZombieVillagerMixin, which it inherits) starts its cure, more apples hasten it. It shies from players and
 * hunts villagers: its hits wither them, and the villagers it withers or kills turn pale too. It neither burns nor
 * drowns into anything, and stays where it turned.
 */
public class PaleVillagerEntity extends ZombieVillager {
    /** How long its hits wither (ticks). */
    static final int WITHER_TICKS = 100;

    public PaleVillagerEntity(EntityType<? extends ZombieVillager> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createPaleVillagerAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                // Zombie#hurtServer reads it: none of its kind come to help
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0);
    }

    /** Flees players, hunts villagers (and wandering traders). */
    @Override
    public void addBehaviourGoals() {
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 10.0F, 1.0, 1.3));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
    }

    /** No random bonus to the reinforcements chance, nor any leader's. */
    @Override
    protected void handleAttributes(float difficulty, EntitySpawnReason reason) {}

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.WITHER, WITHER_TICKS, 0), this);
        return hit;
    }

    /** A villager it kills turns pale, as a zombie's kill turns zombie (as often: on normal, half of them; on hard, all). */
    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity entity, DamageSource source) {
        if (entity instanceof Villager villager && level.getDifficulty() != Difficulty.EASY && level.getDifficulty() != Difficulty.PEACEFUL
                && (level.getDifficulty() == Difficulty.HARD || this.random.nextBoolean())
                && Undeath.turnPaleVillager(level, villager, !this.isSilent())) {
            return false;
        }
        // Not Zombie#killedEntity, which would make a zombie villager of it
        return true;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    protected boolean convertsWhenFreezing() {
        return false;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    /** Stays where it turned, for whoever means to cure it. */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    public SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    public SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    /** A villager's voice, gone low. */
    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.75F;
    }
}
