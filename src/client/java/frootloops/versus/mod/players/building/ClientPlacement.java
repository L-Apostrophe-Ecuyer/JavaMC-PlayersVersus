package frootloops.versus.mod.players.building;

import frootloops.versus.VersusSettings;
import frootloops.versus.mixin.client.players.BlockItemInvoker;
import frootloops.versus.mod.environment.blocks.LadderBlockItem;
import frootloops.versus.mod.players.HandSelection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.ScaffoldingBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The client's part of placing blocks: what it remembers of the player's placements, for the rules that turn them
 * ({@link PlacementRules}), and how a right-click held down places them.
 * <p>
 * A stroke is right-click held since the first block it placed. Within it, a block goes down as soon as the crosshair
 * reaches a new spot for it rather than every fourth tick, but only where the stroke means it to go:
 * <ul>
 * <li>On the face the stroke began on: a floor stays a floor, a wall a wall, a bridge a bridge.</li>
 * <li>Not out of a block of the stroke itself, unless the player moved that way by half a block since placing it: a
 * floor doesn't tower, a wall doesn't grow toward the player; a bridge walked along or a pillar jumped up does.</li>
 * <li>Never twice in the same spot (no slab doubled under the crosshair).</li>
 * </ul>
 * A single click places as vanilla does. Reach-around: looking down past the edge of the block below you, where the
 * crosshair finds nothing, places in front of that block, as Bedrock does.
 */
@Environment(EnvType.CLIENT)
public final class ClientPlacement implements PlacementRules.ClientSide {
    public static final ClientPlacement INSTANCE = new ClientPlacement();

    private ClientPlacement() {}

    /** How far the player must have moved along the stroke's face to grow it out of its own block. */
    private static final double GROWTH = 0.5;
    /** How long (in ticks) the last placement settles the half of the next one beside it. */
    private static final int RECENT = 30;
    /** How far back the eyes are moved to find the block a reach-around goes in front of. */
    private static final double REACH_AROUND = 0.5;

    private static final class Stroke {
        final Item item;
        final BlockState first;
        final Direction face;
        /** Each block of the stroke, with where the player stood when placing it. */
        final Map<Long, Vec3> placed = new HashMap<>();

        Stroke(Item item, BlockState first, Direction face) {
            this.item = item;
            this.first = first;
            this.face = face;
        }
    }

    private boolean held;
    @Nullable private Stroke stroke;

    private long lastPos;
    @Nullable private BlockState lastState;
    private long lastTime;

    /** The spot a held right-click last tried, and whether the player was out of it. */
    private long triedPos = Long.MIN_VALUE;
    private boolean triedFree;

    /** Set while the preview asks what a placement would be, so that nothing is sent or remembered. */
    private boolean previewing;
    @Nullable private BlockState previewVanilla, previewChosen;

