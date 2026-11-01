package frootloops.versus.gametest;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Blight, the Wither's replacement: its doses stack however they're given; a Wither given is Blight instead; a player
 * who dies blighted, or turns, rises as a player zombie wearing and carrying what they had, which drop when it dies.
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

    /** A survival player in the room, wearing a helmet, holding a sword, carrying diamonds. */
    private static ServerPlayer equippedSurvivor(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
        player.snapTo(at.x, at.y, at.z, 0.0F, 0.0F);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        player.getInventory().setItem(20, new ItemStack(Items.DIAMOND, 5));
        return player;
    }

    /** Every Wither given is Blight, at the same strength (up to the most hearts a Wither's dose whitens). */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aWitherGivenIsBlightInstead(GameTestHelper helper) {
        floor(helper);
        Mob pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new Vec3(3.5, 1.0, 3.5));
        pig.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
        helper.assertTrue(!pig.hasEffect(MobEffects.WITHER), "the pig has the Wither");
        MobEffectInstance blight = pig.getEffect(CustomStatusEffects.BLIGHT);
        helper.assertTrue(blight != null, "the Wither didn't become Blight");
        helper.assertTrue(blight.getAmplifier() == Blight.fromWitherAmplifier(1), "the Blight is amplifier " + blight.getAmplifier());
        helper.succeed();
    }

    /**
     * A player who dies blighted rises where they fell as a player zombie in their skin and name, wearing their helmet,
     * holding their sword and carrying the rest; killed, it drops all of it. Their own inventory is left empty, so
     * nothing is dropped twice.
     */
    @GameTest(structure = ROOM, maxTicks = 40)
    public void aPlayerWhoDiesBlightedRisesAsAPlayerZombieWithTheirThings(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = equippedSurvivor(helper);
        player.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, 0));
        player.hurtServer(helper.getLevel(), helper.getLevel().damageSources().genericKill(), 1000.0F);
        helper.assertTrue(player.isDeadOrDying(), "the player lives");
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

    /** A player who dies of anything but blighted dies as they always did: no player zombie. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aPlayerWhoDiesUnblightedLeavesNoZombie(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = equippedSurvivor(helper);
        player.hurtServer(helper.getLevel(), helper.getLevel().damageSources().genericKill(), 1000.0F);
        helper.assertTrue(player.isDeadOrDying(), "the player lives");
        helper.assertTrue(playerZombies(helper).isEmpty(), "a player zombie rose of an unblighted player");
        helper.succeed();
    }

    /** All their hearts white, a player turns: they die of it, and a player zombie rises. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void anAllWhitePlayerTurnsIntoAPlayerZombie(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = equippedSurvivor(helper);
        player.addEffect(new MobEffectInstance(CustomStatusEffects.BLIGHT, 2400, Blight.hearts(player.getMaxHealth()) - 1));
        Undeath.turn(helper.getLevel(), player);
        helper.assertTrue(player.isDeadOrDying(), "the all-white player lives");
        helper.assertTrue(playerZombies(helper).size() == 1, "no player zombie rose of the all-white player");
        helper.succeed();
    }

    /** A creative player can't die: they live, the Blight lifts, and a copy of them rises, nothing taken from them. */
    @GameTest(structure = ROOM, maxTicks = 20)
    public void aCreativePlayerTurningLivesAndLeavesACopy(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = equippedSurvivor(helper);
        player.setGameMode(GameType.CREATIVE);
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
}
