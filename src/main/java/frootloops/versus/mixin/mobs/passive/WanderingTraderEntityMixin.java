package frootloops.versus.mixin.mobs.passive;

import com.google.common.collect.Lists;
import frootloops.versus.mod.mobs.passive.RevampedTradeOffers;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.PointOfInterestTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.*;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;


@Mixin(WanderingTraderEntity.class)
public abstract class WanderingTraderEntityMixin extends MerchantEntity implements VillagerDataContainer {
    public WanderingTraderEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }

    //VillagerType SETTLING_VILLAGER_TYPE = Registry.register(Registries.VILLAGER_TYPE, Identifier.ofVanilla("settler"), new VillagerType("settler"));

    private int experience = 0;

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

    @Override
    public void trade(TradeOffer offer) {
        super.trade(offer);
        if (offer.shouldRewardPlayerExperience()) {
            experience += 1 + this.random.nextInt(2);
            if(experience > 0 && this.getCustomer() instanceof ServerPlayerEntity serverPlayer) {
                experience = 0;
                ServerWorld serverWorld = serverPlayer.getWorld();
                if(!serverWorld.getDimension().hasRaids() || !serverWorld.getDimension().hasSkyLight() || serverWorld.getTimeOfDay() > 12000L) return;

                boolean doesTraderWantToSettleDown = serverWorld.isNearOccupiedPointOfInterest(this.getBlockPos());
                if(!doesTraderWantToSettleDown) {
                    BlockPos spawnPos = serverPlayer.getRespawn().pos();
                    if(spawnPos != null) doesTraderWantToSettleDown = (spawnPos.isWithinDistance(this.getBlockPos(), 32)) && !serverWorld.getPointOfInterestStorage().getInCircle(poiType -> poiType.isIn(PointOfInterestTypeTags.ACQUIRABLE_JOB_SITE), serverPlayer.getBlockPos(), 24, PointOfInterestStorage.OccupationStatus.ANY).toList().isEmpty();
                    else doesTraderWantToSettleDown = serverWorld.getPointOfInterestStorage().getInCircle(poiType -> poiType.isIn(PointOfInterestTypeTags.ACQUIRABLE_JOB_SITE), serverPlayer.getBlockPos(), 16, PointOfInterestStorage.OccupationStatus.ANY).toList().size() > 1;
                }

                if (doesTraderWantToSettleDown) {
                    VillagerEntity villagerEntity = this.convertTo(EntityType.VILLAGER, EntityConversionContext.create(this, true, true), stray -> {});

                    int randomProfessionIndex = this.random.nextInt(10);
                    RegistryKey<VillagerProfession> profession = (randomProfessionIndex < 6) ? VillagerProfession.NONE : (randomProfessionIndex < 8) ? VillagerProfession.CARTOGRAPHER : (randomProfessionIndex < 9) ? VillagerProfession.FISHERMAN : VillagerProfession.FARMER;

                    int randomBiomeIndex = this.random.nextInt(10);
                    RegistryEntry<Biome> currentBiome = serverWorld.getBiome(this.getBlockPos());

                    float currentBiomeTemperature = currentBiome.value().getTemperature();
                    RegistryKey<VillagerType> exoticVillagerType = currentBiome.isIn(BiomeTags.SPAWNS_WARM_VARIANT_FROGS) ? (randomBiomeIndex < 6 ? VillagerType.JUNGLE : VillagerType.SAVANNA) : currentBiomeTemperature > 0.7f ? (randomBiomeIndex < 6 ? VillagerType.JUNGLE : VillagerType.DESERT) : currentBiomeTemperature > 0.4f ? (randomBiomeIndex < 6 ? VillagerType.SWAMP : VillagerType.PLAINS) : VillagerType.TAIGA;
                    RegistryKey<VillagerType> villagerType = this.random.nextInt(10) < 4 ? VillagerType.forBiome(currentBiome) : exoticVillagerType;

                    RegistryEntry<VillagerType> typeEntry = Registries.VILLAGER_TYPE.getEntry(Registries.VILLAGER_TYPE.get(villagerType));
                    RegistryEntry<VillagerProfession> jobEntry = Registries.VILLAGER_PROFESSION.getEntry(Registries.VILLAGER_PROFESSION.get(profession));

                    villagerEntity.setVillagerData(new VillagerData(typeEntry, jobEntry, 0));
                    villagerEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 120));
                }
            }
        }
    }
}
