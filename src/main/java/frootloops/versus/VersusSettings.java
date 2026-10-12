package frootloops.versus;

public abstract class VersusSettings {

    public static class QOL {
        public static boolean DO_HOTBAR_SWAPPING_ON_PICK_KEY = true;
        // Placed blocks turn to match the stroke they're placed in and their neighbours (PlacementRules)
        public static boolean DO_SMARTER_BLOCK_PLACING = true;
        // Looking past the edge of the block below you places in front of it, as Bedrock's reach-around does
        public static boolean DO_BEDROCK_BRIDGING = true;
        // Right-click held places as soon as the crosshair reaches a new spot, but only on the face the stroke began on
        // and not growing out of its own blocks unless you move that way (ClientPlacement)
        public static boolean DO_PLACING_STROKES = true;
        // A block shows its outline where it will go when it's turned unlike vanilla, or reached around to
        public static boolean DO_PLACEMENT_PREVIEW = true;
        // A block just placed breaks much faster (RecentPlacements)
        public static boolean DO_FAST_UNDO_PLACING = true;
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
        // Recipe book groups by family (oak, deepslate, copper), tools and armor by material (RecipeRules)
        public static boolean DO_FAMILY_RECIPE_GROUPS = true;
        // Stairs from three blocks (four stairs), slabs from two (four slabs), pressure plates from three (two plates)
        public static boolean DO_CHEAPER_STAIRS_AND_SLABS = true;
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
    }

}
