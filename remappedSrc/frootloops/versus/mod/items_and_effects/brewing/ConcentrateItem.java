package frootloops.versus.mod.items_and_effects.brewing;

import net.minecraft.block.Block;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.ConsumableComponents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.item.consume.UseAction;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.world.World;

import java.util.List;


public class ConcentrateItem extends BlockItem {

    public static final ConsumableComponent CONCENTRATE_COMPONENT = ConsumableComponents.food().consumeSeconds(2.0f).consumeEffect(new ApplyEffectsConsumeEffect(List.of(new StatusEffectInstance(StatusEffects.HUNGER, 120, 2),new StatusEffectInstance(StatusEffects.NAUSEA, 140, 2)))).build();

    protected final RegistryEntry<StatusEffect> effect;
    protected final int amplifier;
    protected final int duration;

    private final TintedParticleEffect particle;

    public ConcentrateItem(net.minecraft.item.Item.Settings settings, RegistryEntry<StatusEffect> registeredEffect, Block block) {
        this(settings, registeredEffect, 0, registeredEffect.value().isInstant() ? 1 : 30, block);
    }

    public ConcentrateItem(net.minecraft.item.Item.Settings settings, RegistryEntry<StatusEffect> registeredEffect, int amplifier, int duration, Block block) {
        super(block, settings);
        if(registeredEffect != null) {
            this.effect = registeredEffect;
            this.amplifier = amplifier;
            this.duration = duration;
            this.particle = TintedParticleEffect.create(ParticleTypes.ENTITY_EFFECT, effect.value().getColor());
        }
        else {
            this.effect = null;
            this.amplifier = 0;
            this.duration = 0;
            this.particle = null;
        }
    }

    public RegistryEntry<StatusEffect> getEffect() {
        return effect;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.EAT;
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 60;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if(remainingUseTicks % 8 == 0) user.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 16, 4));
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ConcentrateItem item = (ConcentrateItem) stack.getItem();
        if(user.getRandom().nextInt(10) < 7 && item.effect != null) {
            user.addStatusEffect(new StatusEffectInstance(item.effect, item.duration, item.amplifier));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 40, 1));
        }
        else user.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 80, 1));
        stack.decrement(1);
        return super.finishUsing(stack,world,user);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        ConcentrateItem item = (ConcentrateItem) stack.getItem();
        if(item.effect != null) {
            StatusEffectInstance effectInstance = new StatusEffectInstance(item.effect, (item.duration * 2)/3, 0);
            if (entity.canHaveStatusEffect(effectInstance)) {
                if(user.getEntityWorld() instanceof ServerWorld serverWorld) {
                    if(effect.value().isInstant()) {
                        if(user.getRandom().nextInt(10) < 7) {
                            if (item.effect == StatusEffects.INSTANT_DAMAGE)
                                entity.damage(serverWorld, serverWorld.getDamageSources().create(DamageTypes.MAGIC, user), 1.0F);
                            else if (item.effect == StatusEffects.INSTANT_DAMAGE) entity.heal(1.0F);
                        }
                    }
                    else {
                        entity.addStatusEffect(effectInstance);
                    }
                }
                for (int i = 0; i < 20; i++) {
                    double d = entity.getRandom().nextGaussian() * 0.02;
                    double e = entity.getRandom().nextGaussian() * 0.02;
                    double f = entity.getRandom().nextGaussian() * 0.02;
                    entity.getEntityWorld().addParticleClient(this.particle, entity.getParticleX(1.0) - d * 10.0, entity.getRandomBodyY() - e * 10.0, entity.getParticleZ(1.0) - f * 10.0, d, e, f);
                }
                if(entity instanceof MobEntity mobEntity) mobEntity.playAmbientSound();
                else if(entity instanceof PlayerEntity playerEntity) playerEntity.playSound(SoundEvents.ENTITY_GENERIC_EAT.value());

                user.getItemCooldownManager().set(stack, 4 + (item.duration * 2)/3);
                stack.decrement(1);
                return ActionResult.CONSUME;
            }
        }
        return ActionResult.PASS;
    }
}