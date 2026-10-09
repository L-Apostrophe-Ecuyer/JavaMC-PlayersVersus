package frootloops.versus.mixin.mobs.passive;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import frootloops.versus.mod.mobs.passive.RevampedTradeFactories;
import frootloops.versus.mod.mobs.passive.RevampedVillagerOffers;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.SharedConstants;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerDataHolder;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
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

@Mixin(Villager.class)
public abstract class VillagerEntityMixin extends AbstractVillager implements VillagerDataHolder {

    @Shadow private int villagerXp;

    @Shadow private long lastRestockGameTime;
    @Shadow abstract public void restock();
    @Shadow abstract boolean needsToRestock();
    @Shadow @Nullable private Player lastTradedPlayer;
    @Shadow private void updateSpecialPrices(Player player) {}

    // 26.3 levels a villager up right away (1.21.10 waited 40 ticks, with updateMerchantTimer and
    // increaseProfessionLevelOnUpdate), with the same regeneration.
    @Shadow private void increaseMerchantCareer(ServerLevel level) {}

    @Shadow private boolean shouldIncreaseLevel() {
        int i = this.getVillagerData().level();
        return VillagerData.canLevelUp(i) && this.villagerXp >= VillagerData.getMaxXpPerLevel(i);
    }

    public VillagerEntityMixin(EntityType<? extends AbstractVillager> entityType, Level world) {
        super(entityType, world);
    }

