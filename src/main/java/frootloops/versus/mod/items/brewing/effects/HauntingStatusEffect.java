package frootloops.versus.mod.items.brewing.effects;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.GameMode;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

public class HauntingStatusEffect extends StatusEffect  {
    private static final StatusEffectCategory category = StatusEffectCategory.NEUTRAL;
    private static final int color = 0;

    public HauntingStatusEffect() {
        super(category, color, ParticleTypes.SCULK_SOUL);
    }

    @Override
    public void applyInstantEffect(
            ServerWorld world, @Nullable Entity effectEntity, @Nullable Entity attacker, LivingEntity target, int amplifier, double proximity
    ) {
        target.damage(world, target.getDamageSources().outOfWorld(), 2.0f);
    }

    @Override
    public void onApplied(LivingEntity entity, int amplifier) {
        if(!entity.isAlive()) return;

        entity.getWorld().emitGameEvent(entity, GameEvent.ENTITY_DIE, entity.getPos());
        entity.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, entity.getSoundCategory(), 1.0f, 0.2f);
        entity.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENTITY_PLAYER_BREATH, entity.getSoundCategory(), 0.2f, 0.4f);

        if(entity.isPlayer()) {
            if(entity instanceof ServerPlayerEntity player && !player.isSpectator()) {
                player.changeGameMode(GameMode.SPECTATOR);
                int xpToDrop = (player.totalExperience * 2)/5;
                if(xpToDrop > 0 && !player.isExperienceDroppingDisabled()) {
                    player.setExperiencePoints(0);
                    player.setExperienceLevel(0);
                    player.totalExperience = 0;
                    ExperienceOrbEntity.spawn(player.getServerWorld(), player.getPos(), xpToDrop);
                }
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, -1, 5));
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 300, 0));
            }
        }
        else {
            entity.setInvisible(true);
            entity.setNoGravity(true);
            if(entity.getWorld() instanceof ServerWorld serverWorld) {
                int xpToDrop = (entity.getXpToDrop(serverWorld, null) * 3) / 5;
                if (xpToDrop > 0 && !entity.isExperienceDroppingDisabled()) {
                    ExperienceOrbEntity.spawn(serverWorld, entity.getPos(), xpToDrop);
                }
                entity.disableExperienceDropping();
            }
        }
    }

    @Override
    public void onEntityRemoval(ServerWorld world, LivingEntity entity, int amplifier, Entity.RemovalReason reason) {
        removeEffect(entity);
    }

    public static void removeEffect(LivingEntity entity) {
        if(entity.isPlayer()) {
            if(entity instanceof ServerPlayerEntity player) player.changeGameMode(GameMode.SURVIVAL);
            entity.setStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 20, 4), null);
        }
        else {
            entity.setInvisible(false);
            entity.setNoGravity(false);
        }
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 50, 0));
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 50, 0));
        entity.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENTITY_ENDER_EYE_DEATH, entity.getSoundCategory(), 1.0f, 1.0f);
    }


    @Override
    public void playApplySound(LivingEntity entity, int amplifier) {
        entity.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.BLOCK_SCULK_CATALYST_BLOOM, entity.getSoundCategory(), 1.0f, 1.0f);
    }
}
