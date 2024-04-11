package frootloops.versus;

import frootloops.versus.mod.Combat;

public abstract class VersusSettings {

    public static boolean CAN_AIM_ASSIST = true;
    public static boolean CAN_HOLD_TO_ATTACK = true;
    public static boolean DO_BEDROCK_BRIDGING = true;
    public static boolean DO_SLEEP_OVERHAUL = true;
    public static boolean DO_FOOD_OVERHAUL = true;
    public static boolean DO_FOOD_EATING_INTERRUPTION = true;
    public static boolean DO_FOOD_REDUCED_ON_SPAWN = true;
    public static boolean DO_ZOMBIE_SEIGES_OUTSIDE_VILLAGES = true;
    public static boolean DO_ZOMBIE_SOUND_DETECTION = true;

    public static void setAimAssist(boolean newValue) {
        CAN_AIM_ASSIST = newValue;
    }

    public static void setHoldToAttack(boolean newValue) {
        CAN_HOLD_TO_ATTACK = newValue;
    }

    public static void setSleepOverhaul(boolean newValue) {
        DO_SLEEP_OVERHAUL = newValue;
    }

    public static void setFoodAndHealthOverhaul(boolean newValue) {DO_FOOD_OVERHAUL = newValue;}

    public static void setFoodReducedOnSpawn(boolean newValue) {DO_FOOD_REDUCED_ON_SPAWN = newValue;}

    public static void setFoodInterruptedByDamage(boolean newValue) {
        DO_FOOD_EATING_INTERRUPTION = newValue;
    }

    public static void setBedrockBlockBridging(boolean newValue) {
        DO_BEDROCK_BRIDGING = newValue;
    }



    /*
    ------------------------------------------------------------------------------------------------------------
    PURELY FOR VERSIONS >1.20.4, WHERE THE TICK COMMAND WASN'T YET INTRODUCTED
     */
    private static boolean IS_TICK_TIME_FAST = false; // Accessed and modified by mixins in "environment.sleeping"

    public static boolean isTimeFastForwarding() {
        return IS_TICK_TIME_FAST;
    }

    public static void setTimeFastForwarding(boolean newValue) {
        IS_TICK_TIME_FAST = newValue;
    }
}
