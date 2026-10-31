package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.mod.mobs.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A spider overgrown with mushrooms, of the caves under the mushroom islands. It leaves players alone and hunts plain
 * spiders instead: its bite does them no harm, but turns them into mushroom spiders. A player who hurts it is bitten
 * like any spider's prey.
 */
public class MushroomSpiderEntity extends Spider {

    public MushroomSpiderEntity(EntityType<? extends Spider> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        // Whoever hurts it, then the plain spiders it sees
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Spider.class, true, (target, level) -> target.getType() == EntityTypes.SPIDER));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() && this.random.nextInt(5) == 0) {
            this.level().addParticle(ParticleTypes.MYCELIUM, this.getRandomX(0.8), this.getRandomY(), this.getRandomZ(0.8), 0.0, 0.0, 0.0);
        }
    }

    /** A plain spider bitten turns, unharmed; anything else is bitten as a spider bites. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (!(target instanceof Spider spider) || spider.getType() != EntityTypes.SPIDER) return super.doHurtTarget(level, target);
        level.playSound(null, spider.getX(), spider.getY(), spider.getZ(), SoundEvents.PUFFER_FISH_BLOW_OUT, this.getSoundSource(), 0.8F, 0.8F + 0.2F * this.random.nextFloat());
        level.sendParticles(ParticleTypes.MYCELIUM, spider.getX(), spider.getY(0.5), spider.getZ(), 40, 0.7, 0.4, 0.7, 0.02);
        spider.convertTo(ModEntities.MUSHROOM_SPIDER, ConversionParams.single(spider, false, false), mushroom -> {});
        this.setTarget(null);
        return true;
    }
}
