package frootloops.versus.mod.environment.Minecarts;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public record Step(Vec3d position, Vec3d movement, float yRot, float xRot, float weight) {
    public static final PacketCodec<ByteBuf, Float> DEGREES_AS_BYTE_PACKET_CODEC = PacketCodecs.BYTE.xmap(Step::byteToDegrees, Step::degreesToByte);
    public static final PacketCodec<ByteBuf, Step> PACKET_CODEC = PacketCodec.tuple(Vec3d.PACKET_CODEC, Step::position, Vec3d.PACKET_CODEC, Step::movement, DEGREES_AS_BYTE_PACKET_CODEC, Step::yRot, DEGREES_AS_BYTE_PACKET_CODEC, Step::xRot, PacketCodecs.FLOAT, Step::weight, Step::new);
    public static Step ZERO = new Step(Vec3d.ZERO, Vec3d.ZERO, 0.0F, 0.0F, 0.0F);

    private static byte degreesToByte(float degrees) {
        return (byte) MathHelper.floor(degrees * 256.0F / 360.0F);
    }

    private static float byteToDegrees(byte b) {
        return (float)b * 360.0F / 256.0F;
    }
}