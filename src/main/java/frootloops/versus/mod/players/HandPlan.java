package frootloops.versus.mod.players;

import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What a right-click does with each hand, in order: a step interacts with what the crosshair is on (a block's use, an
 * entity's, placing what the hand holds), uses the hand's item, or both, the interaction first, as vanilla's does. The
 * first step that does something ends the click. By these rules, the first that applies:
 * <ol>
 * <li>An item is in use (a shield held up, a bow drawn): only its hand, and its item isn't started again.</li>
 * <li>Falling onto a block with a water bucket in a hand: that hand, to land in the water, then the other.</li>
 * <li>Under attack or facing an attacker with an item that blocks attacks, unless sneaking: only the items that block,
 * the off hand's first, and no interaction with the world. Sneak to place or use what you hold instead.</li>
 * <li>Otherwise the main hand interacts first, as only it can open a chest, a door or a table; then the hands that
 * don't block (the main hand's first, a bow first if an enemy is at the crosshair) and last the items that block, the
 * off hand's first: a blocking item is what you want when something attacks, and the last thing when nothing does, so
 * an offhand torch beats a sword in the main hand, and neither beats the chest in front of them.</li>
 * </ol>
 * In all of them a hand whose item is on cooldown goes last (the click leaves its item unused: the server would
 * refuse it).
 */
public final class HandPlan {
    private HandPlan() {}

    /** What a hand holds, as far as the order goes: whether its item blocks attacks, is on cooldown, shoots. */
    public record Hand(boolean blocks, boolean onCooldown, boolean ranged) {}

    /** A hand's turn: whether it interacts with the world, and whether it then uses its item. */
    public record Step(InteractionHand hand, boolean world, boolean item) {}

    public static List<Step> plan(Hand main, Hand off, @Nullable InteractionHand using, @Nullable InteractionHand bucket,
                                  boolean sneaking, boolean threatened, boolean enemyAhead) {
        if (using != null) return List.of(new Step(using, true, false));
        if (bucket != null) return List.of(new Step(bucket, true, true), new Step(other(bucket), true, true));

        List<InteractionHand> blockers = new ArrayList<>();
        for (InteractionHand hand : new InteractionHand[] {InteractionHand.OFF_HAND, InteractionHand.MAIN_HAND}) {
            Hand held = of(hand, main, off);
            if (held.blocks && !held.onCooldown) blockers.add(hand);
        }
        if (!blockers.isEmpty() && !sneaking && threatened) {
            return blockers.stream().map(hand -> new Step(hand, false, true)).toList();
        }

        List<InteractionHand> hands = new ArrayList<>(List.of(InteractionHand.MAIN_HAND, InteractionHand.OFF_HAND));
        // Stable: the main hand stays before the off hand among equals
        hands.sort(Comparator.<InteractionHand>comparingInt(h -> of(h, main, off).onCooldown ? 1 : 0)
                .thenComparingInt(h -> of(h, main, off).blocks ? 1 : 0)
                .thenComparingInt(h -> enemyAhead && of(h, main, off).ranged ? 0 : 1)
                .thenComparingInt(h -> of(h, main, off).blocks && h == InteractionHand.OFF_HAND ? 0 : 1));

        List<Step> steps = new ArrayList<>();
        steps.add(new Step(InteractionHand.MAIN_HAND, true, false));
        for (InteractionHand hand : hands) {
            if (!of(hand, main, off).blocks || of(hand, main, off).onCooldown) add(steps, new Step(hand, hand != InteractionHand.MAIN_HAND, true));
        }
        // The blockers: their interactions, then their items, the off hand's first
        List<InteractionHand> blocking = hands.stream().filter(h -> of(h, main, off).blocks && !of(h, main, off).onCooldown).toList();
        for (InteractionHand hand : blocking) if (hand != InteractionHand.MAIN_HAND) add(steps, new Step(hand, true, false));
        for (InteractionHand hand : blocking) add(steps, new Step(hand, false, true));
        return List.copyOf(steps);
    }

    /** Adds a step, as one with the step before it when that's its hand's interaction alone and this its item alone. */
    private static void add(List<Step> steps, Step step) {
        Step last = steps.getLast();
        if (last.hand == step.hand && last.world && !last.item && !step.world && step.item) steps.set(steps.size() - 1, new Step(step.hand, true, true));
        else steps.add(step);
    }

    private static Hand of(InteractionHand hand, Hand main, Hand off) {
        return hand == InteractionHand.MAIN_HAND ? main : off;
    }

    private static InteractionHand other(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }
}
