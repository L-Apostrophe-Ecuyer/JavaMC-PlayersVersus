package frootloops.versus;

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
}
