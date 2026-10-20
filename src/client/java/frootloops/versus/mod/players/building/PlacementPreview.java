package frootloops.versus.mod.players.building;

import com.mojang.blaze3d.vertex.PoseStack;
import frootloops.versus.VersusSettings;
import frootloops.versus.mixin.client.players.GameRendererInvoker;
import frootloops.versus.mixin.client.players.LevelRendererAccessor;
import frootloops.versus.mod.players.HandSelection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The outline of the block a right-click would place, where it would go, when that's not what vanilla would place
 * there (turned to match a stroke, the last placement or its neighbours: {@link PlacementRules}) or when it's reached
 * around to (nothing under the crosshair): the cases where the player couldn't otherwise tell what they'll get. A full
 * block shows which way it turns: a rod along a log's axis, a plate on the front of a furnace, an observer, a piston.
 */
@Environment(EnvType.CLIENT)
public final class PlacementPreview {
    private PlacementPreview() {}

    /** White, a little see-through: unlike vanilla's dark outline of the block under the crosshair. */
    private static final int COLOR = 0xB0FFFFFF;

    private static final VoxelShape[] ROD = {
            Shapes.box(0.0, 0.4375, 0.4375, 1.0, 0.5625, 0.5625),
            Shapes.box(0.4375, 0.0, 0.4375, 0.5625, 1.0, 0.5625),
            Shapes.box(0.4375, 0.4375, 0.0, 0.5625, 0.5625, 1.0)};

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(PlacementPreview::submit);
    }

    private record Preview(BlockPos pos, BlockState state, VoxelShape shape) {}

    private static void submit(LevelRenderContext context) {
        if (!VersusSettings.QOL.DO_PLACEMENT_PREVIEW) return;
        Minecraft minecraft = Minecraft.getInstance();
        Preview preview = find(minecraft);
        if (preview == null || preview.shape.isEmpty()) return;
        Vec3 camera = ((LevelRendererAccessor) minecraft.levelRenderer).playersVersus$levelRenderState().cameraRenderState.pos;
        float width = ((GameRendererInvoker) minecraft.gameRenderer).playersVersus$gameRenderState().windowRenderState.appropriateLineWidth;
        PoseStack poseStack = context.poseStack();
        SubmitNodeCollector collector = context.submitNodeCollector();
        poseStack.pushPose();
        // As vanilla's outline: the shape is drawn from the block's corner, relative to the camera
        poseStack.translate(preview.pos.getX() - camera.x, preview.pos.getY() - camera.y, preview.pos.getZ() - camera.z);
        // The see-through lines vanilla's translucent gizmos use: 26.4's linesTranslucent has no pipelines for improved
        // transparency, and drawing it with that setting on crashes the game
        collector.submitShapeOutline(poseStack, preview.shape, RenderTypes.linesTranslucentNoDepthWrite(), COLOR, width, false);
        VoxelShape mark = mark(preview.state, preview.shape);
        if (mark != null) collector.submitShapeOutline(poseStack, mark, RenderTypes.linesTranslucentNoDepthWrite(), COLOR, width, false);
        poseStack.popPose();
    }

    /** What shows the turn of a full block, which its outline alone doesn't; null for any other. */
    @Nullable
    private static VoxelShape mark(BlockState state, VoxelShape shape) {
        if (!Block.isShapeFullBlock(shape)) return null;
        if (state.hasProperty(BlockStateProperties.AXIS)) return ROD[state.getValue(BlockStateProperties.AXIS).ordinal()];
        Direction front = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                : state.hasProperty(BlockStateProperties.FACING) ? state.getValue(BlockStateProperties.FACING) : null;
        if (front == null) return null;
        // A plate just out of the front face
        double minX = 0.3125, minY = 0.3125, minZ = 0.3125, maxX = 0.6875, maxY = 0.6875, maxZ = 0.6875;
        switch (front) {
            case NORTH -> { minZ = -0.0625; maxZ = 0.0; }
            case SOUTH -> { minZ = 1.0; maxZ = 1.0625; }
            case WEST -> { minX = -0.0625; maxX = 0.0; }
            case EAST -> { minX = 1.0; maxX = 1.0625; }
            case DOWN -> { minY = -0.0625; maxY = 0.0; }
            case UP -> { minY = 1.0; maxY = 1.0625; }
        }
        return Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** What to outline this frame, if anything. */
    @Nullable
    private static Preview find(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.gameMode == null || player.isSpectator() || player.isUsingItem()) return null;
        HitResult hit = minecraft.hitResult;
        if (hit == null || hit.getType() == HitResult.Type.ENTITY) return null;
        HandSelection.Plan plan = HandSelection.plan(player, minecraft.level, hit);
        if (!plan.world()) return null;
        // The hand that would place: the first, in the order a right-click tries them, holding a block
        for (InteractionHand hand : plan.hands()) {
            ItemStack stack = player.getItemInHand(hand);
            if (HandSelection.onCooldown(player, hand) || !(stack.getItem() instanceof BlockItem)) continue;
            boolean around = hit.getType() == HitResult.Type.MISS;
            BlockHitResult blockHit = around ? ClientPlacement.INSTANCE.reachAround(player, hand) : (BlockHitResult) hit;
            if (blockHit == null) continue; // Nothing to reach around to: the next hand's turn, as for a right-click
            if (ClientPlacement.INSTANCE.refuses(player, hand, blockHit)) return null;
            ClientPlacement.Choice choice = ClientPlacement.INSTANCE.preview(player, hand, blockHit);
            if (choice == null || (!around && choice.chosen() == choice.vanilla())) return null;
            BlockState state = choice.chosen();
            return new Preview(choice.pos(), state, state.getShape(minecraft.level, choice.pos()));
        }
        return null;
    }
}
