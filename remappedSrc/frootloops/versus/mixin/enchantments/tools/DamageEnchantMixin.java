package frootloops.versus.mixin.enchantments.tools;

import net.minecraft.enchantment.DamageEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(DamageEnchantment.class)
public class DamageEnchantMixin extends Enchantment {

    @Shadow
    private final Optional<TagKey<EntityType<?>>> applicableEntities;

    public DamageEnchantMixin(Properties properties, Optional<TagKey<EntityType<?>>> applicableEntities) {
        super(properties);
        this.applicableEntities = applicableEntities;
    }

    @Override
    public float getAttackDamage(int level, @Nullable EntityType<?> entityType) {
        if (this.applicableEntities.isEmpty()) {
            return 0.5F + (float)Math.max(0, level - 1) * 0.5F;
        } else {
            return entityType != null && entityType.isIn((TagKey)this.applicableEntities.get()) ? (float)level * 2.5F : 0.0F;
        }
    }

    @Override
    public void onTargetDamaged(LivingEntity user, Entity target, int level) {
        if(this == Enchantments.IMPALING) {
            if(target.isTouchingWaterOrRain()) {
                float extraDamageToWetMobs = level; // Note: for some reason, this is called twice. So I've reduced the damage.
                target.damage(user.getDamageSources().trident(user, user), extraDamageToWetMobs);
                user.playSound(SoundEvents.ITEM_TRIDENT_HIT, 1.1f, 1.0f);
            }
        }
        else if (this.applicableEntities.isPresent() && target instanceof LivingEntity livingEntity) {
            if (this.applicableEntities.get() == EntityTypeTags.ARTHROPOD && level > 0 && livingEntity.getType().isIn((TagKey)this.applicableEntities.get())) {
                int i = 20 + user.getRandom().nextInt(10 * level);
                livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, i, 3));
            }
        }
    }

    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        Enchantment self = (Enchantment) ((Object)this);
        if (self == Enchantments.IMPALING) return stack.getItem() instanceof TridentItem;
        return super.isAcceptableItem(stack);
    }

    @Override
    public boolean isAvailableForRandomSelection() {
        return (this != Enchantments.BANE_OF_ARTHROPODS); // Effectively disabled, because it objectively sucks
    }
}