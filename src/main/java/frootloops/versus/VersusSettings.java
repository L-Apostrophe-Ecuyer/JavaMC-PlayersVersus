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
        // The recipe book's groups are an item's recipes; similar items gather in collections (RecipeFamilies), whose popup
        // of icons a click keeps open as a window to drag and close (FamilyWindow)
        public static boolean DO_RECIPE_BOOK_COLLECTIONS = true;
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
        // Falling farther than this breaks through the top of leaves instead of landing on it (a block and a half more
        // than vanilla powder snow's 2.5: a drop of about four and a half blocks still lands on top)
        public static double BREAK_THROUGH_FALL_DISTANCE = 4.0;
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
        // A felled tree's leaves bursting against the ground, by a third of that
        public static double FELLED_LITTER_CHANCE = 0.08;
        // How far below the leaves litter can land
        public static int LITTER_DROP_RANGE = 24;
        // Natural fallen leaves fade at this chance per random tick (about an in-game day at the default random tick
        // speed, which reaches a block about every 68 seconds): browning into leaf litter at this share, else blowing away
        public static double FALLEN_LEAVES_FADING_CHANCE = 0.057;
        public static double FALLEN_LEAVES_BROWNING_SHARE = 0.5;
        // A natural leaf cut off from its tree decays as soon as it knows (its scheduled tick), not on a random tick
        // minutes later, so a felled tree's crown comes down at once
        public static boolean DO_INSTANT_LEAF_DECAY = true;
        // With the camera inside leaves, their texture, tinted as they are there, frames the view (client, LeavesView)
        public static boolean DO_LEAVES_VIEW = true;
    }

    public static class Trees {
        // Growing trees (TreeFeature: new chunks, saplings, bone meal) are made of tree blocks, one per wood (TreeBlock):
        // their wood's log with a face for each side, bark or cut, set once as the tree grows; they drop the vanilla log
        public static boolean DO_TREE_BLOCKS = true;
        // Felling (Felling): cutting a tree block takes its usual time x (1 + CUT_TIME_SCALE x sqrt(n)), n the tree blocks
        // that would fall; a tree falls toward the side its weight sits more than LEAN blocks off the cut, else away from
        // whoever cut it; a group of more than MOST_TREE_BLOCKS connected tree blocks counts as standing
        public static double CUT_TIME_SCALE = 0.5;
        public static double LEAN = 1.5;
        public static int MOST_TREE_BLOCKS = 1024;
        // A cut (a tree block, or the block a tree rests on, breaking, which takes as long as cutting there would) that
        // fells anything creaks, and the tree comes down this many ticks later if nothing holds it up by then
        // (TreeFelling): as falling blocks, at most MOST_FALLING_BLOCKS (tree blocks first, the rest break where they
        // stand), a tree standing over its cut turning over on its hinge as one (Topple); a log hurts what it hits as
        // an anvil does, LOG_DAMAGE_PER_BLOCK for each block it fell (or would have fallen, to go as fast) after the
        // first, at most MOST_LOG_DAMAGE, never taking the last half heart from a player or an owned animal
        public static boolean DO_FELLING = true;
        public static int FELLING_DELAY = 10;
        public static float LOG_DAMAGE_PER_BLOCK = 2.0f;
        public static int MOST_LOG_DAMAGE = 10;
        public static int MOST_FALLING_BLOCKS = 512;
        // Once a tree has grown, the soil under its trunk (dirt, grass, podzol, mycelium, moss) turns to rooted dirt (TreeBlocks)
        public static boolean ROOTED_DIRT_UNDER_TREES = true;
        // A pale oak's trunk goes on down into the ground as roots, ROOT_DEPTH_MIN to ROOT_DEPTH_MAX blocks, and its
        // creaking heart moves down into them HEART_IN_ROOTS of the time (TreeRoots)
        public static boolean DO_ROOTS = true;
        public static int ROOT_DEPTH_MIN = 3;
        public static int ROOT_DEPTH_MAX = 5;
        public static float HEART_IN_ROOTS = 0.75f;
    }

    public static class Mushrooms {
        // Brown and red mushrooms grow by stages of one, two and three, and below y 0 into pale shrooms (MushroomGrowth)
        public static boolean DO_STAGED_MUSHROOMS = true;
        // A mushroom grows where the light is this or less, or on mycelium
        public static int MAX_GROWING_LIGHT = 5;
        // The chance a random tick that could grow a mushroom a stage, turn it pale or have three spread does so
        public static float GROW_CHANCE = 0.25f;
        // Three spread no more where this many blocks of their kind are within 4 blocks across, 1 up or down
        public static int SPREAD_CAP = 8;
        // Conversion: a dose for each DOSE_TICKS spent in pale shrooms, lasting EFFECT_TICKS (each dose refreshes it);
        // all hearts white, SHROOMIFY_TICKS of shaking before shroomifying
        public static int DOSE_TICKS = 100;
        public static int EFFECT_TICKS = 2400;
        public static int SHROOMIFY_TICKS = 100;
        // Pale zombies' and creakings' hits always convert, other zombies' this often (each in place of its own effect)
        public static float ZOMBIE_CONVERSION_CHANCE = 0.04f;
        // Rotten flesh: this chance of a dose lasting ROTTEN_FLESH_CONVERSION_TICKS
        public static float ROTTEN_FLESH_CONVERSION_CHANCE = 0.125f;
        public static int ROTTEN_FLESH_CONVERSION_TICKS = 1200;
    }

    public static class Items {
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
