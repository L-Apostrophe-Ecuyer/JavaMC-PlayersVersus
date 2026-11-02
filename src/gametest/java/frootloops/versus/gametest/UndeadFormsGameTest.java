package frootloops.versus.gametest;

import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.blocks.mushrooms.StagedMushroomBlock;
import frootloops.versus.mod.items_and_effects.CustomBrewingItems;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.items_and_effects.blight.Blight;
import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.mobs.hostile.overworld.PaleVillagerEntity;
import frootloops.versus.mod.mobs.hostile.overworld.PaleWanderingTraderEntity;
import frootloops.versus.mixin.mobs.passive.WolfAccessor;
import frootloops.versus.mod.mobs.hostile.overworld.PaleWolfEntity;
import frootloops.versus.mod.mobs.passive.ZombieChickenEntity;
import frootloops.versus.mod.mobs.passive.ZombieWolfEntity;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.RemoveStatusEffectsConsumeEffect;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * What Blight makes of villagers and animals: whatever dies fully blighted is replaced by its pale form (villagers pale
 * villagers, cured by golden apples; illagers pale zombies; wandering traders pale wandering traders), or, with none, its
 * undead kind (animals with one of their own, still their owner's): a wolf a zombie wolf, and a zombie wolf in its turn a
 * pale wolf, no one's, that hunts and blights. Zombie chickens lay nothing and breed with nothing; the pale and the
 * passive undead are undead. Golden apples cure Blight; grown brown mushroom clusters make mining
 * fatigue.
 */
public class UndeadFormsGameTest {

    private static final String ROOM = "players-versus-gametest:leaves_room"; // 14 x 8 x 8

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 14; x++) for (int z = 0; z < 8; z++) helper.setBlock(x, 0, z, Blocks.STONE.defaultBlockState());
    }

    private static AABB room(GameTestHelper helper) {
        return AABB.encapsulatingFullBlocks(helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(13, 7, 7)));
    }

    /** Blight enough to blight all the entity's hearts. */
    private static MobEffectInstance fullBlight(LivingEntity entity) {
        return new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, Blight.hearts(entity.getMaxHealth()) - 1);
    }

    /** Fully blighted and dealt a killing blow (of anything: here a plain one), as dying fully blighted is how they turn. */
    private static void killFullyBlighted(GameTestHelper helper, LivingEntity entity) {
        entity.addEffect(fullBlight(entity));
        entity.setHealth(1.0F);
        entity.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 2.0F);
    }

    private static <T extends Mob> List<T> found(GameTestHelper helper, Class<T> type, EntityType<?> exactly) {
        return helper.getLevel().getEntitiesOfClass(type, room(helper), mob -> mob.getType() == exactly);
    }

    /** Fully blighted, a villager wastes away under the Blight, and dies into a pale villager: no zombie villager. */
    @GameTest(structure = ROOM, maxTicks = 900)
    public void aFullyBlightedVillagerWastesIntoAPaleVillager(GameTestHelper helper) {
        floor(helper);
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(3.5, 1.0, 3.5));
        villager.addEffect(fullBlight(villager));
        helper.succeedWhen(() -> {
            helper.assertTrue(villager.isRemoved(), "the villager is still a villager");
            List<PaleVillagerEntity> pale = found(helper, PaleVillagerEntity.class, ModEntities.PALE_VILLAGER);
            helper.assertTrue(pale.size() == 1, pale.size() + " pale villagers, not one");
            helper.assertTrue(found(helper, ZombieVillager.class, EntityTypes.ZOMBIE_VILLAGER).isEmpty(), "a zombie villager rose too");
        });
    }

    /** Killed fully blighted, a farmer turns into a pale villager, still a farmer, its experience kept for its cure. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aFarmerKilledFullyBlightedIsAPaleFarmer(GameTestHelper helper) {
        floor(helper);
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(3.5, 1.0, 3.5));
        villager.setVillagerData(villager.getVillagerData().withProfession(helper.getLevel().registryAccess(), VillagerProfession.FARMER));
        villager.setVillagerXp(5);
        killFullyBlighted(helper, villager);
        helper.assertTrue(villager.isRemoved(), "the fully blighted villager killed is still a villager");
        List<PaleVillagerEntity> pale = found(helper, PaleVillagerEntity.class, ModEntities.PALE_VILLAGER);
        helper.assertTrue(pale.size() == 1, pale.size() + " pale villagers, not one");
        helper.assertTrue(pale.getFirst().getVillagerData().profession().is(VillagerProfession.FARMER), "the pale villager isn't a farmer any more");
        helper.assertTrue(pale.getFirst().getVillagerXp() == 5, "the pale villager lost its experience");
        helper.succeed();
    }

    /** Killed fully blighted, an evoker turns into a pale zombie. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void anEvokerKilledFullyBlightedIsAPaleZombie(GameTestHelper helper) {
        floor(helper);
        Mob evoker = helper.spawnWithNoFreeWill(EntityTypes.EVOKER, new Vec3(3.5, 1.0, 3.5));
        killFullyBlighted(helper, evoker);
        helper.assertTrue(evoker.isRemoved(), "the fully blighted evoker killed is still an evoker");
        helper.assertTrue(found(helper, Mob.class, ModEntities.PALE_ZOMBIE).size() == 1, "no pale zombie rose of the evoker");
        helper.succeed();
    }

    /** A golden apple starts a pale villager's cure, and cured, it's a villager of its trade again. */
    @GameTest(structure = ROOM, maxTicks = 100)
    public void aGoldenAppleCuresAPaleVillager(GameTestHelper helper) {
        floor(helper);
        PaleVillagerEntity pale = helper.spawnWithNoFreeWill(ModEntities.PALE_VILLAGER, new Vec3(3.5, 1.0, 3.5));
        pale.setVillagerData(pale.getVillagerData().withProfession(helper.getLevel().registryAccess(), VillagerProfession.LIBRARIAN));
        ServerPlayer player = FakePlayer.get(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_APPLE));
        pale.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(pale.isConverting(), "the golden apple didn't start the pale villager's cure");
        pale.setVillagerConversionTime(1);
        helper.succeedWhen(() -> {
            helper.assertTrue(pale.isRemoved(), "the pale villager isn't cured yet");
            List<Villager> villagers = found(helper, Villager.class, EntityTypes.VILLAGER);
            helper.assertTrue(villagers.size() == 1, villagers.size() + " villagers, not one");
            helper.assertTrue(villagers.getFirst().getVillagerData().profession().is(VillagerProfession.LIBRARIAN), "the cured villager isn't a librarian");
        });
    }

    /** A golden apple alone starts a zombie villager's cure too (no weakness needed). */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aGoldenAppleStartsAZombieVillagersCure(GameTestHelper helper) {
        floor(helper);
        ZombieVillager zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE_VILLAGER, new Vec3(3.5, 1.0, 3.5));
        ServerPlayer player = FakePlayer.get(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_APPLE));
        zombie.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(zombie.isConverting(), "the golden apple didn't start the zombie villager's cure");
        helper.succeed();
    }

    /** Golden apples (both) cure Blight when eaten. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void goldenApplesCureBlight(GameTestHelper helper) {
        for (Item apple : List.of(Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE)) {
            Consumable consumable = new ItemStack(apple).get(DataComponents.CONSUMABLE);
            helper.assertTrue(consumable != null, apple + " can't be eaten");
            boolean cures = consumable.onConsumeEffects().stream().anyMatch(effect -> effect instanceof RemoveStatusEffectsConsumeEffect remove
                    && remove.effects().contains(CustomStatusEffects.BLIGHT));
            helper.assertTrue(cures, apple + " doesn't cure Blight: " + consumable.onConsumeEffects());
        }
        helper.succeed();
    }

    /** Three brown mushrooms grown together break into a cluster, which smelts into a concentrate of mining fatigue. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aGrownBrownMushroomClusterIsMiningFatiguesSource(GameTestHelper helper) {
        floor(helper);
        BlockPos at = new BlockPos(3, 1, 3);
        helper.setBlock(at, CustomBlocks.BROWN_MUSHROOMS.defaultBlockState().setValue(StagedMushroomBlock.MUSHROOMS, 3));
        helper.getLevel().destroyBlock(helper.absolutePos(at), true);
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, room(helper));
        helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(CustomBrewingItems.BROWN_MUSHROOM_CLUSTER),
                "a grown cluster dropped " + drops.stream().map(ItemEntity::getItem).toList());
        var smelted = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(new ItemStack(CustomBrewingItems.BROWN_MUSHROOM_CLUSTER)), helper.getLevel());
        helper.assertTrue(smelted.isPresent(), "a brown mushroom cluster doesn't smelt");
        helper.succeed();
    }

    /** A child villager dying fully blighted turns into a pale villager child. */
    @GameTest(structure = ROOM, maxTicks = 900)
    public void aFullyBlightedChildVillagerIsAPaleChild(GameTestHelper helper) {
        floor(helper);
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(3.5, 1.0, 3.5));
        villager.setAge(-24000);
        villager.addEffect(fullBlight(villager));
        helper.succeedWhen(() -> {
            helper.assertTrue(villager.isRemoved(), "the child is still a villager");
            List<PaleVillagerEntity> pale = found(helper, PaleVillagerEntity.class, ModEntities.PALE_VILLAGER);
            helper.assertTrue(pale.size() == 1 && pale.getFirst().isBaby(), pale.size() + " pale villagers, not one child");
        });
    }

    /** Dying fully blighted, a chicken (with no pale form) turns into its undead kind, a zombie chicken, grown. */
    @GameTest(structure = ROOM, maxTicks = 900)
    public void aFullyBlightedChickenTurnsZombieChicken(GameTestHelper helper) {
        floor(helper);
        Chicken chick = helper.spawnWithNoFreeWill(EntityTypes.CHICKEN, new Vec3(3.5, 1.0, 3.5));
        chick.setAge(-24000);
        chick.addEffect(fullBlight(chick));
        helper.succeedWhen(() -> {
            helper.assertTrue(chick.isRemoved(), "the chicken is still a chicken");
            List<ZombieChickenEntity> undead = found(helper, ZombieChickenEntity.class, ModEntities.ZOMBIE_CHICKEN);
            helper.assertTrue(undead.size() == 1, undead.size() + " zombie chickens, not one");
            helper.assertTrue(!undead.getFirst().isBaby(), "the zombie chicken is a chick");
            helper.assertTrue(!undead.getFirst().hasEffect(CustomStatusEffects.BLIGHT), "the zombie chicken carries Blight");
        });
    }

    /** Dying fully blighted, a tamed horse turns into a zombie horse still tame and still its owner's. */
    @GameTest(structure = ROOM, maxTicks = 900)
    public void aFullyBlightedTamedHorseStaysItsOwners(GameTestHelper helper) {
        floor(helper);
        ServerPlayer owner = FakePlayer.get(helper.getLevel());
        Horse horse = helper.spawnWithNoFreeWill(EntityTypes.HORSE, new Vec3(3.5, 1.0, 3.5));
        horse.setTamed(true);
        horse.setOwner(owner);
        horse.addEffect(fullBlight(horse));
        helper.succeedWhen(() -> {
            helper.assertTrue(horse.isRemoved(), "the horse is still a horse");
            List<AbstractHorse> undead = found(helper, AbstractHorse.class, EntityTypes.ZOMBIE_HORSE);
            helper.assertTrue(undead.size() == 1, undead.size() + " zombie horses, not one");
            AbstractHorse zombie = undead.getFirst();
            helper.assertTrue(zombie.isTamed(), "the zombie horse isn't tame");
            helper.assertTrue(zombie.getOwnerReference() != null && owner.getUUID().equals(zombie.getOwnerReference().getUUID()),
                    "the zombie horse isn't its owner's");
        });
    }

    /**
     * Dying fully blighted, a tamed wolf turns into a zombie wolf still tame, still its owner's, its collar kept and its
     * health a tame wolf's; grown, and eating only rotten flesh.
     */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aFullyBlightedTamedWolfIsAZombieWolfStillItsOwners(GameTestHelper helper) {
        floor(helper);
        ServerPlayer owner = FakePlayer.get(helper.getLevel());
        Wolf wolf = helper.spawnWithNoFreeWill(EntityTypes.WOLF, new Vec3(3.5, 1.0, 3.5));
        wolf.setTame(true, true);
        wolf.setOwner(owner);
        ((WolfAccessor) wolf).playersVersus$setCollarColor(DyeColor.RED);
        killFullyBlighted(helper, wolf);
        helper.assertTrue(wolf.isRemoved(), "the fully blighted wolf killed is still a wolf");
        List<ZombieWolfEntity> undead = found(helper, ZombieWolfEntity.class, ModEntities.ZOMBIE_WOLF);
        helper.assertTrue(undead.size() == 1, undead.size() + " zombie wolves, not one");
        ZombieWolfEntity zombie = undead.getFirst();
        helper.assertTrue(zombie.isTame(), "the zombie wolf isn't tame");
        helper.assertTrue(zombie.getOwnerReference() != null && owner.getUUID().equals(zombie.getOwnerReference().getUUID()), "the zombie wolf isn't its owner's");
        helper.assertTrue(zombie.getCollarColor() == DyeColor.RED, "the zombie wolf's collar is " + zombie.getCollarColor());
        helper.assertTrue(zombie.getMaxHealth() == 40.0F, "the tame zombie wolf has " + zombie.getMaxHealth() + " max health, not a tame wolf's");
        helper.assertTrue(!zombie.isBaby() && !zombie.hasEffect(CustomStatusEffects.BLIGHT), "the zombie wolf is a pup or carries Blight");
        helper.assertTrue(zombie.isFood(new ItemStack(Items.ROTTEN_FLESH)) && !zombie.isFood(new ItemStack(Items.BEEF)), "the zombie wolf eats more than rotten flesh");
        helper.succeed();
    }

    /** Dying fully blighted in its turn, a tamed zombie wolf turns into a pale wolf, wild: its owner forgotten. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aZombieWolfDyingFullyBlightedIsAPaleWolfNoOnesOwn(GameTestHelper helper) {
        floor(helper);
        ServerPlayer owner = FakePlayer.get(helper.getLevel());
        ZombieWolfEntity zombie = helper.spawnWithNoFreeWill(ModEntities.ZOMBIE_WOLF, new Vec3(3.5, 1.0, 3.5));
        zombie.setTame(true, true);
        zombie.setOwner(owner);
        killFullyBlighted(helper, zombie);
        helper.assertTrue(zombie.isRemoved(), "the fully blighted zombie wolf killed is still a zombie wolf");
        List<PaleWolfEntity> pale = found(helper, PaleWolfEntity.class, ModEntities.PALE_WOLF);
        helper.assertTrue(pale.size() == 1, pale.size() + " pale wolves, not one");
        helper.assertTrue(!pale.getFirst().isTame() && pale.getFirst().getOwnerReference() == null, "the pale wolf is still someone's");
        helper.succeed();
    }

    /** A pale wolf is a monster: it takes no Blight, a bone doesn't tame it, and its bite blights. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aPaleWolfCantBeTamedTakesNoBlightAndItsBiteBlights(GameTestHelper helper) {
        floor(helper);
        PaleWolfEntity pale = helper.spawnWithNoFreeWill(ModEntities.PALE_WOLF, new Vec3(3.5, 1.0, 3.5));
        helper.assertTrue(pale instanceof Enemy, "a pale wolf isn't an enemy");
        helper.assertTrue(!pale.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, 0)), "a pale wolf took Blight");
        ServerPlayer player = FakePlayer.get(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE, 64));
        for (int i = 0; i < 20; i++) helper.assertTrue(pale.mobInteract(player, InteractionHand.MAIN_HAND) == InteractionResult.PASS, "a bone did something to a pale wolf");
        helper.assertTrue(!pale.isTame(), "a pale wolf was tamed");
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(5.5, 1.0, 3.5));
        helper.assertTrue(pale.doHurtTarget(helper.getLevel(), pig), "the pale wolf's bite missed");
        helper.assertTrue(pig.hasEffect(CustomStatusEffects.BLIGHT), "the pale wolf's bite didn't blight");
        helper.succeed();
    }

    /** A zombie chicken whose egg is due lays nothing, where a chicken lays; and it eats nothing (so breeds with nothing). */
    @GameTest(structure = ROOM, maxTicks = 100)
    public void zombieChickensLayNoEggs(GameTestHelper helper) {
        floor(helper);
        Chicken chicken = helper.spawn(EntityTypes.CHICKEN, new Vec3(2.5, 1.0, 3.5));
        ZombieChickenEntity zombie = helper.spawn(ModEntities.ZOMBIE_CHICKEN, new Vec3(11.5, 1.0, 3.5));
        chicken.eggTime = 1;
        zombie.eggTime = 1;
        helper.assertTrue(!zombie.isFood(new ItemStack(Items.WHEAT_SEEDS)), "a zombie chicken eats seeds");
        helper.runAfterDelay(40, () -> {
            AABB room = room(helper);
            List<ItemEntity> items = helper.getLevel().getEntitiesOfClass(ItemEntity.class, room);
            long nearChicken = items.stream().filter(item -> item.getX() < helper.absolutePos(new BlockPos(7, 0, 0)).getX()).count();
            helper.assertTrue(nearChicken >= 1, "the chicken laid nothing either: the test proves nothing");
            helper.assertTrue(items.size() == nearChicken, "the zombie chicken laid " + (items.size() - nearChicken));
            helper.succeed();
        });
    }

    /**
     * The pale undead and the undead animals (the zombie chicken, the zombie wolf) are undead (smite, healing harms them);
     * the undead animals burn in daylight.
     */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void thePaleAndTheZombieChickenAreUndead(GameTestHelper helper) {
        floor(helper);
        List<EntityType<? extends Mob>> types = List.of(ModEntities.PALE_ZOMBIE, ModEntities.PALE_CREEPER, ModEntities.PALE_SPIDER, ModEntities.PALE_WOLF,
                ModEntities.ZOMBIE_CHICKEN, ModEntities.ZOMBIE_WOLF);
        for (int i = 0; i < types.size(); i++) {
            Mob mob = helper.spawnWithNoFreeWill(types.get(i), new Vec3(1.5 + 2 * i, 1.0, 3.5));
            helper.assertTrue(mob.is(EntityTypeTags.UNDEAD), mob.getType() + " isn't undead");
            if (mob instanceof ZombieChickenEntity || mob instanceof ZombieWolfEntity) helper.assertTrue(mob.is(EntityTypeTags.BURN_IN_DAYLIGHT), mob.getType() + " doesn't burn in daylight");
        }
        helper.succeed();
    }

    /** What each of the offers gives (its result), in order: the same trades are the same list. */
    private static List<String> sells(MerchantOffers offers) {
        return offers.stream().map(offer -> offer.getResult().getItem() + "x" + offer.getResult().getCount()).toList();
    }

    /** Cures a pale villager or trader at once: a golden apple fed, then its cure's time run out. */
    private static void cure(GameTestHelper helper, PaleVillagerEntity pale) {
        ServerPlayer player = FakePlayer.get(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GOLDEN_APPLE));
        pale.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(pale.isConverting(), "the golden apple didn't start the cure");
        pale.setVillagerConversionTime(1);
    }

    /**
     * A villager blighted pale and cured keeps its memories: its type, trade and level (so its clothes and badge), its
     * experience, its trades and what it thought of someone.
     */
    @GameTest(structure = ROOM, maxTicks = 100)
    public void aBlightedPaleAndCuredVillagerKeepsItsMemories(GameTestHelper helper) {
        floor(helper);
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(3.5, 1.0, 3.5));
        villager.setVillagerData(villager.getVillagerData().withProfession(helper.getLevel().registryAccess(), VillagerProfession.CLERIC).withLevel(3));
        villager.setVillagerXp(70);
        int trades = villager.getOffers().size();
        UUID friend = UUID.randomUUID();
        villager.getGossips().add(friend, GossipType.MAJOR_POSITIVE, 20);
        int liking = villager.getGossips().getReputation(friend, type -> true);
        killFullyBlighted(helper, villager);
        List<PaleVillagerEntity> pale = found(helper, PaleVillagerEntity.class, ModEntities.PALE_VILLAGER);
        helper.assertTrue(pale.size() == 1, pale.size() + " pale villagers, not one");
        cure(helper, pale.getFirst());
        helper.succeedWhen(() -> {
            List<Villager> villagers = found(helper, Villager.class, EntityTypes.VILLAGER);
            helper.assertTrue(villagers.size() == 1, villagers.size() + " villagers, not one");
            Villager cured = villagers.getFirst();
            helper.assertTrue(cured.getVillagerData().profession().is(VillagerProfession.CLERIC), "the cured villager isn't a cleric");
            helper.assertTrue(cured.getVillagerData().level() == 3, "the cured villager is level " + cured.getVillagerData().level() + ", not 3");
            helper.assertTrue(cured.getVillagerXp() == 70, "the cured villager has " + cured.getVillagerXp() + " experience, not 70");
            helper.assertTrue(cured.getOffers().size() == trades, "the cured villager has " + cured.getOffers().size() + " trades, not " + trades);
            helper.assertTrue(cured.getGossips().getReputation(friend, type -> true) == liking, "the cured villager forgot its friend");
        });
    }

    /** Killed fully blighted, a wandering trader turns into a pale wandering trader; cured, it's a wandering trader again, its trades kept. */
    @GameTest(structure = ROOM, maxTicks = 100)
    public void aWanderingTraderKilledFullyBlightedTurnsPaleAndCuresBack(GameTestHelper helper) {
        floor(helper);
        WanderingTrader trader = helper.spawnWithNoFreeWill(EntityTypes.WANDERING_TRADER, new Vec3(3.5, 1.0, 3.5));
        List<String> trades = sells(trader.getOffers());
        helper.assertTrue(!trades.isEmpty(), "the wandering trader has nothing to sell");
        killFullyBlighted(helper, trader);
        helper.assertTrue(trader.isRemoved(), "the fully blighted wandering trader killed is still a wandering trader");
        List<PaleWanderingTraderEntity> pale = found(helper, PaleWanderingTraderEntity.class, ModEntities.PALE_WANDERING_TRADER);
        helper.assertTrue(pale.size() == 1, pale.size() + " pale wandering traders, not one");
        cure(helper, pale.getFirst());
        helper.succeedWhen(() -> {
            List<WanderingTrader> traders = found(helper, WanderingTrader.class, EntityTypes.WANDERING_TRADER);
            helper.assertTrue(traders.size() == 1, traders.size() + " wandering traders, not one");
            helper.assertTrue(sells(traders.getFirst().getOffers()).equals(trades), "the cured trader sells " + sells(traders.getFirst().getOffers()) + ", not " + trades);
            helper.assertTrue(found(helper, Villager.class, EntityTypes.VILLAGER).isEmpty(), "the trader was cured into a villager");
        });
    }
}
