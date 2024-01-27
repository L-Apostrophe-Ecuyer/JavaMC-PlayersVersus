package frootloops.versus.mixin.players.accessors;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.MiningToolItem;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MiningToolItem.class)
public interface MiningToolAccessor {
    @Accessor
    @Mutable
    void setAttackDamage(float newAttackDamage);

    @Accessor
    @Mutable
    void setAttributeModifiers(Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> newAttributeModifiers);
}