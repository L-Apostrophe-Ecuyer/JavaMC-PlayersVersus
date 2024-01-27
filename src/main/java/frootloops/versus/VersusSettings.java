package frootloops.versus;

import frootloops.versus.mod.Combat;

public abstract class VersusSettings {

    private static boolean IS_TICK_TIME_FAST = false; // Accessed and modified by mixins in "environment.sleeping"

    public static boolean isTimeFastForwarding() {
        return IS_TICK_TIME_FAST;
    }

    public static void setTimeFastForwarding(boolean newValue) {
        IS_TICK_TIME_FAST = newValue;
    }

    public static boolean CAN_AIM_ASSIST = true;
    public static boolean CAN_HOLD_TO_ATTACK = true;

    public static void setAimAssist(boolean newValue) {
        CAN_AIM_ASSIST = newValue;
    }

    public static void setHoldToAttack(boolean newValue) {
        CAN_HOLD_TO_ATTACK = newValue;
    }

    public static void setSleepOverhaul(boolean newValue) {
        // TODO
    }

    public static void setFoodAndHealthOverhaul(boolean newValue) {
        // TODO
    }

    public static void setFoodInterruptedByDamage(boolean newValue) {
        // TODO
    }

    public static void setBedrockBlockBridging(boolean newValue) {
        // TODO
    }
}