    public static void register() {
        PlacementRules.client = INSTANCE;
        ClientTickEvents.START_CLIENT_TICK.register(INSTANCE::startTick);
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

    // The rules' hooks (PlacementRules.ClientSide)

    @Override
    public BlockState choose(BlockPlaceContext context, BlockState vanilla) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) return vanilla;
        // A server without the mod would place vanilla's anyway
        if (!ClientPlayNetworking.canSend(PlacementHintPayload.TYPE)) return vanilla;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState first = stroke != null && held && stroke.item == context.getItemInHand().getItem() ? stroke.first : null;
        return PlacementRules.resolve(vanilla, pos, context.getClickedFace(), first, recent(level, pos), player.getEyePosition(),
                PlacementRules.View.of(level));
    }

    @Override
    public void chosen(BlockPlaceContext context, BlockState vanilla, BlockState chosen) {
        if (previewing) {
            previewVanilla = vanilla;
            previewChosen = chosen;
            return;
        }
        if (chosen != vanilla) ClientPlayNetworking.send(new PlacementHintPayload(context.getClickedPos(), Block.getId(chosen)));
    }

    @Override
    public void placed(BlockPlaceContext context, BlockState state) {
        Player player = context.getPlayer();
        if (previewing || player == null) return;
        BlockPos pos = context.getClickedPos();
        Item item = context.getItemInHand().getItem();
        if (held && strokes(item)) {
            if (stroke == null || stroke.item != item) stroke = new Stroke(item, state, context.getClickedFace());
            stroke.placed.put(pos.asLong(), player.position());
        }
        lastPos = pos.asLong();
        lastState = state;
        lastTime = context.getLevel().getGameTime();
    }

    /** The block placed just before right next to pos, if any. */
    @Nullable
    private BlockState recent(Level level, BlockPos pos) {
        if (lastState == null) return null;
        long age = level.getGameTime() - lastTime;
        if (age < 0 || age > RECENT) return null;
        BlockPos last = BlockPos.of(lastPos);
        if (last.equals(pos) || Math.max(Math.abs(last.getX() - pos.getX()), Math.max(Math.abs(last.getY() - pos.getY()), Math.abs(last.getZ() - pos.getZ()))) > 1) return null;
        return lastState;
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
        BlockPos target = target(player, hand, hit);
        if (target == null) return false; // Nothing to place there; the click goes on as vanilla's
        if (stroke.placed.containsKey(target.asLong())) return true;
        Vec3 from = stroke.placed.get(hit.getBlockPos().asLong());
        if (from == null) return false;
        Vec3 moved = player.position().subtract(from);
        Direction face = stroke.face;
        return moved.x * face.getStepX() + moved.y * face.getStepY() + moved.z * face.getStepZ() < GROWTH;
    }

    /** A block item's use on hit is about to be tried with right-click held. */
    public void tried(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        if (!held || !strokes(player.getItemInHand(hand).getItem())) return;
        BlockPos target = target(player, hand, hit);
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
        InteractionHand hand = player.getMainHandItem().getItem() == stroke.item ? InteractionHand.MAIN_HAND
                : player.getOffhandItem().getItem() == stroke.item ? InteractionHand.OFF_HAND : null;
        if (hand == null || !strokes(stroke.item) || HandSelection.onCooldown(player, hand)) return false;
        BlockHitResult hit = minecraft.hitResult instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK ? blockHit
                : minecraft.hitResult != null && minecraft.hitResult.getType() == HitResult.Type.MISS ? reachAround(player, hand) : null;
        if (hit == null || refuses(player, hand, hit)) return false;
        BlockPos target = target(player, hand, hit);
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
        if (!(item instanceof BlockItem) || item instanceof PlaceOnWaterBlockItem || item instanceof ScaffoldingBlockItem) return null;

        Direction facing = player.getDirection();
        Vec3 eyes = player.getEyePosition().subtract(facing.getStepX() * REACH_AROUND, 0.0, facing.getStepZ() * REACH_AROUND);
        Vec3 end = eyes.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
        BlockHitResult hit = player.level().clip(new ClipContext(eyes, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) return null;

        BlockPos against = hit.getBlockPos();
        // The block under the player's feet or beside it at that height: standing on it, on a slab beside it, or
        // jumping off it; not one further down
        double above = player.getY() - (against.getY() + 1);
        if (above < -0.55 || above > 1.5) return null;
        BlockPos target = against.relative(facing);
        if (!player.level().getBlockState(target).canBeReplaced() || !player.isWithinBlockInteractionRange(against, 1.0)) return null;
        Vec3 face = Vec3.atCenterOf(against).add(facing.getStepX() * 0.5, 0.0, facing.getStepZ() * 0.5);
        return new BlockHitResult(face, facing, against, false);
    }

    // Preview

    /** What a placement of the block in hand on hit would put down: vanilla's state and the one chosen, or null. */
    @Nullable
    public BlockState[] wouldPlace(LocalPlayer player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem item)) return null;
        BlockPlaceContext context = item.updatePlacementContext(new BlockPlaceContext(player, hand, stack, hit));
        if (context == null) return null;
        previewing = true;
        previewVanilla = previewChosen = null;
        try {
            BlockState state = ((BlockItemInvoker) item).playersVersus$getPlacementState(context);
            if (state == null) return null;
            return new BlockState[] {previewVanilla != null ? previewVanilla : state, state};
        } finally {
            previewing = false;
        }
    }
}
