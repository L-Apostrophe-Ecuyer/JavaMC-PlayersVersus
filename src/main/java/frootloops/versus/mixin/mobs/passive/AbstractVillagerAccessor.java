package frootloops.versus.mixin.mobs.passive;

import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Sets a villager's or trader's trades. Vanilla's overrideOffers is for clients (it does nothing on the server), so a
 * pale wandering trader cured has its trades put back this way (ZombieVillagerMixin).
 */
@Mixin(AbstractVillager.class)
public interface AbstractVillagerAccessor {

    @Accessor("offers")
    void playersVersus$setOffers(MerchantOffers offers);
}
