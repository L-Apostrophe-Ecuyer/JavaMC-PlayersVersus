package frootloops.versus.mod.enchantments;

import frootloops.versus.VersusMod;
import java.util.Iterator;
import java.util.Optional;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;


public abstract class CustomEnchants {

    public static final ResourceKey<Enchantment> CRITICAL_STRIKE = of("critical_strike");
    public static final ResourceKey<Enchantment> TOSSING = of("tossing");
    public static final ResourceKey<Enchantment> RIPOSTE = of("riposte");
    public static final ResourceKey<Enchantment> CLEAVING = of("cleaving");
    public static final ResourceKey<Enchantment> FROST_ASPECT = of("frost_aspect");
    public static final ResourceKey<Enchantment> CURSE_OF_ENDER = of("ender_curse");
    public static final ResourceKey<Enchantment> BOUNDING_STRIDES = of("bounding_strides");
    public static final ResourceKey<Enchantment> MAGIC_PROTECTION = of("magic_protection");
    public static final ResourceKey<Enchantment> IMPACT_PROTECTION = of("impact_protection");
    public static final ResourceKey<Enchantment> PIERCING_PROTECTION = of("piercing_protection");

    private static ResourceKey<Enchantment> of(String id) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, id));
    }

    public final static void performTossAttack(ServerLevel world,LivingEntity user, Entity target, double magnitude){
        target.push(0.0, magnitude, 0.0);
        target.level().playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, user.getSoundSource(), 1.2f, 1.2f);
        target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, user.getSoundSource(), 1.0f, 1.0f);
    }

    public final static void performFrostAttack(ServerLevel world,LivingEntity user, Entity target, int level){
        if (target instanceof LivingEntity targetEntity && targetEntity.canFreeze()) {
            user.level().playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, user.getSoundSource(), 1.0f, 1.0f);
            targetEntity.clearFire();

            // Minimum ticks to get damaged is 140, for most. ModEntities get rid of 2 FrozenTicks per tick.
            target.setTicksFrozen(target.getTicksFrozen() + 180 + 60 * level);
            if(user.level() instanceof ServerLevel serverWorld) {
                target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_HURT_FREEZE, target.getSoundSource(), 1.0f, 1.0f);
                serverWorld.sendParticles(ParticleTypes.WAX_OFF, target.getX(), target.getY() + 1, target.getZ(), 4, 0.2, 0.2, 0.2, 6.0f);
            }
        }
    }

    public final static void performImpalingAttack(ServerLevel world,LivingEntity user, Entity target, int level){
        if(target.isInWaterOrRain()) {
            target.hurtServer(world, user.damageSources().trident(user, user), (float)level);
            user.playSound(SoundEvents.TRIDENT_HIT, 1.1f, 1.0f);
        }
    }

    public static void onCurseOfEnderUserDamaged(ServerLevel world,LivingEntity user, Entity attacker) {
        if(attacker instanceof LivingEntity && user != null & user.isAlive()) {
            if (!user.level().isClientSide()) {
                user.hurtServer(world, user.damageSources().magic(), 2.0f);
                if(!user.isAlive())
                    return;

                user.setInvulnerableTime(18); // 8 ticks of invincibility frames
                double d = user.getX();
                double e = user.getY();
                double f = user.getZ();
                for (int i = 0; i < 16; ++i) {
                    double g = user.getX() + (user.getRandom().nextDouble() - 0.5) * 16.0;
                    double h = Math.clamp(user.getY() + (double)(user.getRandom().nextInt(16) - 8), user.level().getMinY(), user.level().getMinY() + ((ServerLevel)user.level()).getLogicalHeight() - 1);
                    double j = user.getZ() + (user.getRandom().nextDouble() - 0.5) * 16.0;
                    if (user.isPassenger()) {
                        user.stopRiding();
                    }
                    Vec3 vec3d = user.position();
                    if (!user.randomTeleport(g, h, j, true, state -> false)) continue;

                    user.level().gameEvent(GameEvent.TELEPORT, vec3d, GameEvent.Context.of(user));
                    user.level().playSound(null, d, e, f, SoundEvents.ENDERMAN_TELEPORT, user.getSoundSource(), 1.0f, 1.0f);
                    user.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    user.lookAt(EntityAnchorArgument.Anchor.EYES, attacker.getEyePosition());
                    break;
                }
            }
        }
    }
}
