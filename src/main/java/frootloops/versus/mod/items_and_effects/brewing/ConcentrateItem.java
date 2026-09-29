package frootloops.versus.mod.items_and_effects.brewing;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import java.util.List;


public class ConcentrateItem extends BlockItem {

    public static final Consumable CONCENTRATE_COMPONENT = Consumables.defaultFood().consumeSeconds(2.0f).onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.HUNGER, 120, 2),new MobEffectInstance(MobEffects.NAUSEA, 140, 2)))).build();

    protected final Holder<MobEffect> effect;
    protected final int amplifier;
    protected final int duration;

    private final ColorParticleOption particle;

    public ConcentrateItem(Properties settings, Holder<MobEffect> registeredEffect, Block block) {
        this(settings, registeredEffect, 0, registeredEffect.value().isInstantenous() ? 1 : 30, block);
    }

    public ConcentrateItem(Properties settings, Holder<MobEffect> registeredEffect, int amplifier, int duration, Block block) {
        super(block, settings);
        if(registeredEffect != null) {
            this.effect = registeredEffect;
            this.amplifier = amplifier;
            this.duration = duration;
            this.particle = ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, effect.value().getColor());
        }
        else {
            this.effect = null;
            this.amplifier = 0;
            this.duration = 0;
            this.particle = null;
        }
    }

    public Holder<MobEffect> getEffect() {
        return effect;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 60;
    }

    @Override
    public void onUseTick(Level world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if(remainingUseTicks % 8 == 0) user.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 16, 4));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        ConcentrateItem item = (ConcentrateItem) stack.getItem();
        if(user.getRandom().nextInt(10) < 7 && item.effect != null) {
            user.addEffect(new MobEffectInstance(item.effect, item.duration, item.amplifier));
            user.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 1));
        }
        else user.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
        stack.shrink(1);
        return super.finishUsingItem(stack,world,user);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        ConcentrateItem item = (ConcentrateItem) stack.getItem();
        if(item.effect != null) {
            MobEffectInstance effectInstance = new MobEffectInstance(item.effect, (item.duration * 2)/3, 0);
            if (entity.canBeAffected(effectInstance)) {
                if(user.level() instanceof ServerLevel serverWorld) {
                    if(effect.value().isInstantenous()) {
                        if(user.getRandom().nextInt(10) < 7) {
                            if (item.effect == MobEffects.INSTANT_DAMAGE)
                                entity.hurtServer(serverWorld, serverWorld.damageSources().source(DamageTypes.MAGIC, user), 1.0F);
                            else if (item.effect == MobEffects.INSTANT_DAMAGE) entity.heal(1.0F);
                        }
                    }
                    else {
                        entity.addEffect(effectInstance);
                    }
                }
                for (int i = 0; i < 20; i++) {
                    double d = entity.getRandom().nextGaussian() * 0.02;
                    double e = entity.getRandom().nextGaussian() * 0.02;
                    double f = entity.getRandom().nextGaussian() * 0.02;
                    entity.level().addParticle(this.particle, entity.getRandomX(1.0) - d * 10.0, entity.getRandomY() - e * 10.0, entity.getRandomZ(1.0) - f * 10.0, d, e, f);
                }
                if(entity instanceof Mob mobEntity) mobEntity.playAmbientSound();
                else if(entity instanceof Player playerEntity) playerEntity.makeSound(SoundEvents.GENERIC_EAT.value());

                user.getCooldowns().addCooldown(stack, 4 + (item.duration * 2)/3);
                stack.shrink(1);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }
}