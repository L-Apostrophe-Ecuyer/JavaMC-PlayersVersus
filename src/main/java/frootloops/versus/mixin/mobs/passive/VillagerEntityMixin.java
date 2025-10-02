package frootloops.versus.mixin.mobs.passive;

import com.google.common.collect.Sets;
import frootloops.versus.mod.mobs.passive.RevampedTradeFactories;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
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
        Int2ObjectMap<RevampedTradeFactories.Factory[]> int2ObjectMap = PROFESSION_TO_LEVELED_TRADE.get(villagerData.profession());
        if (int2ObjectMap == null || int2ObjectMap.isEmpty()) {
            return;
        }
        RevampedTradeFactories.Factory[] newTradesAvailable = int2ObjectMap.get(villagerData.level());
        if (newTradesAvailable == null) {
            return;
        }
        TradeOfferList tradeOfferList = getOffers();

        HashSet<Integer> set = Sets.newHashSet();
        int numTradesAdded = (villagerData.level() > 1 ? 3 : 4) + random.nextBetween(0, 1);

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
            RevampedTradeFactories.Factory factory = newTradesAvailable[integer];
            TradeOffer tradeOffer = factory.create(this, this.random);
            if (tradeOffer == null) continue;
            tradeOfferList.add(tradeOffer);
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
