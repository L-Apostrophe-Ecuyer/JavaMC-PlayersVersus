package frootloops.versus.gametest;

import frootloops.versus.VersusSettings;
import frootloops.versus.mod.items_and_effects.blight.Blight;
import frootloops.versus.mod.items_and_effects.blight.Undeath;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.mobs.hostile.overworld.PaleVillagerEntity;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Blight, the Wither's replacement: its doses stack however they're given; a Wither given is Blight instead; fully
 * blighted it deals a heart every ten ticks; whatever dies fully blighted is replaced by its pale form, and a player rises
 * as a player zombie wearing and carrying what they had, which drop when it dies.
 */
public class BlightGameTest {
    private static final String ROOM = "players-versus-gametest:leaves_room"; // 14 x 8 x 8

    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 14; x++) for (int z = 0; z < 8; z++) helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
    }

    /** A lingering cloud dosing whoever stands in it every second adds to what they had: blighted hearts pile up. */
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
     * A player who dies fully blighted rises where they fell as a player zombie in their skin and name, wearing their helmet,
     * holding their sword and carrying the rest; killed, it drops all of it. Their own inventory is left empty, so
     * nothing is dropped twice.
     */
    @GameTest(structure = ROOM, maxTicks = 40)
    public void aPlayerWhoDiesFullyBlightedRisesAsAPlayerZombieWithTheirThings(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = equippedPlayer(helper);
        player.addEffect(fullBlight(player));
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

    /** Blight enough to blight all the entity's hearts. */
    private static MobEffectInstance fullBlight(LivingEntity entity) {
        return new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, Blight.hearts(entity.getMaxHealth()) - 1);
    }

    /**
     * Only a player who dies fully blighted rises, whatever they die of: with some hearts not yet blighted, or not
     * blighted at all, they die as they always did.
     */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void onlyAPlayerWhoDiesFullyBlightedRises(GameTestHelper helper) {
        floor(helper);
        ServerPlayer partly = equippedPlayer(helper);
        partly.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, 0));
        partly.die(helper.getLevel().damageSources().wither());
        helper.assertTrue(playerZombies(helper).isEmpty(), "a player zombie rose of a player with one heart blighted");
        ServerPlayer plain = equippedPlayer(helper);
        plain.die(helper.getLevel().damageSources().wither());
        helper.assertTrue(playerZombies(helper).isEmpty(), "a player zombie rose of a player who wasn't blighted");
        ServerPlayer full = equippedPlayer(helper);
        full.addEffect(fullBlight(full));
        full.die(helper.getLevel().damageSources().genericKill());
        helper.assertTrue(playerZombies(helper).size() == 1, "no player zombie rose of a fully blighted player killed by something else");
        helper.succeed();
    }

    /** Blight replaces the Wither: a Wither given, or forced on, is Blight of its strength (up to a limit), and no Wither. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aWitherGivenIsBlight(GameTestHelper helper) {
        floor(helper);
        Mob given = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        given.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
        MobEffectInstance blight = given.getEffect(CustomStatusEffects.BLIGHT);
        helper.assertTrue(blight != null && blight.getAmplifier() == 1, "Wither II became " + (blight == null ? "no Blight" : "Blight amplifier " + blight.getAmplifier()));
        helper.assertTrue(!given.hasEffect(MobEffects.WITHER), "the Wither stayed the Wither");
        Mob forced = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(6.5, 1.0, 3.5));
        forced.forceAddEffect(new MobEffectInstance(MobEffects.WITHER, 200, 250), null);
        MobEffectInstance limited = forced.getEffect(CustomStatusEffects.BLIGHT);
        helper.assertTrue(limited != null && limited.getAmplifier() == VersusSettings.Blight.WITHER_MAX_HEARTS - 1,
                "a forced Wither of 251 levels became " + (limited == null ? "no Blight" : "Blight amplifier " + limited.getAmplifier()));
        helper.assertTrue(!forced.hasEffect(MobEffects.WITHER), "the forced Wither stayed the Wither");
        helper.succeed();
    }

    /** Fully blighted, it takes a heart every ten ticks, as the Wither would deal it; not fully blighted, nothing. */
    @GameTest(structure = ROOM, maxTicks = 60)
    public void fullyBlightedItTakesAHeartEveryTenTicks(GameTestHelper helper) {
        floor(helper);
        Mob partly = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        partly.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, 1));
        Mob full = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(9.5, 1.0, 3.5));
        full.addEffect(fullBlight(full));
        helper.runAfterDelay(25, () -> {
            // Three blows by now (as it became full, 10 and 20 ticks after): two at the least, whatever the timing
            helper.assertTrue(full.getHealth() <= full.getMaxHealth() - 2.0F * VersusSettings.Blight.FULL_DAMAGE,
                    "the fully blighted pig has " + full.getHealth() + " of " + full.getMaxHealth() + " health");
            helper.assertTrue(full.getHealth() >= full.getMaxHealth() - 4.0F * VersusSettings.Blight.FULL_DAMAGE,
                    "the fully blighted pig lost more than a heart every ten ticks: " + full.getHealth());
            helper.assertTrue(partly.getHealth() == partly.getMaxHealth(), "a pig not fully blighted was hurt");
            helper.assertTrue(!full.hasEffect(MobEffects.WITHER) && !partly.hasEffect(MobEffects.WITHER), "a blighted pig has the Wither");
            helper.succeed();
        });
    }

    /**
     * Whatever dies fully blighted, of anything, is replaced by its pale form: a villager by a pale villager. With a heart
     * not yet blighted it just dies; and something with no pale form (a pig) dies fully blighted all the same.
     */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void dyingFullyBlightedTurnsPale(GameTestHelper helper) {
        floor(helper);
        Villager full = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(3.5, 1.0, 3.5));
        full.addEffect(fullBlight(full));
        full.setHealth(1.0F);
        full.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 2.0F);
        helper.assertTrue(full.isRemoved(), "the fully blighted villager is still a villager");
        List<PaleVillagerEntity> pale = helper.getLevel().getEntitiesOfClass(PaleVillagerEntity.class, everywhere(helper));
        helper.assertTrue(pale.size() == 1, pale.size() + " pale villagers, not one");
        helper.assertTrue(!pale.getFirst().hasEffect(CustomStatusEffects.BLIGHT), "the pale villager carries Blight");

        Villager partly = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(9.5, 1.0, 3.5));
        partly.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, 0));
        partly.setHealth(1.0F);
        partly.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 2.0F);
        helper.assertTrue(partly.isDeadOrDying(), "the partly blighted villager lives");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(PaleVillagerEntity.class, everywhere(helper)).size() == 1, "a partly blighted villager's death left a pale villager");

        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(6.5, 1.0, 5.5));
        pig.addEffect(fullBlight(pig));
        pig.setHealth(1.0F);
        pig.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 2.0F);
        helper.assertTrue(pig.isDeadOrDying(), "the fully blighted pig lives");
        helper.succeed();
    }

    /** A wound doesn't turn anything pale, however low it leaves it: only a death does, and only fully blighted. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aWoundDoesntTurnPale(GameTestHelper helper) {
        floor(helper);
        Villager partly = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(3.5, 1.0, 3.5));
        partly.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, 0));
        partly.setHealth(5.0F);
        partly.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 2.0F);
        Villager full = helper.spawnWithNoFreeWill(EntityTypes.VILLAGER, new Vec3(9.5, 1.0, 3.5));
        full.addEffect(fullBlight(full));
        full.setHealth(5.0F);
        full.hurtServer(helper.getLevel(), helper.getLevel().damageSources().generic(), 2.0F);
        helper.assertTrue(!partly.isRemoved() && !full.isRemoved(), "a wounded villager turned pale");
        helper.succeed();
    }

    /** A creative player can't die: they live, the Blight lifts, and a copy of them rises, nothing taken from them. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aCreativePlayerTurningLivesAndLeavesACopy(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = equippedPlayer(helper);
        player.addEffect(fullBlight(player));
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

    /** A potion of Healing purges the blighted hearts it heals; a stronger one purges the lot. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void healingPurgesBlight(GameTestHelper helper) {
        floor(helper);
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        pig.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, 3));
        MobEffects.INSTANT_HEALTH.value().applyInstantaneousEffect(helper.getLevel(), null, null, pig, 0, 1.0);
        MobEffectInstance left = pig.getEffect(CustomStatusEffects.BLIGHT);
        helper.assertTrue(left != null && left.getAmplifier() == 1, "Healing I left " + (left == null ? "none" : "amplifier " + left.getAmplifier()) + ", not two blighted hearts");
        helper.assertTrue(left.getDuration() > 2000, "the Blight that's left lost its time");
        MobEffects.INSTANT_HEALTH.value().applyInstantaneousEffect(helper.getLevel(), null, null, pig, 1, 1.0);
        helper.assertTrue(!pig.hasEffect(CustomStatusEffects.BLIGHT), "Healing II left Blight");
        helper.succeed();
    }

    /**
     * Absorption gained purges a blighted heart for each heart of it; absorption lost purges nothing. (Absorption is held to
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
        helper.assertTrue(left != null && left.getAmplifier() == 2, "Absorption I left " + (left == null ? "none" : "amplifier " + left.getAmplifier()) + ", not three blighted hearts");
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
