package frootloops.versus.mod.environment;

import com.mojang.blaze3d.vertex.PoseStack;
import frootloops.versus.mod.environment.blocks.trees.Topple;
import frootloops.versus.mod.environment.blocks.trees.TopplePayload;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.state.FallingBlockRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

/**
 * A felled tree turning over, on the client: the server says when it starts and when it lets go (TopplePayload), and
 * in between the client turns it itself, tick by tick as the server does (Topple), so it moves smoothly whatever the
 * server sends of its blocks' positions. Each of its falling blocks is moved round the hinge and drawn turned over as
 * far as the tree is, between ticks too; let go, it keeps the turn it had until it lands.
 */
@Environment(EnvType.CLIENT)
public final class ClientTopples {

    /** How long a let-go topple is remembered, for its blocks' turn, in ticks. */
    private static final int REMEMBERED = 200;
    /** How long a topple the server never says it let go of is turned before it's dropped, in ticks (out of sight, say). */
    private static final int LONGEST = 400;

    /** A felled tree turning over: how, whether the server has let go of it, and for how long it has turned and been let go. */
    private static final class Swing {
        final Topple topple;
        boolean released;
        int ticks, ticksReleased;

        Swing(Topple topple) {
            this.topple = topple;
        }
    }

    /** One of its falling blocks, and where its centre started. */
    private record Piece(Swing swing, Vec3 start) {}

    private static final Map<Integer, Swing> SWINGS = new HashMap<>();
    private static final Map<Integer, Piece> PIECES = new HashMap<>();
    /** The world they turn in: another one (a new dimension, a new server) forgets them all. */
    private static @Nullable ClientLevel level;

    private ClientTopples() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(TopplePayload.TYPE, (payload, context) -> receive(payload));
        ClientTickEvents.START_CLIENT_TICK.register(ClientTopples::tick);
    }

    private static void receive(TopplePayload payload) {
        if (payload.released()) {
            Swing swing = SWINGS.get(payload.swing());
            if (swing == null || swing.released) return;
            swing.released = true;
            swing.topple.stopAt(payload.angle());
            return;
        }
        // Told again (seen afresh partway through), it turns on from the server's angle now
        Swing swing = new Swing(payload.topple()), was = SWINGS.put(payload.swing(), swing);
        if (was != null) PIECES.values().removeIf(piece -> piece.swing() == was);
        for (int i = 0; i < payload.blocks().length; i++) PIECES.put(payload.blocks()[i], new Piece(swing, payload.start(i)));
    }

    private static void tick(Minecraft client) {
        if (client.level != level) {
            level = client.level;
            SWINGS.clear();
            PIECES.clear();
        }
        if (level == null || client.isPaused()) return;
        for (Swing swing : SWINGS.values()) {
            if (!swing.released) swing.topple.step();
            else swing.ticksReleased++;
            swing.ticks++;
        }
        SWINGS.values().removeIf(ClientTopples::forgotten);
        PIECES.values().removeIf(piece -> forgotten(piece.swing()));
    }

    private static boolean forgotten(Swing swing) {
        return swing.ticksReleased > REMEMBERED || swing.ticks > LONGEST;
    }

    /**
     * Moves a felled tree's falling block round its hinge for this tick, from where it was the tick before.
     *
     * @return whether it did (the block turning with its tree, not falling on its own)
     */
    public static boolean drive(FallingBlockEntity block) {
        Piece piece = PIECES.get(block.getId());
        if (piece == null || piece.swing().released) return false;
        Topple topple = piece.swing().topple;
        Vec3 was = topple.centre(piece.start(), topple.angleBefore()), now = topple.centre(piece.start());
        block.setOldPosAndRot(was.subtract(0.0, 0.5, 0.0), block.getYRot(), block.getXRot());
        block.setPos(now.x, now.y - 0.5, now.z);
        block.setDeltaMovement(topple.velocity(piece.start()));
        return true;
    }

    /** Where a felled tree's falling block is drawn this frame, and how far turned over; any other block, as it is. */
    public static void extract(FallingBlockEntity block, FallingBlockRenderState state, float partialTick) {
        Piece piece = PIECES.get(block.getId());
        ToppleRenderState turn = (ToppleRenderState) state;
        if (piece == null) {
            turn.playersVersus$setTurn(0.0f, null);
            return;
        }
        Topple topple = piece.swing().topple;
        double angle = piece.swing().released ? topple.angle() : Mth.lerp(partialTick, topple.angleBefore(), topple.angle());
        if (!piece.swing().released) {
            Vec3 centre = topple.centre(piece.start(), angle);
            state.x = centre.x;
            state.y = centre.y - 0.5;
            state.z = centre.z;
        }
        turn.playersVersus$setTurn((float) angle, topple.way());
    }

    /** Turns the block about to be drawn over as far as its tree is, about its own centre. */
    public static void turn(FallingBlockRenderState state, PoseStack poseStack) {
        ToppleRenderState turn = (ToppleRenderState) state;
        Direction way = turn.playersVersus$turnWay();
        if (way == null || turn.playersVersus$turn() == 0.0f) return;
        // Turning up toward the way it falls: about the level line across it (up x way)
        poseStack.rotateAround(new Quaternionf().rotationAxis(turn.playersVersus$turn(), way.getStepZ(), 0.0f, -way.getStepX()), 0.0f, 0.5f, 0.0f);
    }
}
