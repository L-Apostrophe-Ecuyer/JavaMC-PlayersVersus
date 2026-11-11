package frootloops.versus.mod.environment.worldgen.rocks;

import java.util.List;

/**
 * A rock formation worked out to the last number: its bodies (each stands on the ground on its own: a tower, a boulder
 * beside it), and its blocks. A {@link RockTemplate} draws one from a seed; a {@link RockModel} turns a body into blocks.
 */
public record RockPlan(List<Body> bodies, RockPalette palette) {

    /**
     * How a body is broken up at one scale: the cells of a Voronoi diagram, each a block of rock that stands a little
     * out of the body's outline or back into it, with open cracks along some of the borders between them.
     *
     * @param size  a cell's size across, in blocks; 0: not broken at this scale
     * @param tall  a cell's height as a multiple of its size across: over 1 for upright blocks, under 1 for beds
     * @param depth how far a cell stands out of the outline, or back into it, at most
     * @param open  the share of the borders between cells that are open cracks (0 to 1)
     * @param gape  how deep an open crack goes from the surface, in blocks; it is a block wide
     */
    public record Fracture(double size, double tall, double depth, double open, double gape) {
        public static final Fracture NONE = new Fracture(0, 1, 0, 0, 0);
    }

    /**
     * One mass of rock: its lobes fused into an outline, broken into pieces and those into chunks, split into
     * pinnacles. Lengths are in blocks; x and z are from the body's middle, which is ({@code dx}, {@code dz}) from the
     * formation's; y is from the ground at the body's middle.
     *
     * @param warp         how far the outline is pushed in and out over {@code warpScale} blocks: the rock's large bulges
     * @param pieces       the few large blocks the body is broken into, so that it looks like two or three rocks together
     * @param chunks       the small blocks its faces are broken into
     * @param pinnacles    the size across of the columns the body is split into from above (cells of a Voronoi diagram
     *                     over the ground plan), each its own height; 0: one mass
     * @param pinnacleDrop the share of its height a pinnacle is lower than the tallest by, at most
     * @param cleft        half the width of the clefts between pinnacles, at their top
     * @param roughness    block-sized unevenness
     * @param flutes       how deep the grooves are that run down its faces, a block wide: the joints between columns
     *                     {@code fluteScale} blocks across (the cells of a Voronoi diagram over the ground plan), as
     *                     columnar rock is jointed: organ pipes; 0: none
     * @param dusk         how far its own shade reaches over the side turned from the sun and around its foot, in tuff:
     *                     the game draws no shadow for a rock, so a small one's shade is in its stone (0: only its
     *                     cracks, creases and overhangs; 1: a boulder's)
     * @param cover        how much of its level faces plants cover (0: bare, 1: every ledge)
     * @param coverTop     how much more the cover favours the top over the foot
     * @param drape        how many blocks moss hangs down the face from an edge, at most
     * @param vines        the share of the cover's edges with vines hanging from them
     * @param plants       the share of the grass with a plant on it
     * @param slabs        whether slabs smooth the bare rock's upward faces
     * @param skirtWidth   how far from its foot the ground rises toward it, where it does
     * @param skirtHeight  how high the ground rises at its foot
     * @param scree        how many loose stones lie around its foot (0 to 1)
     */
    public record Body(int dx, int dz, List<Lobe> lobes, double warp, double warpScale, Fracture pieces, Fracture chunks,
                       double pinnacles, double pinnacleDrop, double cleft, double roughness, double flutes, double fluteScale,
                       double dusk, double cover, double coverTop, int drape, double vines, double plants, boolean slabs,
                       double skirtWidth, double skirtHeight, double scree, long seed) {

        public Body withCover(double cover) {
            return this.with(cover, null, null, null, null, 1);
        }

        /**
         * This body with the numbers a template gives instead of its shape's own ({@code null}: the shape's); its skirt is
         * {@code skirt} times as wide and as high.
         */
        public Body with(Double cover, Double vines, Double plants, Double scree, Boolean slabs, double skirt) {
            return new Body(this.dx, this.dz, this.lobes, this.warp, this.warpScale, this.pieces, this.chunks, this.pinnacles, this.pinnacleDrop,
                    this.cleft, this.roughness, this.flutes, this.fluteScale, this.dusk, cover == null ? this.cover : cover, this.coverTop, this.drape, vines == null ? this.vines : vines,
                    plants == null ? this.plants : plants, slabs == null ? this.slabs : slabs, this.skirtWidth * skirt, this.skirtHeight * skirt,
                    scree == null ? this.scree : scree, this.seed);
        }

        public Body at(int dx, int dz) {
            return new Body(dx, dz, this.lobes, this.warp, this.warpScale, this.pieces, this.chunks, this.pinnacles, this.pinnacleDrop, this.cleft,
                    this.roughness, this.flutes, this.fluteScale, this.dusk, this.cover, this.coverTop, this.drape, this.vines, this.plants, this.slabs, this.skirtWidth, this.skirtHeight,
                    this.scree, this.seed);
        }
    }

    /**
     * One rounded part of a body's outline.
     * <ul>
     *   <li>A column ({@code blob} false) rises from the ground: its radius at the height {@code t} (0 at the ground, 1 at
     *   its top) is {@code radius * (1 - t^cap)^(1/shoulder)}: a higher {@code cap} keeps the sides upright longer before
     *   the top rounds off. It leans by ({@code leanX}, {@code leanZ}) at its top, bulges by {@code bulge} of its radius
     *   around the height {@code bulgeAt}, and spreads by {@code foot} at the ground (or draws in, if negative). Its top is cut level
     *   {@code plateau} of its height below where it would end: a flat top for plants to stand on.</li>
     *   <li>A blob ({@code blob} true) is a boulder: an ellipsoid {@code height} tall whose middle is {@code centerY} above
     *   the ground (part of it buried).</li>
     * </ul>
     * Either is cut flat by {@code facets} planes up to {@code facetDepth} of its radius deep: the planes between a point
     * and its neighbours, as a Voronoi cell's are. Across the ground it is an ellipse, {@code radius} one way and
     * {@code radius * aspect} the other, turned by {@code rotation}, pulled out of round by {@code lobing} (the share of
     * its radius it swells and narrows by, two and three times around).
     */
    public record Lobe(boolean blob, double x, double z, double height, double centerY, double radius, double aspect, double rotation,
                       double leanX, double leanZ, double leanCurve, double cap, double shoulder, double bulge, double bulgeAt, double foot,
                       double lobing, double plateau, int facets, double facetDepth, long seed) {
    }
}
