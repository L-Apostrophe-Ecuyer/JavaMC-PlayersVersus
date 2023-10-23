package frootloops.versus.mixin.trading;

import com.google.common.collect.Sets;
import frootloops.versus.mod.passive_mobs.RevampedTradeOffers;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.*;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.*;

import static frootloops.versus.mod.passive_mobs.RevampedTradeOffers.REVAMPED_PROFESSION_TO_LEVELED_TRADE;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity {
    public VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }

    @Shadow
    private static final TrackedData<VillagerData> VILLAGER_DATA = DataTracker.registerData(VillagerEntity.class, TrackedDataHandlerRegistry.VILLAGER_DATA);

    @Override
    public void fillRecipes() {
        VillagerData villagerData = this.dataTracker.get(VILLAGER_DATA);;
        Int2ObjectMap<RevampedTradeOffers.Factory[]> int2ObjectMap = REVAMPED_PROFESSION_TO_LEVELED_TRADE.get(villagerData.getProfession());
        if (int2ObjectMap == null || int2ObjectMap.isEmpty()) {
            return;
        }
        RevampedTradeOffers.Factory[] newTradesAvailable = int2ObjectMap.get(villagerData.getLevel());
        if (newTradesAvailable == null) {
            return;
        }
        TradeOfferList tradeOfferList = getOffers();

        HashSet<Integer> set = Sets.newHashSet();
        int numTradesAdded = villagerData.getLevel() > 1 ? 3 : 4;

        if (newTradesAvailable.length > numTradesAdded) {
            while (set.size() < numTradesAdded) {
                set.add(this.random.nextInt(newTradesAvailable.length));
            }
        } else {
            for (int i = 0; i < newTradesAvailable.length; ++i) {
                set.add(i);
            }
        }
        for (Integer integer : set) {
            RevampedTradeOffers.Factory factory = newTradesAvailable[integer];
            TradeOffer tradeOffer = factory.create(this, this.random);
            if (tradeOffer == null) continue;
            tradeOfferList.add(tradeOffer);
        }
    }
}
