package frootloops.versus.mixin.players.accessors;

import com.google.common.collect.Multimap;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TridentItem.class)
public interface TridentAccessor {
    @Accessor
    @Mutable
    void setAttributeModifiers(Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> newAttributeModifiers);
}