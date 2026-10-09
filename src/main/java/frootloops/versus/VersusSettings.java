package frootloops.versus;

public abstract class VersusSettings {

    public static class QOL {
        public static boolean DO_HOTBAR_SWAPPING_ON_PICK_KEY = true;
        public static boolean DO_SMARTER_BLOCK_PLACING = true;
        public static boolean DO_BEDROCK_BRIDGING = true;
    }

    public static class Combat {
        public static final int MIN_SWEEPING_LEVEL_FOR_SWEEPING_ATTACKS = 1;
        public static boolean CAN_AIM_ASSIST = true;
        public static boolean CAN_HOLD_TO_ATTACK = true;

        public static boolean DO_FOOD_OVERHAUL = true;
        public static boolean DO_FOOD_EATING_INTERRUPTION = true;
        public static boolean DO_FOOD_REDUCED_ON_SPAWN = true;
        public static boolean IS_STARVATION_ENABLED = false;
    }

    public static class Gameplay {
        public static boolean isFastForwardingTime = false;
        public static boolean DO_SLEEP_OVERHAUL = true;
        public static final float SLEEP_TICK_SPEED = 1020f;

        public static boolean DO_ZOMBIE_SEIGES_OUTSIDE_VILLAGES = true;
        public static boolean DO_RAIDS_OUTSIDE_VILLAGES = true;
        public static int BRUSHING_TICKS_PER_STAGE = 2; // Vanilla is 5
    }

    public static class Items {
        public static int MAX_STACK_SIZE_COUNT = 64;
        public static int MAX_COUNT_FOOD = 64;
        public static int MAX_COUNT_STEWS = 16;
        public static int MAX_COUNT_BOTTLED = 8;
        public static int MAX_COUNT_THROWABLE = 64;
        public static int MAX_COUNT_PLACEABLE_ENTITIES = 16;
        public static int MAX_COUNT_BUCKETS = 16;
        public static float EAT_TIME_REGULAR = 1.6F;
        public static float EAT_TIME_MEAT = 1.8F;
        public static float EAT_TIME_SNACK = 0.4F;
        public static float EAT_TIME_VEGGIES = 1.2F;
        public static float EAT_TIME_POISON = 2.4F;
        public static float EAT_TIME_LIQUIDS = 1.0F;
        public static int BREWS_PER_NETHER_WART = 1; // Nether wart is the brewing stand's fuel
    }

}
