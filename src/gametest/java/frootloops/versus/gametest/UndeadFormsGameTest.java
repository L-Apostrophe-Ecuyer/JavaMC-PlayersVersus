package frootloops.versus.gametest;

import frootloops.versus.mod.environment.PaleLine;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.items_and_effects.conversion.Conversion;
import frootloops.versus.mod.mobs.ModEntities;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.illager.Pillager;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * What Conversion makes of villagers and animals: villagers turn as a zombie's kill does, into zombie villagers or, where
 * paleness holds, the illager of their trade; animals with an undead kind of their own turn into it and stay their
 * owner's. Zombie chickens lay nothing and breed with nothing; the pale and the passive undead are undead.
 */
public class UndeadFormsGameTest {

    private static final String ROOM = "players-versus-gametest:leaves_room"; // 14 x 8 x 8

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 14; x++) for (int z = 0; z < 8; z++) helper.setBlock(x, 0, z, Blocks.STONE.defaultBlockState());
    }

    private static AABB room(GameTestHelper helper) {
        return AABB.encapsulatingFullBlocks(helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(13, 7, 7)));
    }

    /** Conversion enough to whiten all the entity's hearts. */
    private static MobEffectInstance allWhite(LivingEntity entity) {
        return new MobEffectInstance(CustomStatusEffects.CONVERSION, 2400, Conversion.hearts(entity.getMaxHealth()) - 1);
    }

    private static <T extends Mob> List<T> found(GameTestHelper helper, Class<T> type, EntityType<?> exactly) {
        return helper.getLevel().getEntitiesOfClass(type, room(helper), mob -> mob.getType() == exactly);
    }

    /**
     * All white, a jobless villager turns as a zombie's kill would: where paleness holds (the test level may be above or
     * below it) into a pillager with a crossbow, elsewhere into a zombie villager.
     */
    @GameTest(structure = ROOM, maxTicks = 200)
    public void anAllWhiteVillagerTurnsIllagerWherePalenessHolds(GameTestHelper helper) {
        floor(helper);
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(3.5, 1.0, 3.5));
        boolean pale = PaleLine.isPale(helper.getLevel(), villager.blockPosition());
        villager.addEffect(allWhite(villager));
        helper.succeedWhen(() -> {
            helper.assertTrue(villager.isRemoved(), "the villager is still a villager");
            List<Pillager> pillagers = found(helper, Pillager.class, EntityTypes.PILLAGER);
            List<ZombieVillager> zombies = found(helper, ZombieVillager.class, EntityTypes.ZOMBIE_VILLAGER);
            if (pale) {
                helper.assertTrue(pillagers.size() == 1 && zombies.isEmpty(), "where paleness holds: " + pillagers.size() + " pillagers, " + zombies.size() + " zombie villagers");
                helper.assertTrue(pillagers.getFirst().getItemBySlot(EquipmentSlot.MAINHAND).is(Items.CROSSBOW), "the pillager has no crossbow");
                helper.assertTrue(pillagers.getFirst().isPersistenceRequired(), "the pillager would despawn");
            } else {
                helper.assertTrue(zombies.size() == 1 && pillagers.isEmpty(), "above the pale: " + zombies.size() + " zombie villagers, " + pillagers.size() + " pillagers");
            }
        });
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
            helper.assertTrue(!undead.getFirst().hasEffect(CustomStatusEffects.CONVERSION), "the zombie chicken carries Conversion");
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
}
