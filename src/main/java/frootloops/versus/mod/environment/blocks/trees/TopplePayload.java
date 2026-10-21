package frootloops.versus.mod.environment.blocks.trees;

import frootloops.versus.VersusMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Server to client: a felled tree has started turning over on its hinge (Topple), with the falling blocks turning with
 * it and the spots they started from, so clients turn it the same way, tick by tick, and draw each block turned; or it
 * has stopped turning, turned and settled this far (released), its blocks falling on their own from there.
 */
public record TopplePayload(int swing, boolean released, Vec3 hinge, Direction way, double along, double up, double inertia, double drop,
                            double angle, double speed, double settled, double settling, int[] blocks, long[] starts) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TopplePayload> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "topple"));
    public static final StreamCodec<FriendlyByteBuf, TopplePayload> CODEC = StreamCodec.ofMember(TopplePayload::write, TopplePayload::read);

    /** This topple starting, turning these falling blocks, which started at these spots. */
    public static TopplePayload start(int swing, Topple topple, int[] blocks, long[] starts) {
        return new TopplePayload(swing, false, topple.hinge(), topple.way(), topple.along(), topple.up(), topple.inertia(), topple.drop(),
                topple.angle(), topple.speed(), topple.settled(), topple.settling(), blocks, starts);
    }

    /** This topple stopped, turned and settled this far. */
    public static TopplePayload release(int swing, double angle, double settled) {
        return new TopplePayload(swing, true, Vec3.ZERO, Direction.NORTH, 0.0, 0.0, 0.0, 0.0, angle, 0.0, settled, 0.0, new int[0], new long[0]);
    }

    /** The topple this starts, as the server's. */
    public Topple topple() {
        return new Topple(this.hinge, this.way, this.along, this.up, this.inertia, this.drop, this.angle, this.speed, this.settled, this.settling);
    }

    /** Where the centre of the i-th block started. */
    public Vec3 start(int i) {
        return Vec3.atCenterOf(BlockPos.of(this.starts[i]));
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(this.swing);
        buf.writeBoolean(this.released);
        buf.writeDouble(this.angle);
        buf.writeDouble(this.settled);
        if (this.released) return;
        buf.writeDouble(this.hinge.x);
        buf.writeDouble(this.hinge.y);
        buf.writeDouble(this.hinge.z);
        buf.writeByte(this.way.get3DDataValue());
        buf.writeDouble(this.along);
        buf.writeDouble(this.up);
        buf.writeDouble(this.inertia);
        buf.writeDouble(this.drop);
        buf.writeDouble(this.speed);
        buf.writeDouble(this.settling);
        buf.writeVarInt(this.blocks.length);
        for (int i = 0; i < this.blocks.length; i++) {
            buf.writeVarInt(this.blocks[i]);
            buf.writeLong(this.starts[i]);
        }
    }

    private static TopplePayload read(FriendlyByteBuf buf) {
        int swing = buf.readVarInt();
        boolean released = buf.readBoolean();
        double angle = buf.readDouble(), settled = buf.readDouble();
        if (released) return release(swing, angle, settled);
        Vec3 hinge = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Direction way = Direction.from3DDataValue(buf.readByte());
        double along = buf.readDouble(), up = buf.readDouble(), inertia = buf.readDouble(), drop = buf.readDouble();
        double speed = buf.readDouble(), settling = buf.readDouble();
        int count = buf.readVarInt();
        int[] blocks = new int[count];
        long[] starts = new long[count];
        for (int i = 0; i < count; i++) {
            blocks[i] = buf.readVarInt();
            starts[i] = buf.readLong();
        }
        return new TopplePayload(swing, false, hinge, way, along, up, inertia, drop, angle, speed, settled, settling, blocks, starts);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
