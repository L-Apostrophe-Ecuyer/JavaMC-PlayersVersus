package frootloops.versus.mixin.enchantments.tools;

import net.minecraft.enchantment.DamageEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.TagKey;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import Rarity;
import java.util.Optional;

@Mixin(DamageEnchantment.class)
public class SharpnessMixin extends Enchantment {
    @Shadow
    private final Optional<TagKey<EntityType<?>>> applicableEntities;

    protected SharpnessMixin(Rarity rarity, TagKey<Item> applicableItems, EquipmentSlot[] slotTypes, Optional<TagKey<EntityType<?>>> applicableEntities) {
        super(rarity, applicableItems, slotTypes);
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
        if (this.applicableEntities.isPresent() && target instanceof LivingEntity livingEntity) {
            if (this.applicableEntities.get() == EntityTypeTags.ARTHROPOD && level > 0 && livingEntity.getType().isIn((TagKey)this.applicableEntities.get())) {
                int i = 20 + user.getRandom().nextInt(10 * level);
                livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, i, 3));
            }
        }
    }

    @Override
    public int getMinPower(int level) {
        int basePower = this.applicableEntities.isEmpty() ? 10 : 16;
        int powerPerLevel = this.applicableEntities.isEmpty() ? 12 : 6;
        return basePower + (level - 1) * powerPerLevel;
    }

    @Override
    public Rarity getRarity() {
        return this.applicableEntities.isPresent() ? Rarity.RARE : Rarity.VERY_RARE;
    }


    @Override
    public boolean isAcceptableItem(ItemStack stack) {
        return (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem || stack.getItem() instanceof TridentItem);
    }

}