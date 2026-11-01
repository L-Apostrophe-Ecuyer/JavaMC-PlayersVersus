package frootloops.versus.gametest;

import frootloops.versus.mod.environment.CustomBlocks;
import frootloops.versus.mod.environment.blocks.mushrooms.StagedMushroomBlock;
import frootloops.versus.mod.items_and_effects.CustomBrewingItems;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.items_and_effects.blight.Blight;
import frootloops.versus.mod.mobs.ModEntities;
import frootloops.versus.mod.mobs.hostile.overworld.PaleVillagerEntity;
import frootloops.versus.mod.mobs.hostile.overworld.PaleWanderingTraderEntity;
import frootloops.versus.mod.mobs.passive.ZombieChickenEntity;
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
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * What Blight and withering make of villagers and animals: Blight turns villagers into zombie villagers and
 * animals with an undead kind of their own into it, still their owner's; withering turns villagers into pale villagers,
 * cured by golden apples, and illagers into pale zombies. Zombie chickens lay nothing and breed with nothing; the pale
 * and the passive undead are undead. Golden apples cure Blight; grown brown mushroom clusters make mining fatigue.
 */
public class UndeadFormsGameTest {

    private static final String ROOM = "players-versus-gametest:leaves_room"; // 14 x 8 x 8

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 14; x++) for (int z = 0; z < 8; z++) helper.setBlock(x, 0, z, Blocks.STONE.defaultBlockState());
    }

    private static AABB room(GameTestHelper helper) {
        return AABB.encapsulatingFullBlocks(helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(13, 7, 7)));
    }

    /** Blight enough to whiten all the entity's hearts. */
    private static MobEffectInstance allWhite(LivingEntity entity) {
        return new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, Blight.hearts(entity.getMaxHealth()) - 1);
    }

    private static <T extends Mob> List<T> found(GameTestHelper helper, Class<T> type, EntityType<?> exactly) {
        return helper.getLevel().getEntitiesOfClass(type, room(helper), mob -> mob.getType() == exactly);
    }

    /** All white, a villager turns as a zombie's kill would: into a zombie villager, anywhere (pale places too). */
    @GameTest(structure = ROOM, maxTicks = 200)
    public void anAllWhiteVillagerTurnsZombieVillager(GameTestHelper helper) {
        floor(helper);
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(3.5, 1.0, 3.5));
        villager.addEffect(allWhite(villager));
        helper.succeedWhen(() -> {
            helper.assertTrue(villager.isRemoved(), "the villager is still a villager");
            List<ZombieVillager> zombies = found(helper, ZombieVillager.class, EntityTypes.ZOMBIE_VILLAGER);
            helper.assertTrue(zombies.size() == 1, zombies.size() + " zombie villagers, not one");
        });
    }

    /** Withered below 8 health, a farmer turns into a pale villager, still a farmer, its experience kept for its cure. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aWitheredVillagerTurnsPale(GameTestHelper helper) {
        floor(helper);
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(3.5, 1.0, 3.5));
        villager.setVillagerData(villager.getVillagerData().withProfession(helper.getLevel().registryAccess(), VillagerProfession.FARMER));
        villager.setVillagerXp(5);
        villager.setHealth(9.0F);
        villager.hurtServer(helper.getLevel(), helper.getLevel().damageSources().wither(), 2.0F);
        helper.assertTrue(villager.isRemoved(), "the withered villager is still a villager");
        List<PaleVillagerEntity> pale = found(helper, PaleVillagerEntity.class, ModEntities.PALE_VILLAGER);
        helper.assertTrue(pale.size() == 1, pale.size() + " pale villagers, not one");
        helper.assertTrue(pale.getFirst().getVillagerData().profession().is(VillagerProfession.FARMER), "the pale villager isn't a farmer any more");
        helper.assertTrue(pale.getFirst().getVillagerXp() == 5, "the pale villager lost its experience");
        helper.succeed();
    }

    /** Withered below 8 health, a pillager turns into a pale zombie; Blight would have made it the zombie of the place. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aWitheredIllagerTurnsPaleZombie(GameTestHelper helper) {
        floor(helper);
        Mob pillager = helper.spawnWithNoFreeWill(EntityTypes.PILLAGER, new Vec3(3.5, 1.0, 3.5));
        pillager.setHealth(9.0F);
        pillager.hurtServer(helper.getLevel(), helper.getLevel().damageSources().wither(), 2.0F);
        helper.assertTrue(pillager.isRemoved(), "the withered pillager is still a pillager");
        helper.assertTrue(found(helper, Mob.class, ModEntities.PALE_ZOMBIE).size() == 1, "no pale zombie rose of the pillager");
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

    /** A child villager turns into a zombie villager child anywhere: there are no illager children. */
    @GameTest(structure = ROOM, maxTicks = 200)
    public void anAllWhiteChildVillagerTurnsZombieVillagerAnywhere(GameTestHelper helper) {
        floor(helper);
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(3.5, 1.0, 3.5));
        villager.setAge(-24000);
        villager.addEffect(allWhite(villager));
        helper.succeedWhen(() -> {
            helper.assertTrue(villager.isRemoved(), "the child is still a villager");
            List<ZombieVillager> zombies = found(helper, ZombieVillager.class, EntityTypes.ZOMBIE_VILLAGER);
            helper.assertTrue(zombies.size() == 1 && zombies.getFirst().isBaby(), zombies.size() + " zombie villagers, not one child");
        });
    }

    /** All white, a chicken turns into a zombie chicken, grown. */
    @GameTest(structure = ROOM, maxTicks = 200)
    public void anAllWhiteChickenTurnsZombieChicken(GameTestHelper helper) {
        floor(helper);
        Chicken chick = helper.spawnWithNoFreeWill(EntityTypes.CHICKEN, new Vec3(3.5, 1.0, 3.5));
        chick.setAge(-24000);
        chick.addEffect(allWhite(chick));
        helper.succeedWhen(() -> {
            helper.assertTrue(chick.isRemoved(), "the chicken is still a chicken");
            List<ZombieChickenEntity> undead = found(helper, ZombieChickenEntity.class, ModEntities.ZOMBIE_CHICKEN);
            helper.assertTrue(undead.size() == 1, undead.size() + " zombie chickens, not one");
            helper.assertTrue(!undead.getFirst().isBaby(), "the zombie chicken is a chick");
            helper.assertTrue(!undead.getFirst().hasEffect(CustomStatusEffects.BLIGHT), "the zombie chicken carries Blight");
        });
    }

    /** All white, a tamed horse turns into a zombie horse still tame and still its owner's. */
    @GameTest(structure = ROOM, maxTicks = 200)
    public void anAllWhiteTamedHorseStaysItsOwners(GameTestHelper helper) {
        floor(helper);
        ServerPlayer owner = FakePlayer.get(helper.getLevel());
        Horse horse = helper.spawnWithNoFreeWill(EntityTypes.HORSE, new Vec3(3.5, 1.0, 3.5));
        horse.setTamed(true);
        horse.setOwner(owner);
        horse.addEffect(allWhite(horse));
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

    /** The pale undead and the zombie chicken are undead (smite, healing harms them); the zombie chicken burns in daylight. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void thePaleAndTheZombieChickenAreUndead(GameTestHelper helper) {
        floor(helper);
        List<EntityType<? extends Mob>> types = List.of(ModEntities.PALE_ZOMBIE, ModEntities.PALE_CREEPER, ModEntities.PALE_SPIDER, ModEntities.ZOMBIE_CHICKEN);
        for (int i = 0; i < types.size(); i++) {
            Mob mob = helper.spawnWithNoFreeWill(types.get(i), new Vec3(2.5 + 3 * i, 1.0, 3.5));
            helper.assertTrue(mob.is(EntityTypeTags.UNDEAD), mob.getType() + " isn't undead");
            if (mob instanceof ZombieChickenEntity) helper.assertTrue(mob.is(EntityTypeTags.BURN_IN_DAYLIGHT), "the zombie chicken doesn't burn in daylight");
        }
        helper.succeed();
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
     * A villager withered pale and cured keeps its memories: its type, trade and level (so its clothes and badge), its
     * experience, its trades and what it thought of someone.
     */
    @GameTest(structure = ROOM, maxTicks = 100)
    public void aWitheredAndCuredVillagerKeepsItsMemories(GameTestHelper helper) {
        floor(helper);
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(3.5, 1.0, 3.5));
        villager.setVillagerData(villager.getVillagerData().withProfession(helper.getLevel().registryAccess(), VillagerProfession.CLERIC).withLevel(3));
        villager.setVillagerXp(70);
        int trades = villager.getOffers().size();
        UUID friend = UUID.randomUUID();
        villager.getGossips().add(friend, GossipType.MAJOR_POSITIVE, 20);
        int liking = villager.getGossips().getReputation(friend, type -> true);
        villager.setHealth(9.0F);
        villager.hurtServer(helper.getLevel(), helper.getLevel().damageSources().wither(), 2.0F);
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

    /** Withered, a wandering trader turns into a pale wandering trader; cured, it's a wandering trader again, its trades kept. */
    @GameTest(structure = ROOM, maxTicks = 100)
    public void aWitheredWanderingTraderTurnsPaleAndCuresBack(GameTestHelper helper) {
        floor(helper);
        WanderingTrader trader = helper.spawnWithNoFreeWill(EntityTypes.WANDERING_TRADER, new Vec3(3.5, 1.0, 3.5));
        int trades = trader.getOffers().size();
        helper.assertTrue(trades > 0, "the wandering trader has nothing to sell");
        trader.setHealth(9.0F);
        trader.hurtServer(helper.getLevel(), helper.getLevel().damageSources().wither(), 2.0F);
        helper.assertTrue(trader.isRemoved(), "the withered wandering trader is still a wandering trader");
        List<PaleWanderingTraderEntity> pale = found(helper, PaleWanderingTraderEntity.class, ModEntities.PALE_WANDERING_TRADER);
        helper.assertTrue(pale.size() == 1, pale.size() + " pale wandering traders, not one");
        cure(helper, pale.getFirst());
        helper.succeedWhen(() -> {
            List<WanderingTrader> traders = found(helper, WanderingTrader.class, EntityTypes.WANDERING_TRADER);
            helper.assertTrue(traders.size() == 1, traders.size() + " wandering traders, not one");
            helper.assertTrue(traders.getFirst().getOffers().size() == trades, "the cured trader has " + traders.getFirst().getOffers().size() + " trades, not " + trades);
            helper.assertTrue(found(helper, Villager.class, EntityTypes.VILLAGER).isEmpty(), "the trader was cured into a villager");
        });
    }
}
