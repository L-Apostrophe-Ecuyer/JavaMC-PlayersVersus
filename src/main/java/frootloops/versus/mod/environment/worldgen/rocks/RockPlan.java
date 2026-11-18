package frootloops.versus.mod.environment.worldgen.rocks;

import java.util.List;

/**
 * A rock formation worked out to the last number: its bodies (each stands on the ground on its own: a tower, a boulder
 * beside it). A {@link RockTemplate} draws one from a seed; a {@link RockModel} turns a body into blocks.
 */
public record RockPlan(List<Body> bodies, RockPalette palette) {

    /**
     * One mass of rock: angular blocks, joined where they meet. Lengths are in blocks; x and z are from the body's middle,
     * which is ({@code dx}, {@code dz}) from the formation's; y is from the ground at the body's middle.
     *
     * @param blocks      the blocks it is made of, in its own coordinates
     * @param blend       how far blocks are joined where they meet, in blocks: a seam is a groove, never a slot
     * @param flare       how much wider its foot is at the ground, curving out over the lowest {@code flareHeight}
     * @param cover       how much of its level tops moss and grass cover (0: bare, 1: about all)
     * @param height      how tall it was made: the cover favours its upper part
     * @param seed        what draws its seams, its tones and its cover
     */
    public record Body(int dx, int dz, List<RockBlock> blocks, double blend, double flare, double flareHeight, double cover, double height, long seed) {

        public Body {
            blocks = List.copyOf(blocks);
        }

        public Body withCover(double cover) {
            return new Body(this.dx, this.dz, this.blocks, this.blend, this.flare, this.flareHeight, cover, this.height, this.seed);
        }

        public Body at(int dx, int dz) {
            return new Body(dx, dz, this.blocks, this.blend, this.flare, this.flareHeight, this.cover, this.height, this.seed);
        }
    }
}