    private static HashMap<ResourceKey<VillagerProfession>, ResourceKey<VillagerProfession>[]> PROFESSION_AFFINITY_MAP = new HashMap<>();
    static {
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.ARMORER, new ResourceKey[]{VillagerProfession.WEAPONSMITH, VillagerProfession.LEATHERWORKER, VillagerProfession.LIBRARIAN});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.WEAPONSMITH, new ResourceKey[]{VillagerProfession.ARMORER, VillagerProfession.TOOLSMITH, VillagerProfession.LIBRARIAN});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.TOOLSMITH, new ResourceKey[]{VillagerProfession.ARMORER, VillagerProfession.WEAPONSMITH, VillagerProfession.LIBRARIAN});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.MASON, new ResourceKey[]{VillagerProfession.TOOLSMITH, VillagerProfession.NONE});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.LEATHERWORKER, new ResourceKey[]{VillagerProfession.ARMORER, VillagerProfession.BUTCHER, VillagerProfession.FARMER, VillagerProfession.FISHERMAN, VillagerProfession.SHEPHERD});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.SHEPHERD, new ResourceKey[]{VillagerProfession.BUTCHER, VillagerProfession.FARMER, VillagerProfession.FISHERMAN, VillagerProfession.LEATHERWORKER});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.FISHERMAN, new ResourceKey[]{VillagerProfession.BUTCHER, VillagerProfession.FARMER, VillagerProfession.FLETCHER, VillagerProfession.CARTOGRAPHER});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.LIBRARIAN, new ResourceKey[]{VillagerProfession.CLERIC, VillagerProfession.CARTOGRAPHER, VillagerProfession.FISHERMAN});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.CARTOGRAPHER, new ResourceKey[]{VillagerProfession.LIBRARIAN, VillagerProfession.FISHERMAN});
        PROFESSION_AFFINITY_MAP.put(VillagerProfession.CLERIC, new ResourceKey[]{VillagerProfession.NITWIT, VillagerProfession.LIBRARIAN});
    }


    @Override
    protected void updateTrades(ServerLevel level) {
        VillagerData villagerData = this.getVillagerData();
        ResourceKey<VillagerProfession> professionKey = villagerData.profession().unwrapKey().orElse(null);
        if (professionKey != null) {
            Int2ObjectMap<RevampedTradeFactories.Factory[]> offersMap = PROFESSION_TO_LEVELED_TRADE.get(professionKey);
            if (offersMap != null && !offersMap.isEmpty()) {
                RevampedTradeFactories.Factory[] tradeOfferFactories = offersMap.get(villagerData.level());

                if (tradeOfferFactories != null) {
                    MerchantOffers tradeOfferList = this.getOffers();
                    ArrayList<RevampedTradeFactories.Factory> availableOffers = Lists.newArrayList(tradeOfferFactories);

                    int n = 0;
                    int maxNumOffers = 2 + Math.max(0, 4 - villagerData.level());


                    // Get index of where "conversion" offers end:
                    int convertIndex = 0;
                    while(convertIndex < tradeOfferList.size() && tradeOfferList.get(convertIndex).getItemCostB().isPresent()) convertIndex++;

                    // Get index of where "buy" offers end:
                    int buyIndex = convertIndex;
                    while(buyIndex < tradeOfferList.size() && tradeOfferList.get(buyIndex).getResult().is(Items.EMERALD)) buyIndex++;

                    // Find offers to chose:
                    findOffersToAdd: while (n < maxNumOffers && !availableOffers.isEmpty()) {
                        MerchantOffer newOffer = (availableOffers.remove(this.random.nextInt(availableOffers.size()))).create(this, this.random);
                        if (newOffer != null) {
                            // A book's enchantment is a stored one, which isEnchanted and getEnchantments don't read:
                            ItemEnchantments bookEnchantments = newOffer.getResult().is(Items.ENCHANTED_BOOK) ? EnchantmentHelper.getEnchantmentsForCrafting(newOffer.getResult()) : ItemEnchantments.EMPTY;
                            boolean isEnchantedBook = !bookEnchantments.isEmpty();
                            Holder<Enchantment> enchant = null;
                            if(isEnchantedBook) {
                                enchant = bookEnchantments.keySet().iterator().next();
                            }

                            // Make sure the offer isn't already being sold (for a book, its enchantment at any level):
                            for (MerchantOffer currentOffer:tradeOfferList) {
                                if(isEnchantedBook && enchant != null) {
                                    if(currentOffer.getResult().is(Items.ENCHANTED_BOOK) && EnchantmentHelper.getEnchantmentsForCrafting(currentOffer.getResult()).getLevel(enchant) > 0)
                                        continue findOffersToAdd;
                                }
                                else if (ItemStack.isSameItemSameComponents(currentOffer.getResult(), newOffer.getResult())
                                        && ItemStack.isSameItemSameComponents(currentOffer.getItemCostA().itemStack(), newOffer.getItemCostA().itemStack()) ) {
                                    continue findOffersToAdd;
                                }
                            }

                            // Add!
                            if(newOffer.getItemCostB().isPresent()) {
                                tradeOfferList.add(convertIndex, newOffer);
                                buyIndex++;
                            }
                            else if(newOffer.getResult().is(Items.EMERALD)) tradeOfferList.add(buyIndex, newOffer);
                            else tradeOfferList.addLast(newOffer);
                            n++;
                        }
                    }
                }
            }
        }

        // A trade levels a villager up there and then on 26.3: as vanilla's updateTrades does, price the new offers for
        // the player still trading, which also sends them the new list (else they'd pick from the old one, by index).
        Player tradingPlayer = this.getTradingPlayer();
        if (tradingPlayer != null) this.updateSpecialPrices(tradingPlayer);
    }

    @Inject(method = "gossip", at = @At("TAIL"))
    public void talkWithVillager(ServerLevel world, Villager partner, long time, CallbackInfo info) {
        ResourceKey<VillagerProfession> myProfession = this.getVillagerData().profession().unwrapKey().get();
        if(myProfession == VillagerProfession.NONE || myProfession == VillagerProfession.NITWIT) return;

        int affinityAmount = 0;
        if(this.needsToRestock()) {
            ResourceKey<VillagerProfession>[] listOfGoodProfessions = PROFESSION_AFFINITY_MAP.getOrDefault(myProfession, null);
            if (listOfGoodProfessions != null) {
                ResourceKey<VillagerProfession> partnerProfession = partner.getVillagerData().profession().unwrapKey().get();
                if(partnerProfession == myProfession){
                    this.restock();
                    affinityAmount = 9;
                }
                else {
                    for(ResourceKey<VillagerProfession> p : listOfGoodProfessions) {
                        if(partnerProfession == p) {
                            this.restock();
                            affinityAmount = 15;
                            break;
                        }
                    }
                }
            }
            lastRestockGameTime -= 1000L;
        }

        this.villagerXp += 3 + affinityAmount;
        if (this.shouldIncreaseLevel()) {
            this.increaseMerchantCareer(world);
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
        }
    }

    @ModifyConstant(method = "allowedToRestock", constant = @Constant(longValue = 2400L))
    private static long restockTimeWithoutGossiping(long timer) {
        return 6000L;
    }

    @Override
    public void rewardTradeXp(MerchantOffer offer) {
        int experienceFromOffer = offer.getXp();
        this.villagerXp = this.villagerXp + experienceFromOffer;
        // As vanilla's: the villager's next tick turns the trade into reputation with this player, with happy particles.
        this.lastTradedPlayer = this.getTradingPlayer();
        if (this.shouldIncreaseLevel()) {
            if (this.level() instanceof ServerLevel serverLevel) this.increaseMerchantCareer(serverLevel);
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
            this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), this.getVillagerData().level() * 8));
        }
        else {
            this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), experienceFromOffer));
        }
    }
}
