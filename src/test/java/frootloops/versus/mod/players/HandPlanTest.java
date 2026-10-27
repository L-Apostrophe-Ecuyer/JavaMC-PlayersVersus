package frootloops.versus.mod.players;

import frootloops.versus.mod.players.HandPlan.Hand;
import frootloops.versus.mod.players.HandPlan.Step;
import net.minecraft.world.InteractionHand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static net.minecraft.world.InteractionHand.MAIN_HAND;
import static net.minecraft.world.InteractionHand.OFF_HAND;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HandPlanTest {

    private static final Hand SWORD = new Hand(true, false, false), SHIELD = new Hand(true, false, false);
    private static final Hand TORCH = new Hand(false, false, false), BOW = new Hand(false, false, true);
    private static final Hand COOLING_SHIELD = new Hand(true, true, false);

    private static Step world(InteractionHand hand) {
        return new Step(hand, true, false);
    }

    private static Step item(InteractionHand hand) {
        return new Step(hand, false, true);
    }

    private static Step both(InteractionHand hand) {
        return new Step(hand, true, true);
    }

    private static List<Step> calm(Hand main, Hand off) {
        return HandPlan.plan(main, off, null, null, false, false, false);
    }

    @Test
    void aSwordAndAShieldOpenTheChestBeforeTheShieldGoesUp() {
        // The main hand is the one a chest, door or table answers; the shield only goes up once neither hand used the block
        assertEquals(List.of(world(MAIN_HAND), both(OFF_HAND), item(MAIN_HAND)), calm(SWORD, SHIELD));
    }

    @Test
    void anOffhandTorchStillBeatsASwordButNotTheChestInFront() {
        assertEquals(List.of(world(MAIN_HAND), both(OFF_HAND), item(MAIN_HAND)), calm(SWORD, TORCH));
    }

    @Test
    void aShieldInTheOffhandWaitsForTheMainHandsItem() {
        assertEquals(List.of(both(MAIN_HAND), both(OFF_HAND)), calm(TORCH, SHIELD));
    }

    @Test
    void underAttackOnlyTheBlockersBlockAndTheWorldIsLeftAlone() {
        assertEquals(List.of(item(OFF_HAND), item(MAIN_HAND)), HandPlan.plan(SWORD, SHIELD, null, null, false, true, false));
        assertEquals(List.of(item(MAIN_HAND)), HandPlan.plan(SWORD, TORCH, null, null, false, true, false));
    }

    @Test
    void sneakingUnderAttackPlacesOrUsesAsWhenCalm() {
        assertEquals(calm(SWORD, TORCH), HandPlan.plan(SWORD, TORCH, null, null, true, true, false));
    }

    @Test
    void aBlockerOnCooldownNeitherBlocksNorUsesTheBlockFirst() {
        // Under attack with the only blocker cooling down: as when calm, the cooling one last
        assertEquals(List.of(both(MAIN_HAND), both(OFF_HAND)), HandPlan.plan(TORCH, COOLING_SHIELD, null, null, false, true, false));
    }

    @Test
    void anItemInUseKeepsItsHand() {
        assertEquals(List.of(world(OFF_HAND)), HandPlan.plan(SWORD, SHIELD, OFF_HAND, null, false, true, false));
    }

    @Test
    void aWaterBucketWhileFallingGoesFirst() {
        assertEquals(List.of(both(OFF_HAND), both(MAIN_HAND)), HandPlan.plan(SWORD, TORCH, null, OFF_HAND, false, true, false));
    }

    @Test
    void aBowGoesFirstAtAnEnemyAhead() {
        assertEquals(List.of(world(MAIN_HAND), both(OFF_HAND), item(MAIN_HAND)), HandPlan.plan(SWORD, BOW, null, null, false, false, true));
        assertEquals(List.of(world(MAIN_HAND), both(OFF_HAND), item(MAIN_HAND)), HandPlan.plan(TORCH, BOW, null, null, false, false, true));
    }
}
