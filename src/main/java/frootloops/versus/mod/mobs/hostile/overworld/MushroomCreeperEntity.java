package frootloops.versus.mod.mobs.hostile.overworld;

import frootloops.versus.mod.items_and_effects.conversion.ConversionAttacks;
import frootloops.versus.mod.mobs.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A creeper overgrown with mushrooms, of the caves under the mushroom islands. It leaves players alone and seeks out
 * creepers instead: it swells and hisses beside one, then lets out a puff of spores (CreeperMixin, in place of the
 * explosion: it never explodes, lit or not) that turns every creeper within {@link #SPORE_REACH} blocks into one of its
 * own and gives everything else living there a dose of Conversion, and can puff again after {@link #PUFF_COOLDOWN}
 * ticks. A player who hurts it is the exception it goes after.
 */
public class MushroomCreeperEntity extends Creeper {

    /** Ticks after a spore puff before it can swell again. */
    public static final int PUFF_COOLDOWN = 100;
    /** How near a creeper has to be, in blocks, to catch the spores. */
    public static final double SPORE_REACH = 3.0;

    private int puffCooldown;

    public MushroomCreeperEntity(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SwellGoal(this) {
            @Override
            public boolean canUse() {
                return MushroomCreeperEntity.this.puffCooldown == 0 && super.canUse();
            }
        });
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        // Whoever hurts it, then the plain creepers it sees
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Creeper.class, true, (target, level) -> target.getType() == EntityTypes.CREEPER));
    }

    @Override
    public void tick() {
        if (this.puffCooldown > 0) this.puffCooldown--;
        super.tick();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide() && this.random.nextInt(5) == 0) {
            this.level().addParticle(ParticleTypes.MYCELIUM, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0.0, 0.0, 0.0);
        }
    }

    /**
     * The spore puff, on the server, where vanilla's explosion would be: no damage, no blocks broken; the creepers
     * near it turn, everything else living near it takes a dose of Conversion, and it deflates to wait out its cooldown.
     */
    public void puff(ServerLevel level) {
        this.setSwellDir(-1);
        this.puffCooldown = PUFF_COOLDOWN;
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PUFFER_FISH_BLOW_OUT, this.getSoundSource(), 1.0F, 0.6F + 0.2F * this.random.nextFloat());
        level.sendParticles(ParticleTypes.MYCELIUM, this.getX(), this.getY(0.5), this.getZ(), 80, SPORE_REACH / 2, 0.8, SPORE_REACH / 2, 0.02);
        level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY(0.5), this.getZ(), 12, 0.6, 0.4, 0.6, 0.02);
        for (Creeper creeper : level.getEntitiesOfClass(Creeper.class, this.getBoundingBox().inflate(SPORE_REACH),
                creeper -> creeper.getType() == EntityTypes.CREEPER && creeper.isAlive())) {
            creeper.convertTo(ModEntities.MUSHROOM_CREEPER, ConversionParams.single(creeper, false, false), mushroom -> {});
        }
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(SPORE_REACH),
                living -> living != this && living.isAlive() && !(living instanceof Creeper))) {
            living.addEffect(ConversionAttacks.dose(), this);
        }
        this.setTarget(null);
    }
}
