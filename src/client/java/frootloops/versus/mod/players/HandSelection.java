package frootloops.versus.mod.players;

import frootloops.versus.mod.Combat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Which hand a right-click tries first, from what the player most likely means. Each hand, in the order chosen here,
 * first interacts with what the crosshair is on (a block's use, an entity's, placing what it holds) and then uses its
 * item, as vanilla's does; only the order of the hands changes, by these rules, the first that applies:
 * <ol>
 * <li>An item is in use (a shield held up, a bow drawn): only its hand, and its item isn't started again.</li>
 * <li>Falling onto a block with a water bucket in a hand: that hand, to land in the water.</li>
 * <li>Under threat (an enemy within reach in front of you, an enemy or a player at the crosshair, or something flying at
 * you) with an item that blocks attacks, unless sneaking: only the items that block, the off hand's first, and no
 * interaction with the world. Sneak to place or use what you hold instead.</li>
 * <li>Otherwise the hands that aren't blocking come first, the main hand's first (a bow first if an enemy is at the
 * crosshair), then the items that block, the off hand's first: a blocking item is what you want when something is
 * attacking, and the last thing when nothing is, so an offhand torch or ender pearl beats a sword in the main hand.</li>
 * </ol>
 * In all of them an item on cooldown goes last, and its item is never used: the server would refuse, and the client
 * would show a use that never happened.
 */
@Environment(EnvType.CLIENT)
public final class HandSelection {
    private HandSelection() {}

    /** The hands in order; whether each interacts with the world first; whether each then uses its item. */
    public record Plan(InteractionHand[] hands, boolean world, boolean items) {}

    private static final double MELEE_RANGE = 4.5, TARGET_RANGE = 6.0, PROJECTILE_RANGE = 10.0, FALLING = 3.5;

    public static Plan plan(LocalPlayer player, ClientLevel level, @Nullable HitResult hit) {
        if (player.isUsingItem()) return new Plan(new InteractionHand[] {player.getUsedItemHand()}, true, false);

        InteractionHand bucket = fallingBucket(player, hit);
        if (bucket != null) return new Plan(new InteractionHand[] {bucket, other(bucket)}, true, true);

        List<InteractionHand> blockers = new ArrayList<>();
        for (InteractionHand hand : new InteractionHand[] {InteractionHand.OFF_HAND, InteractionHand.MAIN_HAND}) {
            if (blocks(player.getItemInHand(hand)) && !onCooldown(player, hand)) blockers.add(hand);
        }
        if (!blockers.isEmpty() && !player.isShiftKeyDown() && threatened(player, level, hit)) {
            return new Plan(blockers.toArray(new InteractionHand[0]), false, true);
        }

        boolean enemyAhead = hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof Monster;
        List<InteractionHand> hands = new ArrayList<>(List.of(InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND));
        // Stable: the main hand stays before the off hand among equals, and the off hand goes first among blockers
        hands.sort(Comparator.<InteractionHand>comparingInt(h -> onCooldown(player, h) ? 1 : 0)
                .thenComparingInt(h -> blocks(player.getItemInHand(h)) ? 1 : 0)
                .thenComparingInt(h -> enemyAhead && ranged(player.getItemInHand(h)) ? 0 : 1)
                .thenComparingInt(h -> blocks(player.getItemInHand(h)) && h == InteractionHand.OFF_HAND ? 0 : 1));
        return new Plan(hands.toArray(new InteractionHand[0]), true, true);
    }

    public static boolean blocks(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.BLOCKS_ATTACKS);
    }

    public static boolean onCooldown(LocalPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return !stack.isEmpty() && player.getCooldowns().isOnCooldown(stack);
    }

    private static InteractionHand other(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private static boolean ranged(ItemStack stack) {
        return !stack.isEmpty() && (stack.getUseAnimation() == ItemUseAnimation.BOW || stack.is(Items.CROSSBOW));
    }

    /** The hand with a water bucket to land in, when falling fast onto a block in reach. */
    @Nullable
    private static InteractionHand fallingBucket(LocalPlayer player, @Nullable HitResult hit) {
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return null;
        if (player.onGround() || player.isInWater() || player.getAbilities().flying || player.isFallFlying()) return null;
        if (player.fallDistance <= FALLING) return null;
        for (InteractionHand hand : new InteractionHand[] {InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND}) {
            if (player.getItemInHand(hand).is(Items.WATER_BUCKET) && !onCooldown(player, hand)) return hand;
        }
        return null;
    }

    private static boolean threatened(LocalPlayer player, ClientLevel level, @Nullable HitResult hit) {
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if ((target instanceof Monster || target instanceof Player) && target.isAlive() && player.distanceTo(target) <= TARGET_RANGE) return true;
        }
        for (Monster monster : level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(MELEE_RANGE), Entity::isAlive)) {
            if (monster.distanceTo(player) <= MELEE_RANGE && Combat.isLookingTowards(player, monster.getEyePosition())) return true;
        }
        Vec3 eyes = player.getEyePosition();
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(PROJECTILE_RANGE),
                p -> p.getOwner() != player && p.getDeltaMovement().lengthSqr() > 0.04)) {
            Vec3 toPlayer = eyes.subtract(projectile.position());
            if (projectile.getDeltaMovement().normalize().dot(toPlayer.normalize()) > 0.85) return true;
        }
        return false;
    }
}
