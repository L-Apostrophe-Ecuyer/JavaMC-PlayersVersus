package frootloops.versus.mixin.mobs.passive;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import frootloops.versus.mod.mobs.passive.RevampedTradeOffers;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.village.*;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.HashSet;

import static frootloops.versus.mod.mobs.passive.RevampedTradeOffers.REVAMPED_PROFESSION_TO_LEVELED_TRADE;

@Mixin(WanderingTraderEntity.class)
public abstract class WanderingTraderEntityMixin extends MerchantEntity implements VillagerDataContainer {
    public WanderingTraderEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void fillRecipes() {
        TradeOfferList tradeOfferList = this.getOffers();
        for (Pair<RevampedTradeOffers.Factory[], Integer> pair : RevampedTradeOffers.REBALANCED_WANDERING_TRADER_TRADES) {

            ArrayList<RevampedTradeOffers.Factory> offersList = Lists.newArrayList(pair.getLeft());
            int count = pair.getRight() - 1 + this.random.nextBetweenExclusive(0, 3);

            int i = 0;
            while (i < count && !offersList.isEmpty()) {
                TradeOffer tradeOffer = offersList.remove(this.random.nextInt(offersList.size())).create(this, this.random);
                if (tradeOffer == null) continue;
                tradeOfferList.add(tradeOffer);
                ++i;
            }
        }
    }
}
