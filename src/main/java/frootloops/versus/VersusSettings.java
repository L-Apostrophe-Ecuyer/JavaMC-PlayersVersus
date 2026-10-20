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
        // The recipe book: a click outside an open group's popup closes it and goes through (RecipeBookPageMixin)
        public static boolean DO_RECIPE_BOOK_CLICK_THROUGH = true;
        // A group's popup opens when the mouse stays on it and closes when the mouse leaves it and its popup
        public static boolean DO_RECIPE_BOOK_HOVER_GROUPS = true;
        // The recipe book lists what can be crafted first, then the rest, each ordered like the inventory sort (RecipeBookOrdering)
        public static boolean DO_SMART_RECIPE_ORDER = true;
        // Double-click a recipe in the book to place the most crafts, double-click again for one (RecipeBookComponentMixin)
        public static boolean DO_RECIPE_DOUBLE_CLICK_MAX = true;
        // A stack in the hand that runs out is refilled from the largest matching stack in the inventory (HotbarRefill)
        public static boolean DO_HOTBAR_REFILL = true;
        // A recipe group's icon cycles through what can be crafted now, when anything can (RecipeButtonMixin)
        public static boolean DO_RECIPE_ICONS_CYCLE_CRAFTABLE = true;
        // Right-click pins a recipe group, or a recipe in its popup, to the top of its section (RecipePins)
        public static boolean DO_RECIPE_BOOK_PINS = true;
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

        // Copper golems carry a whole stack (vanilla: 16) to the chest that already holds the most of it
        public static boolean DO_SMARTER_COPPER_GOLEMS = true;
    }

    // Speeds in blocks per tick: walking is about 0.22, sprinting 0.28, a ladder climbs at about 0.12.
    public static class Leaves {
        // Living things other than animals and spiders push through leaves, slowed the deeper they're in, climb them
        // holding jump and sink holding sneak, and stand on a leaf they're above unless they fall onto it hard (LeafMovement)
        public static boolean DO_LEAF_MOVEMENT = true;
        // The cap on horizontal speed barely touching leaves (above sprinting: none) and fully inside them
        public static double SPEED_CAP_BRUSHING = 0.32;
        public static double SPEED_CAP_INSIDE = 0.12;
        // Above the cap, the most of the speed lost in one tick, so pushing into leaves slows over a few ticks
        public static double MOST_SPEED_LOST_PER_TICK = 0.35;
        // Vertical speed lost per tick fully inside leaves, less the less of the body is in them (a fall into a canopy)
        public static double VERTICAL_DRAG = 0.3;
        // With leaves at the feet and falling no faster than this, holding jump climbs and holding sneak sinks
        public static double CATCH_SPEED = 0.3;
        public static double CLIMB_SPEED = 0.1;
        public static double DESCEND_SPEED = 0.15; // scaffolding's
        // Falling farther than this breaks through the top of leaves instead of landing on it (vanilla powder snow's)
        public static double BREAK_THROUGH_FALL_DISTANCE = 2.5;
        // Landing on top of leaves divides the fall
        public static double LANDING_FALL_DIVISOR = 3.0;
        // Gliders go through leaves, losing this much of their speed per tick fully inside them
        public static double GLIDE_DRAG = 0.3;
        // Swimmers lose this much of their speed per tick fully inside underwater leaves; it stays in their speed for water
        // to slow further, so fully inside they keep about half to two thirds of it (a third sprint-swimming)
        public static double SWIM_DRAG = 0.15;

        // Natural leaves pushed through drop leaf litter to the ground below (cherry: pink petals), each leaf block a
        // tick by chance SHED_RATE * (speed - 0.1)^2, at most MOST_SHED_CHANCE; decaying leaves by DECAY_LITTER_CHANCE (LeafLitter)
        public static boolean DO_LEAF_LITTER_SHEDDING = true;
        public static double SHED_RATE = 0.1;
        public static double MOST_SHED_CHANCE = 0.25;
        public static double DECAY_LITTER_CHANCE = 0.25;
        // How far below the leaves litter can land
        public static int LITTER_DROP_RANGE = 24;
        // Natural fallen leaves brown into leaf litter at this chance per random tick (about an in-game day at the
        // default random tick speed, which reaches a block about every 68 seconds)
        public static double FALLEN_LEAVES_BROWNING_CHANCE = 0.057;
        // A natural leaf cut off from its tree decays as soon as it knows (its scheduled tick), not on a random tick
        // minutes later, so a felled tree's crown comes down at once
        public static boolean DO_INSTANT_LEAF_DECAY = true;
        // With the camera inside leaves, their texture, tinted as they are there, frames the view (client, LeavesView)
        public static boolean DO_LEAVES_VIEW = true;
        // Warnings in the log (logger players-versus/leaves) following players through leaves on both sides, and every
        // time the server pulls a player back or finds nothing under them (LeafDebug); for debugging, off when done
        public static boolean LOG_LEAVES = true;
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
        public static int BREWS_PER_NETHER_WART = 1; // Nether wart is the brewing stand's fuel
    }

}
