package frootloops.versus.mod.players;

import frootloops.versus.mod.Combat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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

import java.util.List;

/**
 * The client's half of {@link HandPlan}: what each hand holds, whether a water bucket can break a fall, and whether
 * the player is under attack or facing an attacker (an enemy within reach in front of them, an enemy or a player at the
 * crosshair, something flying at them, or a hit taken in the last two seconds from something still close).
 */
@Environment(EnvType.CLIENT)
public final class HandSelection {
    private HandSelection() {}

    private static final double MELEE_RANGE = 4.5, TARGET_RANGE = 6.0, PROJECTILE_RANGE = 10.0, FALLING = 3.5, ATTACKER_RANGE = 8.0;

    public static List<HandPlan.Step> plan(LocalPlayer player, ClientLevel level, @Nullable HitResult hit) {
        boolean enemyAhead = hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof Monster;
        return HandPlan.plan(hand(player, InteractionHand.MAIN_HAND), hand(player, InteractionHand.OFF_HAND),
                player.isUsingItem() ? player.getUsedItemHand() : null, fallingBucket(player, hit), player.isShiftKeyDown(),
                threatened(player, level, hit), enemyAhead);
    }

    private static HandPlan.Hand hand(LocalPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return new HandPlan.Hand(blocks(stack), onCooldown(player, hand), ranged(stack));
    }

    public static boolean blocks(ItemStack stack) {
        return !stack.isEmpty() && stack.has(DataComponents.BLOCKS_ATTACKS);
    }

    public static boolean onCooldown(LocalPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return !stack.isEmpty() && player.getCooldowns().isOnCooldown(stack);
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
        // Under attack: hit in the last two seconds (the game forgets the source after that) by something still close
        DamageSource lastHit = player.getLastDamageSource();
        if (lastHit != null && lastHit.getEntity() instanceof LivingEntity attacker && attacker != player && attacker.isAlive()
                && player.distanceTo(attacker) <= ATTACKER_RANGE) return true;
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
