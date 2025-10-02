package frootloops.versus.mod.enchantments;

import frootloops.versus.VersusMod;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import java.util.Iterator;
import java.util.Optional;


public abstract class CustomEnchants {

    public static final RegistryKey<Enchantment> CRITICAL_STRIKE = of("critical_strike");
    public static final RegistryKey<Enchantment> TOSSING = of("tossing");
    public static final RegistryKey<Enchantment> RIPOSTE = of("riposte");
    public static final RegistryKey<Enchantment> CLEAVING = of("cleaving");
    public static final RegistryKey<Enchantment> FROST_ASPECT = of("frost_aspect");
    public static final RegistryKey<Enchantment> CURSE_OF_ENDER = of("ender_curse");
    public static final RegistryKey<Enchantment> BOUNDING_STRIDES = of("bounding_strides");
    public static final RegistryKey<Enchantment> MAGIC_PROTECTION = of("magic_protection");
    public static final RegistryKey<Enchantment> IMPACT_PROTECTION = of("impact_protection");
    public static final RegistryKey<Enchantment> PIERCING_PROTECTION = of("piercing_protection");

    private static RegistryKey<Enchantment> of(String id) {
        return RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(VersusMod.MOD_ID, id));
    }

    public final static void performTossAttack(ServerWorld world,LivingEntity user, Entity target, double magnitude){
        target.addVelocity(0.0, magnitude, 0.0);
        target.getWorld().playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, user.getSoundCategory(), 1.2f, 1.2f);
        target.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, user.getSoundCategory(), 1.0f, 1.0f);
    }

    public final static void performFrostAttack(ServerWorld world,LivingEntity user, Entity target, int level){
        if (target instanceof LivingEntity targetEntity && targetEntity.canFreeze()) {
            user.getWorld().playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, user.getSoundCategory(), 1.0f, 1.0f);
            targetEntity.extinguish();

            // Minimum ticks to get damaged is 140, for most. ModEntities get rid of 2 FrozenTicks per tick.
            target.setFrozenTicks(target.getFrozenTicks() + 180 + 60 * level);
            if(user.getWorld() instanceof ServerWorld serverWorld) {
                target.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_HURT_FREEZE, target.getSoundCategory(), 1.0f, 1.0f);
                serverWorld.spawnParticles(ParticleTypes.WAX_OFF, target.getX(), target.getY() + 1, target.getZ(), 4, 0.2, 0.2, 0.2, 6.0f);
            }
        }
    }

    public final static void performImpalingAttack(ServerWorld world,LivingEntity user, Entity target, int level){
        if(target.isTouchingWaterOrRain()) {
            target.damage(world, user.getDamageSources().trident(user, user), (float)level);
            user.playSound(SoundEvents.ITEM_TRIDENT_HIT, 1.1f, 1.0f);
        }
    }

    public static void onCurseOfEnderUserDamaged(ServerWorld world,LivingEntity user, Entity attacker) {
        if(attacker instanceof LivingEntity && user != null & user.isAlive()) {
            if (!user.getWorld().isClient()) {
                user.damage(world, user.getDamageSources().magic(), 2.0f);
                if(!user.isAlive())
                    return;

                user.timeUntilRegen = 18; // 8 ticks of invincibility frames
                double d = user.getX();
                double e = user.getY();
                double f = user.getZ();
                for (int i = 0; i < 16; ++i) {
                    double g = user.getX() + (user.getRandom().nextDouble() - 0.5) * 16.0;
                    double h = MathHelper.clamp(user.getY() + (double)(user.getRandom().nextInt(16) - 8), user.getWorld().getBottomY(), user.getWorld().getBottomY() + ((ServerWorld)user.getWorld()).getLogicalHeight() - 1);
                    double j = user.getZ() + (user.getRandom().nextDouble() - 0.5) * 16.0;
                    if (user.hasVehicle()) {
                        user.stopRiding();
                    }
                    Vec3d vec3d = user.getEntityPos();
                    if (!user.teleport(g, h, j, true)) continue;

                    user.getWorld().emitGameEvent(GameEvent.TELEPORT, vec3d, GameEvent.Emitter.of(user));
                    user.getWorld().playSound(null, d, e, f, SoundEvents.ENTITY_ENDERMAN_TELEPORT, user.getSoundCategory(), 1.0f, 1.0f);
                    user.playSound(SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    user.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, attacker.getEyePos());
                    break;
                }
            }
        }
    }
}
