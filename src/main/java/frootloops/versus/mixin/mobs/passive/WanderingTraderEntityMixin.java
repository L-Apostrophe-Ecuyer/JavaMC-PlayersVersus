package frootloops.versus.mixin.mobs.passive;

import com.google.common.collect.Lists;
import frootloops.versus.mod.environment.WorldTime;
import frootloops.versus.mod.mobs.passive.RevampedTradeFactories;
import frootloops.versus.mod.mobs.passive.RevampedVillagerOffers;
import frootloops.versus.mod.mobs.passive.RevampedWandererOffers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerDataHolder;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;


@Mixin(WanderingTrader.class)
public abstract class WanderingTraderEntityMixin extends AbstractVillager implements VillagerDataHolder {
    public WanderingTraderEntityMixin(EntityType<? extends AbstractVillager> entityType, Level world) {
        super(entityType, world);
    }

    private int experience = 0;

    @Override
    protected void updateTrades(ServerLevel level) {
        MerchantOffers tradeOfferList = this.getOffers();
        for (Pair<RevampedTradeFactories.Factory[], Integer> pair : RevampedWandererOffers.WANDERING_TRADER_TRADES) {

            ArrayList<RevampedTradeFactories.Factory> offersList = Lists.newArrayList(pair.getLeft());
            int count = pair.getRight() - 1 + this.random.nextInt(0, 3);

            int i = 0;
            while (i < count && !offersList.isEmpty()) {
                MerchantOffer tradeOffer = offersList.remove(this.random.nextInt(offersList.size())).create(this, this.random);
                if (tradeOffer == null) continue;
                tradeOfferList.add(tradeOffer);
                ++i;
            }
        }
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        super.notifyTrade(offer);
        if (offer.shouldRewardExp()) {
            experience += 1 + this.random.nextInt(2);
            if(experience > 0 && this.getTradingPlayer() instanceof ServerPlayer serverPlayer) {
                experience = 0;
                ServerLevel serverWorld = serverPlayer.level();
                if(!WorldTime.hasRaids(serverWorld) || !serverWorld.dimensionType().hasSkyLight() || WorldTime.dayTime(serverWorld) > 12000L) return;

                boolean doesTraderWantToSettleDown = serverWorld.isVillage(this.blockPosition());
                if(!doesTraderWantToSettleDown) {
                    BlockPos spawnPos = serverPlayer.getRespawnConfig().respawnData().pos();
                    if(spawnPos != null) doesTraderWantToSettleDown = (spawnPos.closerThan(this.blockPosition(), 32)) && !serverWorld.getPoiManager().getInRange(poiType -> poiType.is(PoiTypeTags.ACQUIRABLE_JOB_SITE), serverPlayer.blockPosition(), 24, PoiManager.Occupancy.ANY).toList().isEmpty();
                    else doesTraderWantToSettleDown = serverWorld.getPoiManager().getInRange(poiType -> poiType.is(PoiTypeTags.ACQUIRABLE_JOB_SITE), serverPlayer.blockPosition(), 16, PoiManager.Occupancy.ANY).toList().size() > 1;
                }

                if (doesTraderWantToSettleDown) {
                    Villager villagerEntity = this.convertTo(EntityTypes.VILLAGER, ConversionParams.single(this, true, true), stray -> {});

                    int randomProfessionIndex = this.random.nextInt(10);
                    ResourceKey<VillagerProfession> profession = (randomProfessionIndex < 6) ? VillagerProfession.NONE : (randomProfessionIndex < 8) ? VillagerProfession.CARTOGRAPHER : (randomProfessionIndex < 9) ? VillagerProfession.FISHERMAN : VillagerProfession.FARMER;

                    int randomBiomeIndex = this.random.nextInt(10);
                    Holder<Biome> currentBiome = serverWorld.getBiome(this.blockPosition());

                    float currentBiomeTemperature = currentBiome.value().getBaseTemperature();
                    ResourceKey<VillagerType> exoticVillagerType = currentBiome.is(BiomeTags.SPAWNS_WARM_VARIANT_FROGS) ? (randomBiomeIndex < 6 ? VillagerType.JUNGLE : VillagerType.SAVANNA) : currentBiomeTemperature > 0.7f ? (randomBiomeIndex < 6 ? VillagerType.JUNGLE : VillagerType.DESERT) : currentBiomeTemperature > 0.4f ? (randomBiomeIndex < 6 ? VillagerType.SWAMP : VillagerType.PLAINS) : VillagerType.TAIGA;
                    ResourceKey<VillagerType> villagerType = this.random.nextInt(10) < 4 ? VillagerType.byBiome(currentBiome) : exoticVillagerType;

                    Holder<VillagerType> typeEntry = BuiltInRegistries.VILLAGER_TYPE.wrapAsHolder(BuiltInRegistries.VILLAGER_TYPE.getValue(villagerType));
                    Holder<VillagerProfession> jobEntry = BuiltInRegistries.VILLAGER_PROFESSION.wrapAsHolder(BuiltInRegistries.VILLAGER_PROFESSION.getValue(profession));

                    villagerEntity.setVillagerData(new VillagerData(typeEntry, jobEntry, 0));
                    villagerEntity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120));
                }
            }
        }
    }
}
