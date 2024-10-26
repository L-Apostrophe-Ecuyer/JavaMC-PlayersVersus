package frootloops.versus.mixin.mobs.passive;

import com.google.common.collect.Sets;
import frootloops.versus.mod.mobs.passive.RevampedTradeOffers;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.village.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

import static frootloops.versus.mod.mobs.passive.RevampedTradeOffers.REVAMPED_PROFESSION_TO_LEVELED_TRADE;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity implements VillagerDataContainer {

    @Shadow private int experience;

    @Nullable
    private PlayerEntity customer, lastCustomer;

    @Shadow private int levelUpTimer;
    @Shadow  private boolean levelingUp;

    @Shadow private boolean canLevelUp() {
        int i = this.getVillagerData().getLevel();
        return VillagerData.canLevelUp(i) && this.experience >= VillagerData.getUpperLevelExperience(i);
    }

    public VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }


    @Override
    public void fillRecipes() {
        VillagerData villagerData = this.getVillagerData();
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
        int numTradesAdded = (villagerData.getLevel() > 1 ? 3 : 4) + random.nextBetween(0, 1);

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

    @Inject(method = "talkWithVillager", at = @At("TAIL"))
    public void talkWithVillager(ServerWorld world, VillagerEntity villager, long time, CallbackInfo info) {
        this.experience += 3;
    }

    @Override
    public void afterUsing(TradeOffer offer) {
        this.experience = this.experience + offer.getMerchantExperience();
        this.lastCustomer = customer;
        if (this.canLevelUp()) {
            this.levelUpTimer = 40;
            this.levelingUp = true;
        }
    }
}
