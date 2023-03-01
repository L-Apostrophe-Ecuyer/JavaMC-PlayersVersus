package frootloops.versus.mixin.players.accessors;

import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;

@Mixin(Item.class)
public interface ItemAccessor {
    @Accessor
    UUID getATTACK_DAMAGE_MODIFIER_ID();
    @Accessor
    UUID getATTACK_SPEED_MODIFIER_ID();

    @Accessor
    @Mutable
    void setMaxCount(int newCount);

}