package frootloops.versus.mixin.players;

import com.google.common.collect.Multimap;
import frootloops.versus.util.Enchants;
import frootloops.versus.util.enchantments.TossingEnchantment;
import frootloops.versus.util.Combat;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world, ItemCooldownManager itemCooldownManager) {
        super(entityType, world);
        this.itemCooldownManager = itemCooldownManager;
    }

    @Shadow
    private final ItemCooldownManager itemCooldownManager;

    @Shadow public int totalExperience;


    @Inject(method = "createPlayerAttributes", at = @At(value = "HEAD"), cancellable = true)
    private static void createPlayerAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {

        cir.setReturnValue(LivingEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, Combat.PLAYER_BASE_ATTACK_DAMAGE)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.10000000149011612)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, Combat.PLAYER_BASE_ATTACK_SPEED)
                .add(EntityAttributes.GENERIC_LUCK));
    }

    @Inject(method = "getXpToDrop", at = @At("RETURN"), cancellable = true)
    public void getXpToDrop(CallbackInfoReturnable<Integer> cir) {
        if (this.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
            cir.setReturnValue(0);
        } else {
            cir.setReturnValue(((64 + this.totalExperience) >> 3) + (this.totalExperience >> 1));
        }
    }

    @ModifyVariable(method = "damage", ordinal = 0, at = @At("HEAD"))
    private float rebalancedDamage(float amount2, DamageSource source, float amount) {

        // Falling doesn't hurt as much:
        if (source.isIn(DamageTypeTags.IS_FALL))//(source == DamageSource.FALL)
            return amount/1.75f;

        // Hitting blocks while flying no longer neglects helmet protection:
        if(source.method_49708(DamageTypes.FLY_INTO_WALL)) {//(source == DamageSource.FLY_INTO_WALL) {
            ItemStack helmet = this.getEquippedStack(EquipmentSlot.HEAD);
            if(helmet != null) {
                Multimap<EntityAttribute, EntityAttributeModifier> helmetAttributeModifiers = helmet.getAttributeModifiers(EquipmentSlot.HEAD);

                float armorAmount = 0.0f;
                for (EntityAttributeModifier modifier:helmetAttributeModifiers.get(EntityAttributes.GENERIC_ARMOR))
                    armorAmount += modifier.getValue();

                float toughnessAmount = 0.0f;
                for (EntityAttributeModifier modifier:helmetAttributeModifiers.get(EntityAttributes.GENERIC_ARMOR_TOUGHNESS))
                    toughnessAmount += modifier.getValue();

                float protectionAmount = (float)Math.max(EnchantmentHelper.getLevel(Enchants.PHYSICAL_PROTECTION, helmet), EnchantmentHelper.getLevel(Enchantments.PROTECTION, helmet));
                if (protectionAmount > 0) amount = DamageUtil.getInflictedDamage(amount, protectionAmount);

                return DamageUtil.getDamageLeft(amount, armorAmount, toughnessAmount);
            }
        }
        return amount;
    }

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;dropShoulderEntities()V"))
    private void onDamageInterruptEating(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getAttacker() != null && amount > 1.0F) {
            Item item = this.activeItemStack.getItem();
            if (item.isFood() || item instanceof PotionItem) {
                this.clearActiveItem();
                itemCooldownManager.set(item, 16);
            }
        }
    }

    @Inject(method = "attack", at = @At("TAIL"))
    public void attack(Entity target, CallbackInfo ci) {
        int frostAspect = EnchantmentHelper.getEquipmentLevel(Enchants.FROST_ASPECT, this);
        boolean isFrostAttack = (target instanceof LivingEntity && frostAspect > 0);
        if (isFrostAttack) {
            this.world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, this.getSoundCategory(), 1.0f, 1.0f);
            target.extinguish();

            if(target.canFreeze()) {
                // Minimum ticks to get damaged is 140, for most. Entities get rid of 2 FrozenTicks per tick.
                target.setFrozenTicks(target.getFrozenTicks() + 220);
                if(this.world instanceof ServerWorld) {
                    this.world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_HURT_FREEZE, this.getSoundCategory(), 1.0f, 1.0f);
                    ((ServerWorld)this.world).spawnParticles(ParticleTypes.WAX_OFF, target.getX(), target.getY() + 1, target.getZ(), 4, 0.2, 0.2, 0.2, 6.0f);
                }
            }
        }

        boolean isToss = !this.isSneaking() && this.onGround && this.getMainHandStack().getItem() instanceof ShovelItem;
        if (isToss) {
            TossingEnchantment.performTossAttack(this, target, 0.125 + 0.125 * (double)EnchantmentHelper.getEquipmentLevel(Enchants.TOSSING, this));
        }
    }
}