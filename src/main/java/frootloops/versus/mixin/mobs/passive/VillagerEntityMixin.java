package frootloops.versus.mixin.mobs.passive;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import frootloops.versus.mod.mobs.passive.RevampedTradeFactories;
import frootloops.versus.mod.mobs.passive.RevampedVillagerOffers;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.SharedConstants;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.village.*;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

import static frootloops.versus.mod.mobs.passive.RevampedVillagerOffers.PROFESSION_TO_LEVELED_TRADE;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin extends MerchantEntity implements VillagerDataContainer {

    @Shadow private int experience;

    @Shadow private long lastRestockTime;
    @Shadow abstract public void restock();
    @Shadow abstract boolean needsRestock();
    @Nullable
    private PlayerEntity customer, lastCustomer;

    @Shadow private int levelUpTimer;
    @Shadow  private boolean levelingUp;

    @Shadow private boolean canLevelUp() {
        int i = this.getVillagerData().level();
        return VillagerData.canLevelUp(i) && this.experience >= VillagerData.getUpperLevelExperience(i);
    }

    public VillagerEntityMixin(EntityType<? extends MerchantEntity> entityType, World world) {
        super(entityType, world);
    }

    private static HashMap<RegistryKey<VillagerProfession>, RegistryKey<VillagerProfession>[]> PROFESSION_AFFINITY_MAP = new HashMap<>();
    static {
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.ARMORER, new RegistryKey[]{VillagerProfession.WEAPONSMITH, VillagerProfession.LEATHERWORKER, VillagerProfession.LIBRARIAN});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.WEAPONSMITH, new RegistryKey[]{VillagerProfession.ARMORER, VillagerProfession.TOOLSMITH, VillagerProfession.LIBRARIAN});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.TOOLSMITH, new RegistryKey[]{VillagerProfession.ARMORER, VillagerProfession.WEAPONSMITH, VillagerProfession.LIBRARIAN});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.MASON, new RegistryKey[]{VillagerProfession.TOOLSMITH, VillagerProfession.NONE});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.LEATHERWORKER, new RegistryKey[]{VillagerProfession.ARMORER, VillagerProfession.BUTCHER, VillagerProfession.FARMER, VillagerProfession.FISHERMAN, VillagerProfession.SHEPHERD});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.SHEPHERD, new RegistryKey[]{VillagerProfession.BUTCHER, VillagerProfession.FARMER, VillagerProfession.FISHERMAN, VillagerProfession.LEATHERWORKER});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.FISHERMAN, new RegistryKey[]{VillagerProfession.BUTCHER, VillagerProfession.FARMER, VillagerProfession.FLETCHER, VillagerProfession.CARTOGRAPHER});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.LIBRARIAN, new RegistryKey[]{VillagerProfession.CLERIC, VillagerProfession.CARTOGRAPHER, VillagerProfession.FISHERMAN});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.CARTOGRAPHER, new RegistryKey[]{VillagerProfession.LIBRARIAN, VillagerProfession.FISHERMAN});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.CLERIC, new RegistryKey[]{VillagerProfession.NITWIT, VillagerProfession.LIBRARIAN});
    }


    @Override
    public void fillRecipes() {
        VillagerData villagerData = this.getVillagerData();
        RegistryKey<VillagerProfession> professionKey = villagerData.profession().getKey().orElse(null);
        if (professionKey != null) {
            Int2ObjectMap<RevampedTradeFactories.Factory[]> offersMap = PROFESSION_TO_LEVELED_TRADE.get(professionKey);
            if (offersMap != null && !offersMap.isEmpty()) {
                RevampedTradeFactories.Factory[] tradeOfferFactories = offersMap.get(villagerData.level());

                if (tradeOfferFactories != null) {
                    TradeOfferList tradeOfferList = this.getOffers();
                    ArrayList<RevampedTradeFactories.Factory> availableOffers = Lists.newArrayList(tradeOfferFactories);

                    int n = 0;
                    int maxNumOffers = 2 + Math.max(0, 4 - villagerData.level());

                    // Get index of where "buy" offers end:z
                    int buyIndex = 0;
                    while(buyIndex < tradeOfferList.size() && tradeOfferList.get(buyIndex).getSellItem().isOf(Items.EMERALD)) buyIndex++;

                    // Find offers to chose:
                    findOffersToAdd: while (n < maxNumOffers && !availableOffers.isEmpty()) {
                        TradeOffer newOffer = (availableOffers.remove(this.random.nextInt(availableOffers.size()))).create(this, this.random);
                        if (newOffer != null) {

                            // Make sure the offer isn't already being sold:
                            for (TradeOffer currentOffer:tradeOfferList) {
                                if (ItemStack.areItemsAndComponentsEqual(currentOffer.getSellItem(), newOffer.getSellItem())
                                        && ItemStack.areItemsAndComponentsEqual(currentOffer.getFirstBuyItem().itemStack(), newOffer.getFirstBuyItem().itemStack()) ) {
                                    continue findOffersToAdd;
                                }
                            }

                            // Add!
                            if(newOffer.getSellItem().isOf(Items.EMERALD)) tradeOfferList.add(buyIndex, newOffer);
                            else tradeOfferList.addLast(newOffer);
                            n++;
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "talkWithVillager", at = @At("TAIL"))
    public void talkWithVillager(ServerWorld world, VillagerEntity partner, long time, CallbackInfo info) {
        RegistryKey<VillagerProfession> myProfession = this.getVillagerData().profession().getKey().get();
        if(myProfession == VillagerProfession.NONE || myProfession == VillagerProfession.NITWIT) return;

        int affinityAmount = 0;
        if(this.needsRestock()) {
            RegistryKey<VillagerProfession>[] listOfGoodProfessions = PROFESSION_AFFINITY_MAP.getOrDefault(myProfession, null);
            if (listOfGoodProfessions != null) {
                RegistryKey<VillagerProfession> partnerProfession = partner.getVillagerData().profession().getKey().get();
                if(partnerProfession == myProfession){
                    this.restock();
                    affinityAmount = 9;
                }
                else {
                    for(RegistryKey<VillagerProfession> p : listOfGoodProfessions) {
                        if(partnerProfession == p) {
                            this.restock();
                            affinityAmount = 15;
                            break;
                        }
                    }
                }
            }
            lastRestockTime -= 1000L;
        }

        this.experience += 3 + affinityAmount;
        if (this.canLevelUp()) {
            this.levelUpTimer = 40;
            this.levelingUp = true;
        }
    }

    @ModifyConstant(method = "canRestock", constant = @Constant(longValue = 2400L))
    private static long restockTimeWithoutGossiping(long timer) {
        return 6000L;
    }

    @Override
    public void afterUsing(TradeOffer offer) {
        int experienceFromOffer = offer.getMerchantExperience();
        this.experience = this.experience + experienceFromOffer;
        this.lastCustomer = customer;
        if (this.canLevelUp()) {
            this.levelUpTimer = 40;
            this.levelingUp = true;
            this.getEntityWorld().spawnEntity(new ExperienceOrbEntity(this.getEntityWorld(), this.getX(), this.getY() + 0.5, this.getZ(), this.getVillagerData().level() * 8));
        }
        else {
            this.getEntityWorld().spawnEntity(new ExperienceOrbEntity(this.getEntityWorld(), this.getX(), this.getY() + 0.5, this.getZ(), experienceFromOffer));
        }
    }
}
