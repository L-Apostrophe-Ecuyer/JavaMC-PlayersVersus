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
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The outline of the block a right-click would place, where it would go, when that's not what vanilla would place
 * there (turned to match a stroke or its neighbours: {@link PlacementRules}) or when it's reached around to (nothing
 * under the crosshair): the cases where the player couldn't otherwise tell what they'll get.
 */
@Environment(EnvType.CLIENT)
public final class PlacementPreview {
    private PlacementPreview() {}

    /** White, a little see-through: unlike vanilla's dark outline of the block under the crosshair. */
    private static final int COLOR = 0xB0FFFFFF;

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(PlacementPreview::submit);
    }

    private record Preview(BlockPos pos, VoxelShape shape) {}

    private static void submit(LevelRenderContext context) {
        if (!VersusSettings.QOL.DO_PLACEMENT_PREVIEW) return;
        Minecraft minecraft = Minecraft.getInstance();
        Preview preview = find(minecraft);
        if (preview == null || preview.shape.isEmpty()) return;
        Vec3 camera = ((LevelRendererAccessor) minecraft.levelRenderer).playersVersus$levelRenderState().cameraRenderState.pos;
        float width = ((GameRendererInvoker) minecraft.gameRenderer).playersVersus$gameRenderState().windowRenderState.appropriateLineWidth;
        PoseStack poseStack = context.poseStack();
        poseStack.pushPose();
        // As vanilla's outline: the shape is drawn from the block's corner, relative to the camera
        poseStack.translate(preview.pos.getX() - camera.x, preview.pos.getY() - camera.y, preview.pos.getZ() - camera.z);
        context.submitNodeCollector().submitShapeOutline(poseStack, preview.shape, RenderTypes.linesTranslucent(), COLOR, width, false);
        poseStack.popPose();
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
            ClientPlacement.Placement placement = ClientPlacement.INSTANCE.wouldPlace(player, hand, blockHit);
            if (placement == null || (!around && placement.chosen() == placement.vanilla())) return null;
            BlockState state = placement.chosen();
            return new Preview(placement.pos(), state.getShape(minecraft.level, placement.pos()));
        }
        return null;
    }
}
