package frootloops.versus.mod.items_and_effects.brewing.effects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

public class HauntingStatusEffect extends MobEffect  {
    private static final MobEffectCategory category = MobEffectCategory.NEUTRAL;
    private static final int color = 0;

    public HauntingStatusEffect() {
        super(category, color, ParticleTypes.SCULK_SOUL);
    }

    @Override
    public void applyInstantenousEffect(
            ServerLevel world, @Nullable Entity effectEntity, @Nullable Entity attacker, LivingEntity target, int amplifier, double proximity
    ) {
        target.hurtServer(world, target.damageSources().fellOutOfWorld(), 2.0f);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if(!entity.isAlive()) return;

        entity.level().gameEvent(entity, GameEvent.ENTITY_DIE, entity.position());
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.WARDEN_SONIC_CHARGE, entity.getSoundSource(), 1.0f, 0.2f);
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_BREATH, entity.getSoundSource(), 0.2f, 0.4f);

        if(entity.isAlwaysTicking()) {
            if(entity instanceof ServerPlayer player && !player.isSpectator()) {
                player.setGameMode(GameType.SPECTATOR);
                int xpToDrop = (player.totalExperience * 2)/5;
                if(xpToDrop > 0 && !player.wasExperienceConsumed()) {
                    player.setExperiencePoints(0);
                    player.setExperienceLevels(0);
                    player.totalExperience = 0;
                    ExperienceOrb.award(player.level(), player.position(), xpToDrop);
                }
                entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, -1, 5));
                entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 300, 0));
            }
        }
        else {
            entity.setInvisible(true);
            entity.setNoGravity(true);
            if(entity.level() instanceof ServerLevel serverWorld) {
                int xpToDrop = (entity.getExperienceReward(serverWorld, null) * 3) / 5;
                if (xpToDrop > 0 && !entity.wasExperienceConsumed()) {
                    ExperienceOrb.award(serverWorld, entity.position(), xpToDrop);
                }
                entity.skipDropExperience();
            }
        }
    }

    @Override
    public void onMobRemoved(ServerLevel world, LivingEntity entity, int amplifier, Entity.RemovalReason reason) {
        removeEffect(entity);
    }

    public static void removeEffect(LivingEntity entity) {
        if(entity.isAlwaysTicking()) {
            if(entity instanceof ServerPlayer player) player.setGameMode(GameType.SURVIVAL);
            entity.forceAddEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 4), null);
        }
        else {
            entity.setInvisible(false);
            entity.setNoGravity(false);
        }
        entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 50, 0));
        entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 0));
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENDER_EYE_DEATH, entity.getSoundSource(), 1.0f, 1.0f);
    }


    @Override
    public void onEffectAdded(LivingEntity entity, int amplifier) {
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.SCULK_CATALYST_BLOOM, entity.getSoundSource(), 1.0f, 1.0f);
    }
}
