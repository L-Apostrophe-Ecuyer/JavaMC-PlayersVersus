package frootloops.versus.mod.items_and_effects.containers;

import net.minecraft.world.item.DyeColor;
import org.jspecify.annotations.Nullable;

/**
 * The colour of a shulker box's grid: the box's own (the dyes' usual colours; the undyed box's purple), in three shades,
 * the frame around the cells, the cells, and the highlight of the selected one. As opaque RGB (alpha 0xFF).
 */
public final class ShulkerColors {

    /** The undyed shulker box's purple. */
    private static final int UNDYED = 0x966896;
    private static final int OPAQUE = 0xFF000000;
    // A dark box (black's) keeps its cells apart from the frame by these floors, per channel
    private static final int FRAME_FLOOR = 0x10, CELL_FLOOR = 0x2C;

    private ShulkerColors() {
    }

    /** The box's colour as RGB: the dye's, or the undyed box's. */
    public static int of(@Nullable DyeColor dye) {
        if (dye == null) return UNDYED;
        return switch (dye) {
            case WHITE -> 0xF9FFFE;
            case ORANGE -> 0xF9801D;
            case MAGENTA -> 0xC74EBD;
            case LIGHT_BLUE -> 0x3AB3DA;
            case YELLOW -> 0xFED83D;
            case LIME -> 0x80C71F;
            case PINK -> 0xF38BAA;
            case GRAY -> 0x474F52;
            case LIGHT_GRAY -> 0x9D9D97;
            case CYAN -> 0x169C9C;
            case PURPLE -> 0x8932B8;
            case BLUE -> 0x3C44AA;
            case BROWN -> 0x835432;
            case GREEN -> 0x5E7C16;
            case RED -> 0xB02E26;
            case BLACK -> 0x1D1D21;
        };
    }

    /** The frame around the cells: the colour at 40%. */
    public static int frame(int rgb) {
        return OPAQUE | scaled(rgb, 0.4F, FRAME_FLOOR);
    }

    /** The cells: the colour at 65%. */
    public static int cell(int rgb) {
        return OPAQUE | scaled(rgb, 0.65F, CELL_FLOOR);
    }

    /** The selected cell: the colour lightened towards white. */
    public static int highlight(int rgb) {
        int packed = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int channel = rgb >> shift & 0xFF;
            packed |= (channel + Math.round((255 - channel) * 0.6F)) << shift;
        }
        return OPAQUE | packed;
    }

    private static int scaled(int rgb, float factor, int floor) {
        int packed = 0;
        for (int shift = 16; shift >= 0; shift -= 8) packed |= Math.max(floor, Math.round((rgb >> shift & 0xFF) * factor)) << shift;
        return packed;
    }
}
