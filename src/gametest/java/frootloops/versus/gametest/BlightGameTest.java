package frootloops.versus.gametest;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.items_and_effects.blight.Blight;
import frootloops.versus.mod.items_and_effects.blight.Undeath;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.mobs.hostile.overworld.PlayerZombieEntity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Blight, the Wither's replacement: its doses stack however they're given; a Wither given is Blight instead; a player
 * who dies blighted rises as a player zombie wearing and carrying what they had, which drop when it dies.
 */
public class BlightGameTest {
    private static final String ROOM = "players-versus-gametest:leaves_room"; // 14 x 8 x 8

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 14; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
    }

    /** A lingering cloud dosing whoever stands in it every second adds to what they had: white hearts pile up. */
    @GameTest(structure = ROOM, maxTicks = 300)
    public void aCloudOfItStacksItsDoses(GameTestHelper helper) {
        floor(helper);
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        Vec3 at = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
        AreaEffectCloud cloud = new AreaEffectCloud(helper.getLevel(), at.x, at.y, at.z);
        cloud.setRadius(3.0F);
        cloud.setWaitTime(0);
        cloud.setDuration(400);
        cloud.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, 0));
        helper.getLevel().addFreshEntity(cloud);
        helper.succeedWhen(() -> {
            MobEffectInstance had = pig.getEffect(CustomStatusEffects.BLIGHT);
            helper.assertTrue(had != null && had.getAmplifier() >= 2, "the pig in the cloud has amplifier " + (had == null ? "none" : had.getAmplifier()) + ", not stacking");
        });
    }

    private static AABB everywhere(GameTestHelper helper) {
        return AABB.encapsulatingFullBlocks(helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(13, 7, 7)));
    }

    private static List<PlayerZombieEntity> playerZombies(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(PlayerZombieEntity.class, everywhere(helper));
    }

    /**
     * A player in the room, wearing a helmet, holding a sword, carrying diamonds. (The game test helper's mock player is
     * always creative, so damage can't kill it: the tests call {@code die} themselves, which is what damage does.)
     */
    private static ServerPlayer equippedPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 at = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
        player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        player.getInventory().setItem(20, new ItemStack(Items.DIAMOND, 5));
        return player;
    }

    /**
     * A player who dies of the Wither while blighted rises where they fell as a player zombie in their skin and name, wearing their helmet,
     * holding their sword and carrying the rest; killed, it drops all of it. Their own inventory is left empty, so
     * nothing is dropped twice.
     */
    @GameTest(structure = ROOM, maxTicks = 40)
    public void aPlayerWhoDiesBlightedRisesAsAPlayerZombieWithTheirThings(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = equippedPlayer(helper);
        player.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, 0));
        player.die(helper.getLevel().damageSources().wither());
        List<PlayerZombieEntity> zombies = playerZombies(helper);
        helper.assertTrue(zombies.size() == 1, zombies.size() + " player zombies, not one");
        PlayerZombieEntity zombie = zombies.getFirst();
        helper.assertTrue(zombie.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "the player zombie doesn't wear the player's helmet");
        helper.assertTrue(zombie.getItemBySlot(EquipmentSlot.MAINHAND).is(Items.IRON_SWORD), "the player zombie doesn't hold the player's sword");
        helper.assertTrue(zombie.getStash().stream().anyMatch(stack -> stack.is(Items.DIAMOND) && stack.getCount() == 5), "the player zombie doesn't carry the diamonds");
        helper.assertTrue(zombie.getProfile().equals(player.getProfile()), "the player zombie isn't in the player's skin");
        helper.assertTrue(player.getInventory().isEmpty(), "the player still has things on them");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, everywhere(helper)).isEmpty(), "the player dropped things as well");

        zombie.hurtServer(helper.getLevel(), helper.getLevel().damageSources().genericKill(), 1000.0F);
        List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, everywhere(helper));
        helper.assertTrue(dropped.stream().anyMatch(item -> item.getItem().is(Items.IRON_HELMET)), "the helmet wasn't dropped");
        helper.assertTrue(dropped.stream().anyMatch(item -> item.getItem().is(Items.IRON_SWORD)), "the sword wasn't dropped");
        helper.assertTrue(dropped.stream().anyMatch(item -> item.getItem().is(Items.DIAMOND) && item.getItem().getCount() == 5), "the diamonds weren't dropped");
        helper.succeed();
    }

    /** Blighted but not killed by the Wither, or killed by the Wither but not blighted: they die as they always did. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void onlyTheWitherUnderTheBlightConvertsAPlayer(GameTestHelper helper) {
        floor(helper);
        ServerPlayer blighted = equippedPlayer(helper);
        blighted.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, 0));
        blighted.die(helper.getLevel().damageSources().genericKill());
        helper.assertTrue(playerZombies(helper).isEmpty(), "a player zombie rose of a blighted player who didn't die of the Wither");
        ServerPlayer plain = equippedPlayer(helper);
        plain.die(helper.getLevel().damageSources().wither());
        helper.assertTrue(playerZombies(helper).isEmpty(), "a player zombie rose of a player who wasn't blighted");
        helper.succeed();
    }

    /** The Wither is still the Wither: a Wither given stays one. Blight is Blight. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aWitherStaysTheWither(GameTestHelper helper) {
        floor(helper);
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        pig.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
        helper.assertTrue(pig.hasEffect(MobEffects.WITHER), "the Wither didn't stay the Wither");
        helper.assertTrue(!pig.hasEffect(CustomStatusEffects.BLIGHT), "the Wither became Blight");
        helper.succeed();
    }

    /** All their hearts white, the Blight deals the Wither: a strong one, kept up. Not all white, none. */
    @GameTest(structure = ROOM, maxTicks = 100)
    public void allWhiteBlightDealsTheWither(GameTestHelper helper) {
        floor(helper);
        Mob few = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        few.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, 1));
        Mob all = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(9.5, 1.0, 3.5));
        all.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, Blight.hearts(all.getMaxHealth()) - 1));
        helper.succeedWhen(() -> {
            MobEffectInstance wither = all.getEffect(MobEffects.WITHER);
            helper.assertTrue(wither != null && wither.getAmplifier() == VersusSettings.Blight.WITHER_AMPLIFIER, "the all-white pig has " + (wither == null ? "no Wither" : "Wither amplifier " + wither.getAmplifier()));
            helper.assertTrue(!few.hasEffect(MobEffects.WITHER), "a pig not all white has the Wither");
        });
    }

    /**
     * Killed by the Wither under the Blight, the living are converted if they can be: a villager into a zombie villager.
     * Without the Blight it dies, and there's no zombie.
     */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void dyingOfTheWitherUnderTheBlightConverts(GameTestHelper helper) {
        floor(helper);
        Villager blighted = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(3.5, 1.0, 3.5));
        blighted.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, 0));
        blighted.setHealth(1.0F);
        blighted.hurtServer(helper.getLevel(), helper.getLevel().damageSources().wither(), 2.0F);
        helper.assertTrue(blighted.isRemoved(), "the blighted villager is still a villager");
        List<ZombieVillager> zombies = helper.getLevel().getEntitiesOfClass(ZombieVillager.class, everywhere(helper));
        helper.assertTrue(zombies.size() == 1, zombies.size() + " zombie villagers, not one");

        Villager plain = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(9.5, 1.0, 3.5));
        plain.setHealth(1.0F);
        plain.hurtServer(helper.getLevel(), helper.getLevel().damageSources().wither(), 2.0F);
        helper.assertTrue(plain.isDeadOrDying(), "the plain villager lives");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ZombieVillager.class, everywhere(helper)).size() == 1, "a plain villager's wither death left a zombie villager");

        // And something with no undead form just dies of it, blighted or not
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(6.5, 1.0, 5.5));
        pig.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, 0));
        pig.setHealth(1.0F);
        pig.hurtServer(helper.getLevel(), helper.getLevel().damageSources().wither(), 2.0F);
        helper.assertTrue(pig.isDeadOrDying(), "the blighted pig lives");
        helper.succeed();
    }

    /** A blow of the Wither that wounds, not kills, doesn't turn a blighted thing pale: the Wither is for killing. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void theWithersWoundsDontTurnPale(GameTestHelper helper) {
        floor(helper);
        Villager villager = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(3.5, 1.0, 3.5));
        villager.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, 0));
        villager.setHealth(9.0F);
        villager.hurtServer(helper.getLevel(), helper.getLevel().damageSources().wither(), 2.0F);
        helper.assertTrue(!villager.isRemoved(), "the wounded villager was turned");
        helper.succeed();
    }

    /** A creative player can't die: they live, the Blight lifts, and a copy of them rises, nothing taken from them. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aCreativePlayerTurningLivesAndLeavesACopy(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = equippedPlayer(helper);
        player.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, Blight.hearts(player.getMaxHealth()) - 1));
        Undeath.turn(helper.getLevel(), player);
        helper.assertTrue(!player.isDeadOrDying(), "the creative player died");
        helper.assertTrue(!player.hasEffect(CustomStatusEffects.BLIGHT), "the creative player is still blighted");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "the creative player lost their helmet");
        helper.assertTrue(player.getInventory().getItem(20).is(Items.DIAMOND), "the creative player lost their diamonds");
        List<PlayerZombieEntity> zombies = playerZombies(helper);
        helper.assertTrue(zombies.size() == 1, zombies.size() + " player zombies, not one");
        helper.assertTrue(zombies.getFirst().getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "the copy doesn't wear a helmet");
        helper.assertTrue(zombies.getFirst().getStash().isEmpty(), "the copy carries things");
        helper.succeed();
    }

    /** A potion of Healing purges the white hearts it heals; a stronger one purges the lot. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void healingPurgesBlight(GameTestHelper helper) {
        floor(helper);
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        pig.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, 3));
        MobEffects.INSTANT_HEALTH.value().applyInstantaneousEffect(helper.getLevel(), null, null, pig, 0, 1.0);
        MobEffectInstance left = pig.getEffect(CustomStatusEffects.BLIGHT);
        helper.assertTrue(left != null && left.getAmplifier() == 1, "Healing I left " + (left == null ? "none" : "amplifier " + left.getAmplifier()) + ", not two white hearts");
        helper.assertTrue(left.getDuration() > 2000, "the Blight that's left lost its time");
        MobEffects.INSTANT_HEALTH.value().applyInstantaneousEffect(helper.getLevel(), null, null, pig, 1, 1.0);
        helper.assertTrue(!pig.hasEffect(CustomStatusEffects.BLIGHT), "Healing II left Blight");
        helper.succeed();
    }

    /**
     * Absorption gained purges a white heart for each heart of it; absorption lost purges nothing. (Absorption is held to
     * the entity's maximum absorption, which only the Absorption effect raises: the tests go through the effect.)
     */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void absorptionPurgesBlight(GameTestHelper helper) {
        floor(helper);
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        pig.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, 4));
        pig.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 0));
        helper.assertTrue(pig.getAbsorptionAmount() >= 4.0F, "the Absorption effect gave " + pig.getAbsorptionAmount() + " absorption");
        MobEffectInstance left = pig.getEffect(CustomStatusEffects.BLIGHT);
        helper.assertTrue(left != null && left.getAmplifier() == 2, "Absorption I left " + (left == null ? "none" : "amplifier " + left.getAmplifier()) + ", not three white hearts");
        // Losing it takes no more Blight with it
        pig.setAbsorptionAmount(0.0F);
        helper.assertTrue(pig.getEffect(CustomStatusEffects.BLIGHT).getAmplifier() == 2, "losing absorption purged Blight");
        // A stronger one purges more than is there
        Mob other = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(6.5, 1.0, 3.5));
        other.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, 3));
        other.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 1));
        helper.assertTrue(!other.hasEffect(CustomStatusEffects.BLIGHT), "Absorption II left Blight");
        helper.succeed();
    }

}
