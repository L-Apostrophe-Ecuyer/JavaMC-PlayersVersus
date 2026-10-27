package frootloops.versus.mod.players.building;

import frootloops.versus.VersusSettings;
import frootloops.versus.mixin.client.players.BlockItemInvoker;
import frootloops.versus.mod.environment.blocks.LadderBlockItem;
import frootloops.versus.mod.players.HandSelection;
import frootloops.versus.mod.players.RayTraceHandler;
import frootloops.versus.mod.players.death.RespawnNearbyPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ScaffoldingBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The client's part of placing blocks, which is all of it but the quick undo ({@link RecentPlacements}): what it
 * remembers of the player's placements, the state the rules choose for the next one ({@link PlacementRules}), and how
 * a right-click held down places them.
 * <p>
 * A chosen state is placed by clicking so that vanilla places it: the face clicked, the height clicked on a side, and
 * the way the player looks, sent just before the click and put back just after. Each way of clicking is tried here
 * first, with vanilla's own placement, and only one that gives the chosen state is sent; if none does, the click is
 * vanilla's. Any server takes it as an ordinary placement: the block clicked is within reach, the spot clicked is on
 * its face. To click another face of the spot itself (a log's axis), what the crosshair is on must be a block that
 * isn't used by a click (not a chest, a door, a lever), so a click that would use it still does.
 * <p>
 * A stroke is right-click held since the first block it placed. Within it, once held as long as vanilla waits before
 * repeating, a block goes down as soon as the crosshair reaches a new spot for it rather than every fourth tick, but
 * only where the stroke means it to go:
 * <ul>
 * <li>On the face the stroke began on: a floor stays a floor, a wall a wall, a bridge a bridge.</li>
 * <li>Not out of a block of the stroke itself, unless the player moved that way by half a block since placing it: a
 * floor doesn't tower, a wall doesn't grow toward the player; a bridge walked along or a pillar jumped up does.</li>
 * <li>Never twice in the same spot (no slab doubled under the crosshair).</li>
 * </ul>
 * A slab clicked where vanilla would double one the player just placed (the last placement, or any of the stroke's)
 * goes beside it instead, in the same half: on along the row, or out of the side clicked ({@link #aim}). Sneak to
 * double it.
 * A single click places as vanilla does. Reach-around: looking down past the edge of the block below you, where the
 * crosshair finds nothing, places in front of that block, as Bedrock does.
 */
@Environment(EnvType.CLIENT)
public final class ClientPlacement {
    public static final ClientPlacement INSTANCE = new ClientPlacement();

    private ClientPlacement() {}

    /** How far the player must have moved along the stroke's face to grow it out of its own block. */
    private static final double GROWTH = 0.5;
    /** How long (in ticks) the last placement guides the next one beside it. */
    private static final int RECENT = 60;
    /** How far back the eyes are moved to find the block a reach-around goes in front of. */
    private static final double REACH_AROUND = 0.5;
    /**
     * How long (in ticks) right-click is held after a stroke's first block before blocks go down on arrival: vanilla's
     * delay before it repeats, so that a click held a moment while the mouse moves still places one block.
     */
    private static final int FIRST_REPEAT = 4;
    /** How much closer than the server's limit (a block past the reach) a spot clicked must be. */
    private static final double REACH_MARGIN = 0.5;
    /** The most ways of clicking tried for one placement. */
    private static final int MOST_TRIES = 96;

    private static final class Stroke {
        final Item item;
        final BlockState first;
        final Direction face;
        final long start;
        /** Each block of the stroke, with where the player stood when placing it. */
        final Map<Long, Vec3> placed = new HashMap<>();

        Stroke(Item item, BlockState first, Direction face, long start) {
            this.item = item;
            this.first = first;
            this.face = face;
            this.start = start;
        }
    }

    private boolean held;
    @Nullable private Stroke stroke;

    @Nullable private PlacementRules.Placed last, previous;
    private long lastTime, previousTime;

    /** The spot a held right-click last tried, and whether the player was out of it. */
    private long triedPos = Long.MIN_VALUE;
    private boolean triedFree;

    public static void register() {
        ClientTickEvents.START_CLIENT_TICK.register(INSTANCE::startTick);
        // Any of the mod's own packets tells whether the server runs it
        ClientPlayConnectionEvents.JOIN.register((listener, sender, minecraft) -> RecentPlacements.serverAgrees(ClientPlayNetworking.canSend(RespawnNearbyPayload.ID)));
        ClientPlayConnectionEvents.DISCONNECT.register((listener, minecraft) -> RecentPlacements.serverAgrees(false));
        PlacementPreview.register();
    }

    /** Before the keys are handled: a release or a new press of right-click ends the stroke. */
    private void startTick(Minecraft minecraft) {
        boolean down = minecraft.player != null && minecraft.options.keyUse.isDown();
        if (!down || !held) {
            stroke = null;
            triedPos = Long.MIN_VALUE;
        }
        held = down;
    }

    // Choosing the state

    /** A placement of the block in hand: the click, its context, vanilla's state, and the state the rules choose. */
    public record Choice(BlockHitResult hit, BlockPlaceContext context, BlockState vanilla, BlockState chosen) {
        public BlockPos pos() {
            return context.getClickedPos();
        }
    }

    /** What a click of the block in hand on hit would place, and what the rules make of it; null if nothing. */
    @Nullable
    public Choice choose(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem item)) return null;
        BlockPlaceContext context = item.updatePlacementContext(new BlockPlaceContext(player, hand, stack, hit));
        if (context == null || !context.canPlace()) return null;
        BlockState vanilla = ((BlockItemInvoker) item).playersVersus$getPlacementState(context);
        if (vanilla == null) return null;
        if (!VersusSettings.QOL.DO_SMARTER_BLOCK_PLACING || player.isShiftKeyDown()) return new Choice(hit, context, vanilla, vanilla);
        Level level = player.level();
        BlockPos pos = context.getClickedPos();
        BlockState first = stroke != null && held && stroke.item == item ? stroke.first : null;
        BlockState chosen = PlacementRules.resolve(vanilla, pos, context.getClickedFace(), first, recent(level, pos), player.getEyePosition(),
                PlacementRules.View.of(level));
        return new Choice(hit, context, vanilla, chosen != vanilla && PlacementRules.acceptable(vanilla, chosen) ? chosen : vanilla);
    }

    /** The block placed just before, if it's recent and not at pos. */
    @Nullable
    private PlacementRules.Placed recent(Level level, BlockPos pos) {
        if (last == null) return null;
        long age = level.getGameTime() - lastTime;
        return age < 0 || age > RECENT || last.pos().equals(pos) ? null : last;
    }

    // Clicking for it

    /** A way of clicking: where, and which way the player looks. */
    public record Click(BlockHitResult hit, float yRot, float xRot) {}

    /**
     * The block in hand used where the crosshair is, as the right-click's use of it: placed where {@link #aim} puts it,
     * in the state the rules choose, by clicking so that vanilla places it, and remembered.
     */
    public InteractionResult use(MultiPlayerGameMode gameMode, LocalPlayer player, InteractionHand hand, BlockHitResult crosshair) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem)) return gameMode.useItemOn(player, hand, crosshair);
        BlockHitResult hit = aim(player, hand, crosshair);
        Item item = stack.getItem();
        Choice choice = choose(player, hand, hit);
        Click click = choice == null || choice.chosen == choice.vanilla ? null : clickFor(player, hand, hit, choice);
        BlockState before = choice == null ? null : player.level().getBlockState(choice.pos());

        InteractionResult result = click == null ? gameMode.useItemOn(player, hand, hit) : useAs(gameMode, player, hand, click);

        if (choice != null && result instanceof InteractionResult.Success) {
            BlockState after = player.level().getBlockState(choice.pos());
            if (after != before && !after.isAir()) placed(player, item, choice.pos(), after, crosshair.getDirection());
        }
        return result;
    }

    /** The use, clicked as click says, the player looking that way for it on both sides. */
    private static InteractionResult useAs(MultiPlayerGameMode gameMode, LocalPlayer player, InteractionHand hand, Click click) {
        float yRot = player.getYRot(), xRot = player.getXRot();
        boolean turn = click.yRot != yRot || click.xRot != xRot;
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (turn && connection == null) return gameMode.useItemOn(player, hand, click.hit);
        if (turn) {
            connection.send(new ServerboundMovePlayerPacket.Rot(click.yRot, click.xRot, player.onGround(), player.horizontalCollision));
            player.setYRot(click.yRot);
            player.setXRot(click.xRot);
        }
        try {
            return gameMode.useItemOn(player, hand, click.hit);
        } finally {
            if (turn) {
                player.setYRot(yRot);
                player.setXRot(xRot);
                connection.send(new ServerboundMovePlayerPacket.Rot(yRot, xRot, player.onGround(), player.horizontalCollision));
            }
        }
    }

    /** A way of clicking that makes vanilla place the chosen state where vanilla would place its own, or null. */
    @Nullable
    private Click clickFor(LocalPlayer player, InteractionHand hand, BlockHitResult hit, Choice choice) {
        Level level = player.level();
        BlockPos pos = choice.pos();
        // A click on a block that's used by clicking it stays vanilla's, so that it's still used
        if (!choice.context.replacingClickedOnBlock() && !plain(level.getBlockState(hit.getBlockPos()).getBlock())) return null;

        BlockState vanilla = choice.vanilla, chosen = choice.chosen;
        // A trapdoor faces the side it's clicked on, or away from the player when clicked on a top or bottom
        boolean face = differs(vanilla, chosen, BlockStateProperties.AXIS) || differs(vanilla, chosen, BlockStateProperties.HALF)
                || differs(vanilla, chosen, BlockStateProperties.SLAB_TYPE) || chosen.getBlock() instanceof TrapDoorBlock;
        boolean facing = differs(vanilla, chosen, BlockStateProperties.HORIZONTAL_FACING) || differs(vanilla, chosen, BlockStateProperties.FACING);
        boolean rotation = differs(vanilla, chosen, BlockStateProperties.ROTATION_16);

        // Where: the face clicked at another height, then any face of the spot itself
        List<BlockHitResult> hits = new ArrayList<>();
        hits.add(hit);
        if (hit.getDirection().getAxis().isHorizontal()) {
            hits.add(atHeight(hit, pos, 0.25));
            hits.add(atHeight(hit, pos, 0.75));
        }
        if (face && player.isWithinBlockInteractionRange(pos, REACH_MARGIN)) {
            for (Direction side : Direction.values()) {
                if (side.getAxis().isVertical()) hits.add(onFace(pos, side, 0.5));
                else {
                    hits.add(onFace(pos, side, 0.25));
                    hits.add(onFace(pos, side, 0.75));
                }
            }
        }

        // Which way the player looks: as they do, then the nearest ways that turn the block
        float yRot = player.getYRot(), xRot = player.getXRot();
        List<float[]> looks = new ArrayList<>();
        looks.add(new float[] {yRot, xRot});
        if (facing) {
            List<Float> ways = new ArrayList<>();
            for (Direction direction : Direction.Plane.HORIZONTAL) ways.add(direction.toYRot());
            ways.sort(Comparator.comparingDouble(way -> Math.abs(Mth.wrapDegrees(way - yRot))));
            for (float way : ways) looks.add(new float[] {way, xRot});
            for (float way : ways) looks.add(new float[] {way, 0.0F});
            looks.add(new float[] {yRot, 90.0F});
            looks.add(new float[] {yRot, -90.0F});
        }
        if (rotation) {
            List<Float> ways = new ArrayList<>();
            for (int step = 0; step < 16; step++) ways.add(step * 22.5F);
            ways.sort(Comparator.comparingDouble(way -> Math.abs(Mth.wrapDegrees(way - yRot))));
            for (float way : ways) looks.add(new float[] {way, xRot});
        }

        ItemStack stack = player.getItemInHand(hand);
        BlockItem item = (BlockItem) stack.getItem();
        int tries = 0;
        for (BlockHitResult candidate : hits) {
            for (float[] look : looks) {
                if (candidate == hit && look[0] == yRot && look[1] == xRot) continue; // Vanilla's own
                if (++tries > MOST_TRIES) return null;
                BlockState placed = simulate(player, hand, stack, item, candidate, look[0], look[1], pos);
                if (placed != null && PlacementRules.sameTurn(placed, chosen)) return new Click(candidate, look[0], look[1]);
            }
        }
        return null;
    }

    /** What vanilla would place for a click, the player looking that way; null if not at pos. */
    @Nullable
    private static BlockState simulate(LocalPlayer player, InteractionHand hand, ItemStack stack, BlockItem item, BlockHitResult hit,
                                       float yRot, float xRot, BlockPos pos) {
        float y = player.getYRot(), x = player.getXRot();
        player.setYRot(yRot);
        player.setXRot(xRot);
        try {
            BlockPlaceContext context = item.updatePlacementContext(new BlockPlaceContext(player, hand, stack, hit));
            if (context == null || !context.canPlace() || !context.getClickedPos().equals(pos)) return null;
            return ((BlockItemInvoker) item).playersVersus$getPlacementState(context);
        } finally {
            player.setYRot(y);
            player.setXRot(x);
        }
    }

    private static boolean differs(BlockState a, BlockState b, Property<?> property) {
        return a.hasProperty(property) && !a.getValue(property).equals(b.getValue(property));
    }

    /** The same click at a height on the side of pos (the spot placed, level with the block clicked). */
    private static BlockHitResult atHeight(BlockHitResult hit, BlockPos pos, double height) {
        Vec3 at = hit.getLocation();
        return new BlockHitResult(new Vec3(at.x, pos.getY() + height, at.z), hit.getDirection(), hit.getBlockPos(), false);
    }

    /** A click on a face of the spot itself, at a height for a side. */
    private static BlockHitResult onFace(BlockPos pos, Direction face, double height) {
        Vec3 center = Vec3.atCenterOf(pos);
        double y = face == Direction.UP ? pos.getY() + 1.0 : face == Direction.DOWN ? pos.getY() : pos.getY() + height;
        return new BlockHitResult(new Vec3(center.x + face.getStepX() * 0.5, y, center.z + face.getStepZ() * 0.5), face, pos, false);
    }

    private static final Map<Class<?>, Boolean> PLAIN = new ConcurrentHashMap<>();

    /** Whether a block does nothing when clicked (no use of its own, with an item or without), so a click can pass it. */
    private static boolean plain(Block block) {
        return PLAIN.computeIfAbsent(block.getClass(), type -> {
            for (Class<?> c = type; c != null && c != Block.class && c != BlockBehaviour.class; c = c.getSuperclass()) {
                for (Method method : c.getDeclaredMethods()) {
                    if (method.getName().equals("useWithoutItem") || method.getName().equals("useItemOn")) return false;
                }
            }
            return true;
        });
    }

    // Slabs going on beside the last

    /**
     * The click a slab in hand makes instead of doubling one the player just placed (the last placement, or any of the
     * held stroke's): on the face of that slab the new one goes out of ({@link PlacementRules#slabStep}), at its height,
     * so vanilla places it beside, in the same half. Sneaking, an older slab, or no room beside: the click as it is.
     */
    public BlockHitResult aim(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        if (!VersusSettings.QOL.DO_SMARTER_BLOCK_PLACING || player.isShiftKeyDown() || hit.getType() != HitResult.Type.BLOCK) return hit;
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof SlabBlock)) return hit;
        Level level = player.level();
        BlockPos clicked = hit.getBlockPos();
        BlockState slab = level.getBlockState(clicked);
        if (!slab.is(item.getBlock()) || slab.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE || !justPlaced(level, clicked)) return hit;
        if (!new BlockPlaceContext(player, hand, stack, hit).replacingClickedOnBlock()) return hit; // Vanilla wouldn't double it
        Direction step = PlacementRules.slabStep(hit.getDirection(), travel(level), player.getDirection());
        BlockHitResult beside = onFace(clicked, step, slab.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.TOP ? 0.75 : 0.25);
        BlockPos target = target(player, hand, beside);
        return target != null && target.equals(clicked.relative(step)) ? beside : hit;
    }

    /** Whether the block at pos is one the player just placed: the last placement, or one of the held stroke's. */
    private boolean justPlaced(Level level, BlockPos pos) {
        if (stroke != null && held && stroke.placed.containsKey(pos.asLong())) return true;
        long age = level.getGameTime() - lastTime;
        return last != null && last.pos().equals(pos) && age >= 0 && age <= RECENT;
    }

    /** The way the last two placements went, if they were placed beside each other on a level one after the other. */
    @Nullable
    private Direction travel(Level level) {
        if (last == null || previous == null || lastTime - previousTime > RECENT || level.getGameTime() - lastTime > RECENT) return null;
        return PlacementRules.travel(previous.pos(), last.pos());
    }

    // Remembering

    private void placed(LocalPlayer player, Item item, BlockPos pos, BlockState state, Direction face) {
        if (held && strokes(item)) {
            if (stroke == null || stroke.item != item) stroke = new Stroke(item, state, face, player.level().getGameTime());
            stroke.placed.put(pos.asLong(), player.position());
        }
        previous = last;
        previousTime = lastTime;
        last = new PlacementRules.Placed(pos.immutable(), state);
        lastTime = player.level().getGameTime();
    }

    // Strokes

    /** Items a stroke places: blocks, but not ladders and scaffolding, which hold to climb a column or a ledge. */
    private static boolean strokes(Item item) {
        return VersusSettings.QOL.DO_PLACING_STROKES && item instanceof BlockItem
                && !(item instanceof ScaffoldingBlockItem) && !(item instanceof LadderBlockItem);
    }

    /** Whether a right-click held down shouldn't place the block in hand on hit, as it's not where the stroke goes. */
    public boolean refuses(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        if (stroke == null || !held) return false;
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() != stroke.item || !strokes(stack.getItem())) return false;
        if (hit.getDirection() != stroke.face) return true;
        BlockHitResult aimed = aim(player, hand, hit);
        BlockPos target = target(player, hand, aimed);
        if (target == null) return false; // Nothing to place there; the click goes on as vanilla's
        if (stroke.placed.containsKey(target.asLong())) return true;
        // A slab going on beside one of the stroke's grows the row, not out of that slab
        if (aimed != hit) return false;
        Vec3 from = stroke.placed.get(hit.getBlockPos().asLong());
        if (from == null) return false;
        Vec3 moved = player.position().subtract(from);
        Direction face = stroke.face;
        return moved.x * face.getStepX() + moved.y * face.getStepY() + moved.z * face.getStepZ() < GROWTH;
    }

    /** A block item's use on hit is about to be tried with right-click held. */
    public void tried(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        if (!held || !strokes(player.getItemInHand(hand).getItem())) return;
        BlockPos target = target(player, hand, aim(player, hand, hit));
        triedPos = target == null ? Long.MIN_VALUE : target.asLong();
        triedFree = target != null && free(player, target);
    }

    /**
     * Before the keys are handled: whether right-click, held in a stroke, should place now rather than when its delay
     * runs out, as the crosshair has reached a new spot for the stroke's block (or the player moved out of it).
     */
    public boolean arrived(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (stroke == null || !held || player == null || player.isUsingItem() || minecraft.level == null) return false;
        if (minecraft.level.getGameTime() - stroke.start < FIRST_REPEAT) return false;
        InteractionHand hand = player.getMainHandItem().getItem() == stroke.item ? InteractionHand.MAIN_HAND
                : player.getOffhandItem().getItem() == stroke.item ? InteractionHand.OFF_HAND : null;
        if (hand == null || !strokes(stroke.item) || HandSelection.onCooldown(player, hand)) return false;
        BlockHitResult hit = minecraft.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK ? blockHit
                : minecraft.hitResult != null && minecraft.hitResult.getType() == HitResult.Type.MISS ? reachAround(player, hand) : null;
        if (hit == null || refuses(player, hand, hit)) return false;
        BlockPos target = target(player, hand, aim(player, hand, hit));
        if (target == null || !free(player, target)) return false;
        return target.asLong() != triedPos || !triedFree;
    }

    /** Where the block in hand would go for hit, or null if it can't go anywhere. */
    @Nullable
    private static BlockPos target(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem item)) return null;
        BlockPlaceContext context = item.updatePlacementContext(new BlockPlaceContext(player, hand, stack, hit));
        return context == null || !context.canPlace() ? null : context.getClickedPos();
    }

    private static boolean free(LocalPlayer player, BlockPos pos) {
        return !player.getBoundingBox().intersects(new AABB(pos));
    }

    // Reach-around

    /**
     * Where a block placed with the crosshair on nothing goes: in front of the block the player stands on (or is just
     * above), the way they face, when they look down past its edge; null if not there.
     */
    @Nullable
    public BlockHitResult reachAround(LocalPlayer player, InteractionHand hand) {
        if (!VersusSettings.QOL.DO_BEDROCK_BRIDGING || player.getXRot() < 0.0F || player.isSpectator()) return null;
        Item item = player.getItemInHand(hand).getItem();
        if (!(item instanceof BlockItem) || item instanceof ScaffoldingBlockItem) return null;

        Direction facing = player.getDirection();
        Vec3 eyes = player.getEyePosition().subtract(facing.getStepX() * REACH_AROUND, 0.0, facing.getStepZ() * REACH_AROUND);
        HitResult hit = RayTraceHandler.rayTrace(player, player.level(), eyes, player.getViewVector(1.0F), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player.blockInteractionRange());
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return null;

        BlockPos against = blockHit.getBlockPos();
        // The block under the player's feet or beside it at that height: standing on it, on a slab beside it, or
        // jumping off it; not one further down
        double above = player.getY() - (against.getY() + 1);
        if (above < -0.55 || above > 1.5) return null;
        BlockPos target = against.relative(facing);
        if (!player.level().getBlockState(target).canBeReplaced() || !player.isWithinBlockInteractionRange(against, REACH_MARGIN)) return null;
        Vec3 face = Vec3.atCenterOf(against).add(facing.getStepX() * 0.5, 0.0, facing.getStepZ() * 0.5);
        return new BlockHitResult(face, facing, against, false);
    }

    // Preview

    private long previewKey = Long.MIN_VALUE;
    @Nullable private BlockState previewVanilla, previewChosen;
    private boolean previewClickable;

    /**
     * What a right-click with the block in hand on hit would place: the choice, with the chosen state vanilla's when
     * no click can place it (as the click then places vanilla's); null if nothing.
     */
    @Nullable
    public Choice preview(LocalPlayer player, InteractionHand hand, BlockHitResult crosshair) {
        BlockHitResult hit = aim(player, hand, crosshair);
        Choice choice = choose(player, hand, hit);
        if (choice == null || choice.chosen == choice.vanilla) return choice;
        // Searching for the click again only when something changed
        long key = choice.pos().asLong() * 31 + hit.getBlockPos().asLong() * 7 + hit.getDirection().ordinal();
        if (key != previewKey || choice.vanilla != previewVanilla || choice.chosen != previewChosen) {
            previewKey = key;
            previewVanilla = choice.vanilla;
            previewChosen = choice.chosen;
            previewClickable = clickFor(player, hand, hit, choice) != null;
        }
        return previewClickable ? choice : new Choice(choice.hit, choice.context, choice.vanilla, choice.vanilla);
    }
}
