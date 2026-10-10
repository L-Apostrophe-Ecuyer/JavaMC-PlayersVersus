"""The Players Versus overworld's density functions and noise settings in Minecraft 26.3's format.

26.3 compiles density functions itself, into float samplers that work on whole volumes, so the terrain is data again:
the Java kernels of plan phase 3 (PvTerrain, PvFinalDensity, PvEntrances, PvNoodle, PvDepth, PvHighRiver and the
corridor bias) are written here as trees of vanilla types, with the same formulas and the same order of operations,
and this script writes them out. The aquifer stays Java: its two density-function types only name its inputs.

Constants shared with the Java side come from PvWorldgenConstants.java, so there is one place to change them.

Two things differ from the 1.21.10 JSON on purpose:
  * 1.21.10's minecraft:y was a clamped gradient computed in doubles, which put a few heights a hair off (y 32 read
    31.9999999999995); 26.3's is exact. Every band on y is written with half-integer bounds that give the heights
    1.21.10 gave (band()).
  * An infinite "no cut" value becomes 64, vanilla's own for noodles: nothing the final density takes the minimum
    with ever comes near it.

python3 pv_density.py            writes the files
python3 pv_density.py --check    compares the files with what it would write
"""
import json
import math
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
DATA = ROOT / "src/main/resources/data/players-versus/worldgen"
CONSTANTS_JAVA = ROOT / "src/main/java/frootloops/versus/mod/environment/worldgen/PvWorldgenConstants.java"
NS = "players-versus"
CELL_XZ, CELL_Y = 4, 8
NO_CUT = 64.0
# The smallest positive float: v < this is v <= 0 for any float v.
MIN_POSITIVE = 1.4e-45


def constants():
    """The int and double constants of PvWorldgenConstants, evaluated in order (later ones may use earlier ones)."""
    values = {}
    text = CONSTANTS_JAVA.read_text()
    for kind, name, expr in re.findall(r'public static final (int|double) (\w+) = ([^;]+);', text):
        value = eval(expr, {}, dict(values))
        values[name] = int(value) if kind == "int" else float(value)
    return values


C = constants()


# --- 1.21.10's minecraft:y and bands on it -------------------------------------------------------------------------

def y_1_21(y):
    """1.21.10's minecraft:y at a block: Mth.clampedMap(y, -4064, 4062, -4064, 4062) in doubles."""
    t = (y - -4064.0) / (4062.0 - -4064.0)
    return -4064.0 if t < 0 else 4062.0 if t > 1 else -4064.0 + t * (4062.0 - -4064.0)


def band(lo, hi):
    """The bounds of a range_choice on 26.3's (exact) minecraft:y that hold the heights 1.21.10's [lo, hi) held."""
    members = [y for y in range(-2048, 2048) if lo <= y_1_21(float(y)) < hi]
    assert members == list(range(members[0], members[-1] + 1)), (lo, hi)
    return members[0] - 0.5, members[-1] + 0.5


# --- vanilla types --------------------------------------------------------------------------------------------------

def mc(name):
    return "minecraft:" + name


def pv(name):
    return NS + ":" + name


def binary(kind, left, right):
    return {"type": mc(kind), "left": left, "right": right}


def add(a, b): return binary("add", a, b)
def sub(a, b): return binary("sub", a, b)
def mul(a, b): return binary("mul", a, b)
def div(a, b): return binary("div", a, b)
def min_(a, b): return binary("min", a, b)
def max_(a, b): return binary("max", a, b)
def unary(kind, x): return {"type": mc(kind), "input": x}
def abs_(x): return unary("abs", x)
def square(x): return unary("square", x)
def half_negative(x): return unary("half_negative", x)
def quarter_negative(x): return unary("quarter_negative", x)
def squeeze(x): return unary("squeeze", x)
def cache(x): return unary("cache", x)
def blend_density(x): return unary("blend_density", x)


def clamp(x, lo, hi):
    return {"type": mc("clamp"), "input": x, "min": float(lo), "max": float(hi)}


def gradient(from_y, to_y, from_value, to_value):
    """1.21.10's y_clamped_gradient: clamped outside the range."""
    return {"type": mc("gradient"), "axis": "y", "from_coordinate": from_y, "from_value": float(from_value),
            "to_coordinate": to_y, "to_value": float(to_value)}


def range_choice(x, lo, hi, in_range, out_of_range):
    return {"type": mc("range_choice"), "input": x, "min_inclusive": float(lo), "max_exclusive": float(hi),
            "when_in_range": in_range, "when_out_of_range": out_of_range}


def y_band(lo, hi, in_range, out_of_range):
    """A range_choice on minecraft:y that picks 1.21.10's heights for [lo, hi)."""
    low, high = band(lo, hi)
    return range_choice(mc("y"), low, high, in_range, out_of_range)


def lerp(alpha, first, second):
    """first + alpha * (second - first)."""
    return {"type": mc("lerp"), "alpha": alpha, "first": first, "second": second}


def interpolated(x):
    return {"type": mc("interpolated"), "cell_size_xz": CELL_XZ, "cell_size_y": CELL_Y, "input": x}


def slice_y(y, x):
    return {"type": mc("slice"), "axis": "y", "coordinate": y, "input": x}


def noise(name, xz, y, shift=False):
    out = {"type": mc("noise"), "noise": name, "xz_scale": float(xz), "y_scale": float(y)}
    if shift:
        out["shift_x"], out["shift_z"] = mc("shift_x"), mc("shift_z")
    return out


def spaghetti(noise_name):
    """1.21.10's weird_scaled_sampler (rarity type_1) over a spaghetti noise, as vanilla 26.3 writes it."""
    rarities = [0.75, 1.0, 1.5, 2.0]
    return abs_({"type": mc("interval_select"),
                 "functions": [mul(noise(mc(noise_name), 1.0 / r, 1.0 / r), r) for r in rarities],
                 "input": cache(noise(mc("spaghetti_3d_rarity"), 2.0, 1.0)),
                 "thresholds": [-0.5, 0.0, 0.5]})


# --- the functions (each mirrors the Java kernel it replaces) ------------------------------------------------------

REF_OFFSET, REF_FACTOR, REF_JAGGEDNESS = mc("overworld/offset"), mc("overworld/factor"), mc("overworld/jaggedness")
REF_RIDGES, REF_CONTINENTS = mc("overworld/ridges"), mc("overworld/continents")
REF_BASE_3D, REF_ROUGHNESS = mc("overworld/base_3d_noise"), mc("overworld/caves/spaghetti_roughness_function")
REF_DEPTH, REF_ENTRANCES = pv("overworld/depth"), pv("overworld/caves/entrances")
REF_SLOPED_CHEESE, REF_TERRAIN = pv("overworld/sloped_cheese"), pv("overworld/terrain")
REF_TOGGLE, REF_THICKNESS = pv("overworld/caves/noodle_toggle"), pv("overworld/caves/noodle_thickness")
REF_RIDGE_A, REF_RIDGE_B = pv("overworld/caves/noodle_ridge_a"), pv("overworld/caves/noodle_ridge_b")
REF_CORRIDOR_NOODLE, REF_VALLEY = pv("overworld/caves/corridor_noodle"), pv("overworld/high_river/valley")
REF_CORRIDOR_ENTRANCES, REF_FLOODED_CORRIDORS = pv("overworld/caves/corridor_entrances"), pv("overworld/caves/flooded_corridors")
REF_ACROSS = pv("overworld/high_river/across")
REF_UPPER_VALLEY, REF_UPPER_ACROSS = pv("overworld/high_river/upper_valley"), pv("overworld/high_river/upper_across")
REF_BANK, REF_UPPER_BANK = pv("overworld/high_river/bank"), pv("overworld/high_river/upper_bank")
REF_DRY_PATHS = pv("overworld/caves/dry_paths")
REF_BASIN_LEVEL, REF_BASIN_FLOOR = pv("overworld/caves/basin_level"), pv("overworld/caves/basin_floor")


def depth():
    """PvDepth / TerrainFormulas.depth: vanilla's depth, lowered along rivers (|R| < 0.2) in y 54..127."""
    ridge_scaled = mul(REF_RIDGES, 6.75)
    river = min_(0.0, add(add(gradient(54, 74, 0.0, -1.0), gradient(56, 120, 0.0, 1.0)), square(ridge_scaled)))
    river_depth = y_band(54, 128, range_choice(REF_RIDGES, -0.2, 0.2, river, 0.0), 0.0)
    return add(add(gradient(-64, 320, 1.5, -1.5), REF_OFFSET), river_depth)


def river_carver():
    """TerrainFormulas.riverCarver: 1 away from rivers; along one (|R| < 0.22) from y 48 up, lower."""
    scaled = mul(REF_RIDGES, 7.0)
    channel = add(add(gradient(50, 74, 0.8, -1.2), add(gradient(68, 90, 0.0, 0.3), gradient(68, 128, 0.0, 0.89))), square(scaled))
    return y_band(48, 256, range_choice(REF_RIDGES, -0.22, 0.22, channel, 1.0), 1.0)


def sloped_cheese():
    """PvTerrain.slopedCheese: depth with jagged peaks, scaled by the factor, plus the 3D base noise; rivers cut in."""
    jagged = cache(noise(mc("jagged"), 1500.0, 0.0))
    with_peaks = add(REF_DEPTH, mul(REF_JAGGEDNESS, half_negative(jagged)))
    shaped = mul(with_peaks, REF_FACTOR)
    return cache(min_(river_carver(), add(mul(quarter_negative(shaped), 4.0), REF_BASE_3D)))


def terrain():
    """PvTerrain: the terrain at a cell corner, faded to fixed values at the world's bottom and top. Pillars stand in
    every cave: below the surface layer in the cheese caves, entrances and spaghetti; in the surface layer (sloped
    cheese under 1.5625) in the entrances, scaled with them (5 times), and under the min with the sloped cheese, so
    never above the ground."""
    cheese = add(mul(square(noise(mc("cave_layer"), 1.0, 8.0)), 4.0),
                 add(clamp(add(noise(mc("cave_cheese"), 1.0, 0.6666666666666666), 0.27), -1.0, 1.0),
                     clamp(add(mul(REF_SLOPED_CHEESE, -0.64), 1.5), 0.0, 0.5)))
    with_spaghetti = min_(min_(cheese, REF_ENTRANCES), add(1.0, REF_ROUGHNESS))
    pillars = min_(0.3, max_(-0.02, add(mul(max_(0.0, add(noise(mc("pillar"), 20.0, 0.5), -0.15)), 0.7), -0.2)))
    standing = range_choice(pillars, -1000000.0, 0.03, -1000000.0, pillars)
    underground = max_(with_spaghetti, standing)
    surface_layer = min_(REF_SLOPED_CHEESE, mul(max_(REF_ENTRANCES, standing), 5.0))
    caves = range_choice(REF_SLOPED_CHEESE, -1000000.0, 1.5625, surface_layer, underground)
    return lerp(gradient(-64, -40, 0.0, 1.0), 0.1171875, lerp(gradient(240, 256, 1.0, 0.0), -0.078125, caves))


def entrances():
    """PvEntrances: cave entrances and spaghetti tunnels, plus the ramen caves in y 0..43. The height terms (lower is
    more caves): fewer near the surface, most in y 28..38, few in y -16..18 but the basins' lakes, more again around
    y -40. A small rise centred on y 24 (0.03, gone by y 18 and 30) thins the caves there, just over the lakes."""
    shape = mul(mul(gradient(-4, 8, 0.0, 1.0), gradient(8, 16, 1.4, 1.0)), gradient(16, 44, 1.2, 0.0))
    carved = mul(shape, add(add(mul(noise(mc("noodle_ridge_a"), 4.0, 2.0), 0.08), -0.2), abs_(noise(mc("noodle"), 3.0, 3.0))))
    ramen = y_band(0, 44, min_(0.0, mul(add(carved, 0.1), 2.0)), 0.0)
    around_y24 = add(gradient(30, 24, 0.0, 0.03), gradient(24, 18, 0.0, -0.03))
    heights = add(add(gradient(96, 72, 0.15, 0.0), gradient(66, 56, -0.1, 0.025)),
                  add(gradient(48, 38, 0.0, -0.155), add(add(gradient(28, 18, 0.0, 0.265), around_y24),
                                                          add(gradient(-16, -40, 0.0, -0.23), gradient(-40, -60, 0.0, 0.245)))))
    base = add(mul(min_(REF_CONTINENTS, 0.1), -0.1), heights)
    entrance = add(add(noise(mc("cave_entrance"), 0.8, 0.75), 0.37), gradient(-10, 30, 0.3, 0.0))
    spaghetti_sum = add(max_(spaghetti("spaghetti_3d_1"), spaghetti("spaghetti_3d_2")),
                        add(mul(noise(mc("spaghetti_3d_thickness"), 1.0, 1.0), -0.011499999999999996), -0.0765))
    tunnels = add(REF_ROUGHNESS, clamp(spaghetti_sum, -1.0, 1.0))
    return cache(add(ramen, add(base, min_(entrance, tunnels))))


def noodle_input(when_in_range, when_out_of_range):
    """A noodle input without its interpolation, in y -60..320: the aquifer's basins read it at cell corners."""
    return y_band(-60, 321, when_in_range, when_out_of_range)


def noodle_bias():
    """PvNoodle.bias: fewer noodles near the surface and in the deep layers."""
    return add(gradient(96, 56, -0.05, 0.08), add(gradient(56, 40, 0.0, -0.1), add(gradient(32, 20, 0.0, 0.1),
                                                                                     add(gradient(-8, -32, 0.0, -0.3), gradient(-52, -64, 0.0, 0.35)))))


def tunnel(toggle, thickness, ridge_a, ridge_b):
    """PvNoodle.tunnel: 64 (solid) where the toggle is off, else the thickness plus the larger ridge."""
    return range_choice(toggle, -1000000.0, -0.2, 64.0, add(thickness, mul(max_(abs_(ridge_a), abs_(ridge_b)), 1.5)))


def noodle():
    """PvNoodle, for the aquifer's basins: the bias plus the tunnel, from the inputs at the block (no interpolation)."""
    return add(noodle_bias(), tunnel(REF_TOGGLE, REF_THICKNESS, REF_RIDGE_A, REF_RIDGE_B))


def path_bias():
    """The noodle's height bias with dry paths: at most DRY_NOODLE_BIAS in y DRY_NOODLE_MIN_Y..DRY_NOODLE_MAX_Y, where
    the bias otherwise keeps noodles out, so some lead down through those layers. It meets the bias at the band's ends,
    where that is lower already."""
    low, high = band(C["DRY_NOODLE_MIN_Y"], C["DRY_NOODLE_MAX_Y"] + 1)
    return range_choice(mc("y"), low, high, min_(noodle_bias(), C["DRY_NOODLE_BIAS"]), noodle_bias())


def corridor_entrances():
    """The entrance value the corridors' zone and the dry noodles are decided by, interpolated, over the dry noodles'
    heights (1 elsewhere): the final density's noodle and the flooded corridors read the same one. It reaches the cell
    corners at y -16 and 32; the corridors' layers (y -3..23) only read the corners from y -8 to 24."""
    return interpolated(y_band(C["DRY_NOODLE_MIN_Y"], C["DRY_NOODLE_MAX_Y"] + 1, REF_ENTRANCES, 1.0))


def corridor_noodle():
    """The final density's noodle (PvFinalDensity): the tunnel from interpolated inputs, with the corridors' bias
    (PvNoodle.corridorBias) in the basin layers, y -3..23: where the entrance value says a flooded cave is near (below
    CORRIDOR_ENTRANCES), the bias moves to CORRIDOR_BIAS, and on to CORRIDOR_FLARE_BIAS nearer the caves. In those
    layers the dry paths' bias only holds away from the entrance caves (from CORRIDOR_ENTRANCES up, over the same
    taper), so the noodles it adds rarely meet the flooded ones; above and below them it holds everywhere in its
    heights, so the paths join the caves there."""
    entrances_value = REF_CORRIDOR_ENTRANCES
    share = clamp(mul(sub(C["CORRIDOR_ENTRANCES"], entrances_value), C["CORRIDOR_ZONE_SCALE"]), 0.0, 1.0)
    dry = clamp(mul(sub(entrances_value, C["CORRIDOR_ENTRANCES"]), C["CORRIDOR_ZONE_SCALE"]), 0.0, 1.0)
    flare = clamp(mul(sub(C["CORRIDOR_FLARE_FROM"], entrances_value), C["CORRIDOR_FLARE_SCALE"]), 0.0, 1.0)
    target = lerp(flare, C["CORRIDOR_BIAS"], C["CORRIDOR_FLARE_BIAS"])
    away = lerp(dry, noodle_bias(), path_bias())
    layers = band(C["BASIN_MIN_Y"] + 1, C["CORRIDOR_MAX_Y"])
    bias = range_choice(mc("y"), layers[0], layers[1], lerp(share, away, target), path_bias())
    return add(bias, tunnel(interpolated(REF_TOGGLE), interpolated(REF_THICKNESS), interpolated(REF_RIDGE_A), interpolated(REF_RIDGE_B)))


def dry_paths():
    """What the aquifer keeps dry in the basin layers: the final density's noodle outside the corridors' zone (the
    entrance value from CORRIDOR_ENTRANCES up), NO_CUT inside it. Within DRY_PATH_SHELL of where it opens a block the
    basins place no water, so the dry noodles lead down through them."""
    return range_choice(REF_CORRIDOR_ENTRANCES, C["CORRIDOR_ENTRANCES"], 1000000.0, REF_CORRIDOR_NOODLE, NO_CUT)


def basin_level():
    """The basins' level by column (PvAquiferRules): their water, barriers and flooded corridors only reach up to it.
    BASIN_LEVEL_DRY, the basin layers' bottom, where the broad noise players-versus:cave_basins is below
    BASIN_LEVEL_DRY_BELOW, so the caves there are dry from y 32 down; rising to BASIN_LEVEL_FULL where it reaches
    BASIN_LEVEL_FULL_ABOVE, so between them the lakes stand at different heights."""
    lo, hi = C["BASIN_LEVEL_DRY_BELOW"], C["BASIN_LEVEL_FULL_ABOVE"]
    share = clamp(div(sub(noise(pv("cave_basins"), 1.0, 0.0), lo), hi - lo), 0.0, 1.0)
    return add(float(C["BASIN_LEVEL_DRY"]), mul(share, float(C["BASIN_LEVEL_FULL"] - C["BASIN_LEVEL_DRY"])))


def basin_floor():
    """The basins' floor by column (PvAquiferRules): their water stays above it, so the stone that holds a lake where
    its cave goes on down is rough: BASIN_FLOOR_MID plus BASIN_FLOOR_BUMPS blocks per unit of the surface noise (flat in
    y), within BASIN_FLOOR_Y..2 x BASIN_FLOOR_MID."""
    bumps = add(float(C["BASIN_FLOOR_MID"]), mul(noise(mc("surface"), 2.0, 0.0), C["BASIN_FLOOR_BUMPS"]))
    return clamp(bumps, float(C["BASIN_FLOOR_Y"]), 2.0 * C["BASIN_FLOOR_MID"])


def flooded_corridors():
    """What the aquifer floods in the basin layers: the final density's noodle inside the corridors' zone (the
    entrance value below CORRIDOR_ENTRANCES), NO_CUT outside it, so the dry paths stay dry. Where this is at most 0 the
    final density's noodle has opened the block."""
    return range_choice(REF_CORRIDOR_ENTRANCES, -1000000.0, C["CORRIDOR_ENTRANCES"], REF_CORRIDOR_NOODLE, NO_CUT)


# The high river's two layers on one path (the noise's zero line): the river at HIGH_RIVER_Y, and a thinner one at
# HIGH_RIVER_UPPER_Y. Where the ground climbs from one to the other, the lower one cuts a gorge that the upper one ends
# at and falls into.
HIGH_RIVER = {"y": C["HIGH_RIVER_Y"], "bed": C["HIGH_RIVER_BED"], "half_width": C["HIGH_RIVER_HALF_WIDTH"],
              "widening": C["HIGH_RIVER_WIDENING"], "flare": C["HIGH_RIVER_FLARE_WIDENING"], "closed": C["HIGH_RIVER_CLOSED"],
              "gorge": C["HIGH_RIVER_GORGE_WIDTH"], "bank_bottom": C["HIGH_RIVER_BANK_MIN_Y"]}
HIGH_RIVER_UPPER = {"y": C["HIGH_RIVER_UPPER_Y"], "bed": C["HIGH_RIVER_UPPER_BED"], "half_width": C["HIGH_RIVER_UPPER_HALF_WIDTH"],
                    "widening": C["HIGH_RIVER_UPPER_WIDENING"], "flare": C["HIGH_RIVER_UPPER_FLARE_WIDENING"],
                    "closed": C["HIGH_RIVER_UPPER_CLOSED"], "bank_bottom": C["HIGH_RIVER_UPPER_BANK_MIN_Y"]}
# The banks' value where they make ground, and where they don't (the final density takes the maximum with them).
BANK_SOLID, NO_FILL = 1.0, -64.0


def high_river_channel():
    """The river's noise, interpolated on the terrain's cells: 0 in the middle of the river."""
    return interpolated(pv("overworld/high_river"))


def high_river_activity(layer):
    """How open a layer is, from the depth at its surface's height: nothing up to HIGH_RIVER_MIN_DEPTH (where the
    ground's nominal surface isn't above its water, so it would only run on the ground or over it, not in it), widening
    to 1 at HIGH_RIVER_WIDE_DEPTH, 1 up to HIGH_RIVER_FULL_DEPTH, then narrowing into higher ground. A layer with a gorge
    narrows to that share of its width at its closing depth and stops there, so the gorge's head is a wall; the others
    narrow to nothing."""
    depth_at_surface = interpolated(slice_y(layer["y"], REF_DEPTH))
    lowest, wide, full, closed = C["HIGH_RIVER_MIN_DEPTH"], C["HIGH_RIVER_WIDE_DEPTH"], C["HIGH_RIVER_FULL_DEPTH"], layer["closed"]
    widening = clamp(div(sub(depth_at_surface, lowest), wide - lowest), 0.0, 1.0)
    if "gorge" not in layer:
        return min_(widening, clamp(div(sub(closed, depth_at_surface), closed - full), 0.0, 1.0))
    gorge = layer["gorge"]
    narrowing = clamp(add(1.0, mul(sub(depth_at_surface, full), -(1.0 - gorge) / (closed - full))), gorge, 1.0)
    return range_choice(depth_at_surface, -1000000.0, closed, min_(widening, narrowing), 0.0)


def high_river_flare(layer, depth):
    """How far a layer's walls have leaned out by a depth under the ground's nominal surface: nothing from
    HIGH_RIVER_FLARE_DEPTH down, then leaning out more the higher they get (the square of the height into that depth, so
    the lean grows smoothly to the layer's flare per block at the nominal surface), and by the full flare per block
    above it."""
    full = C["HIGH_RIVER_FLARE_DEPTH"]
    into = clamp(sub(full, depth), 0.0, full)
    curve = mul(square(into), layer["flare"] / (2.0 * full * C["DEPTH_PER_BLOCK"]))
    above = mul(max_(0.0, mul(depth, -1.0)), layer["flare"] / C["DEPTH_PER_BLOCK"])
    return add(curve, above)


def high_river_full_half_width(layer):
    """A layer's valley's half width by height at full activity: 0 at its bed's bottom less one, its half width at its
    surface, and wider above it by its widening per block, plus its walls' flare near the ground's nominal surface (from
    the depth at its surface and the height above it, the depth at each height), as far as it reaches past the flare at
    the water: so the walls rise steeply from the water where the ground stands high and open out at the rim."""
    y_surface, bed, top = layer["y"], layer["bed"], C["HIGH_RIVER_VALLEY_MAX_Y"]
    depth_at_surface = interpolated(slice_y(y_surface, REF_DEPTH))
    depth_here = sub(depth_at_surface, gradient(y_surface, top, 0.0, (top - y_surface) * C["DEPTH_PER_BLOCK"]))
    flare = max_(0.0, sub(high_river_flare(layer, depth_here), high_river_flare(layer, depth_at_surface)))
    return add(add(gradient(y_surface - bed - 1, y_surface, 0.0, layer["half_width"]),
                   gradient(y_surface, top, 0.0, (top - y_surface) * layer["widening"])), flare)


def high_river_across(layer):
    """How far a block is inside a layer's valley across it: its channel's distance from the river's middle, less the
    valley's half width there times the layer's activity; negative inside. The inputs are 2D and interpolated on the
    terrain's cells."""
    return cache(sub(abs_(high_river_channel()), mul(high_river_activity(layer), high_river_full_half_width(layer))))


def below_layer_at_surface(layer, below):
    """The lower layer's across at this layer's surface: the same terms as its own, its half width taken at that height."""
    return sub(abs_(high_river_channel()), mul(high_river_activity(below), slice_y(layer["y"], high_river_full_half_width(below))))


def high_river_bank(layer, below=None):
    """The ground a layer runs in where the terrain leaves it open (a dip, a cave's mouth), so its water never stands
    over open ground on a wall: from its surface down to its bank's bottom, solid within its half width plus
    HIGH_RIVER_BANK_MARGIN at its surface, and wider by HIGH_RIVER_BANK_SLOPE per block down, so the deeper a bank
    reaches, the further it slopes out; the margin and the slope shrink with a river under half its full width, so a
    narrowing river's tip has no more bank than water. Only where its water runs (its activity above 0), and for a layer
    over another not inside the lower one's valley; NO_FILL elsewhere. The valleys' cut comes after it in the final
    density, so the river's own channel stays open."""
    y_surface, bottom = layer["y"], layer["bank_bottom"]
    margin, slope = C["HIGH_RIVER_BANK_MARGIN"], C["HIGH_RIVER_BANK_SLOPE"]
    activity = high_river_activity(layer)
    reach = mul(clamp(mul(activity, 2.0), 0.0, 1.0), gradient(y_surface, bottom, margin, margin + (y_surface - bottom) * slope))
    half_width = add(mul(activity, layer["half_width"]), reach)
    inside = range_choice(sub(half_width, abs_(high_river_channel())), MIN_POSITIVE, 1000000.0, BANK_SOLID, NO_FILL)
    bank = range_choice(activity, MIN_POSITIVE, 1000000.0, inside, NO_FILL)
    if below is not None:
        bank = range_choice(below_layer_at_surface(layer, below), -1000000.0, 0.0, NO_FILL, bank)
    return range_choice(mc("y"), bottom - 0.5, y_surface + 0.5, bank, NO_FILL)


def high_river_valley(layer, across, below=None):
    """A layer's valley, negative where it opens a block, NO_CUT elsewhere: in y from its bed's bottom up to
    HIGH_RIVER_VALLEY_MAX_Y, inside its width (across), which is nothing where the ground's nominal surface isn't above
    its water (high_river_activity), so it only runs cut into the ground, and in the ground its bank gives it where the
    terrain dips. A layer over another (below) opens nothing at and under its surface inside the lower one's valley at
    its surface, so it ends at the lower one's gorge and falls into it. The aquifer puts water where this is negative at
    or under the surface."""
    y_surface, bed, top = layer["y"], layer["bed"], C["HIGH_RIVER_VALLEY_MAX_Y"]
    opened = range_choice(across, -1000000.0, 0.0, across, NO_CUT)
    in_ground = opened
    if below is not None:
        in_ground = range_choice(below_layer_at_surface(layer, below), -1000000.0, 0.0, NO_CUT, in_ground)
    in_valley = range_choice(mc("y"), y_surface - bed - 0.5, y_surface + 0.5, in_ground, opened)
    return range_choice(mc("y"), y_surface - bed - 0.5, top - 0.5, in_valley, NO_CUT)


def final_density():
    """PvFinalDensity: the interpolated terrain, scaled and squeezed, cut by the noodles, filled by the high river's
    banks and cut by its two valleys; plus the beardifier, which 26.3 names in the data (1.21.10 added it in code)."""
    terrain_interpolated = squeeze(interpolated(mul(blend_density(REF_TERRAIN), 0.64)))
    banks = max_(REF_BANK, REF_UPPER_BANK)
    valleys = min_(REF_VALLEY, REF_UPPER_VALLEY)
    return add(min_(max_(min_(terrain_interpolated, REF_CORRIDOR_NOODLE), banks), valleys), {"type": mc("beardifier")})


# --- ore veins (PvOreVeins, as 26.3's ore_vein material rules) -----------------------------------------------------
#
# 26.3's ore_vein rule places a vein block where its density is above 0, with that density as the chance, then ore
# where a draw is below its richness and the gap is below 0, raw ore blocks by their chance, and the filler otherwise:
# PvOreVeins' order of checks and draws. Players Versus' veins: a lower threshold (0.3, vanilla 0.4), a higher chance
# (0.85, vanilla 0.7), copper in terracotta at y 32..96 and iron in tuff at y -8..36 (CustomWorldgen.VeinType).

VEINS = {
    # name: (ore, raw ore block, filler, min y, max y, ore chance at full strength, raw ore block chance, sign of toggle)
    "copper": ("copper_ore", "raw_copper_block", "terracotta", 32, 96, 0.6, 0.25, 1.0),
    "iron": ("deepslate_iron_ore", "raw_iron_block", "tuff", -8, 36, 0.35, 0.08, -1.0),
}
VEIN_THRESHOLD, VEIN_CHANCE, VEIN_MIN_ORE_CHANCE, VEIN_FULL_STRENGTH = 0.3, 0.85, 0.15, 0.6
REF_VEIN_TOGGLE, REF_VEIN_MASK = pv("overworld/ore_vein/toggle"), pv("overworld/ore_vein/mask")


def vein_toggle():
    """1.21.10's vein_toggle: the veininess noise in y -60..50, 0 elsewhere."""
    return cache(interpolated(y_band(-60, 51, noise(mc("ore_veininess"), 1.5, 1.5), 0.0)))


def vein_mask():
    """Positive inside a vein's ridges: 1.21.10's vein_ridged, negated."""
    ridge = lambda name: abs_(interpolated(y_band(-60, 51, noise(mc(name), 4.0, 4.0), 0.0)))
    return cache(sub(0.08, max_(ridge("ore_vein_a"), ridge("ore_vein_b"))))


def vein_strength(sign):
    """How far the toggle is into a vein type's side: the toggle for copper, its negation for iron."""
    return REF_VEIN_TOGGLE if sign > 0 else unary("negate", REF_VEIN_TOGGLE)


def vein_density(name):
    """VEIN_CHANCE where PvOreVeins placed a vein block of this type (before its draw), -1 elsewhere: within the type's
    heights, strong enough after the edge fade (20 blocks from the ends, down to -0.2), and inside the ridges."""
    _, _, _, low, high, _, _, sign = VEINS[name]
    fade = add(mul(clamp(min_(sub(float(high), mc("y")), sub(mc("y"), float(low))), 0.0, 20.0), 0.01), -0.2)
    strong = range_choice(add(vein_strength(sign), fade), VEIN_THRESHOLD, 1000000.0, VEIN_CHANCE, -1.0)
    in_ridges = range_choice(REF_VEIN_MASK, MIN_POSITIVE, 1000000.0, strong, -1.0)
    return range_choice(mc("y"), low - 0.5, high + 0.5, in_ridges, -1.0)


def vein_richness(name):
    """The chance of ore: 0.15 at the threshold, rising to the type's chance at strength 0.6."""
    ore_chance, sign = VEINS[name][5], VEINS[name][7]
    slope = (ore_chance - VEIN_MIN_ORE_CHANCE) / (VEIN_FULL_STRENGTH - VEIN_THRESHOLD)
    return add(mul(clamp(vein_strength(sign), VEIN_THRESHOLD, VEIN_FULL_STRENGTH), slope), VEIN_MIN_ORE_CHANCE - VEIN_THRESHOLD * slope)


def vein_rule(name):
    ore, raw, filler, _, _, _, raw_chance, _ = VEINS[name]
    return {"type": mc("ore_vein"), "density": pv("overworld/ore_vein/%s_density" % name), "filler_block": mc(filler),
            "filler_gap": mc("overworld/ore_vein/gap"), "ore_block": mc(ore), "raw_ore_block": mc(raw),
            "raw_ore_chance": raw_chance, "richness": pv("overworld/ore_vein/%s_richness" % name)}


FUNCTIONS = {
    "overworld/ore_vein/toggle": vein_toggle,
    "overworld/ore_vein/mask": vein_mask,
    "overworld/ore_vein/copper_density": lambda: vein_density("copper"),
    "overworld/ore_vein/copper_richness": lambda: vein_richness("copper"),
    "overworld/ore_vein/iron_density": lambda: vein_density("iron"),
    "overworld/ore_vein/iron_richness": lambda: vein_richness("iron"),
    "overworld/depth": depth,
    "overworld/sloped_cheese": sloped_cheese,
    "overworld/terrain": terrain,
    "overworld/final_density": final_density,
    "overworld/caves/entrances": entrances,
    "overworld/caves/noodle_toggle": lambda: noodle_input(noise(mc("noodle"), 1.0, 1.0), -1.0),
    "overworld/caves/noodle_thickness": lambda: noodle_input(add(mul(noise(mc("noodle_thickness"), 1.0, 1.0), -0.025), -0.07500000000000001), 0.0),
    "overworld/caves/noodle_ridge_a": lambda: noodle_input(noise(mc("noodle_ridge_a"), 2.6666666666666665, 2.6666666666666665), 0.0),
    "overworld/caves/noodle_ridge_b": lambda: noodle_input(noise(mc("noodle_ridge_b"), 2.6666666666666665, 2.6666666666666665), 0.0),
    "overworld/caves/noodle": noodle,
    "overworld/caves/corridor_entrances": corridor_entrances,
    "overworld/caves/corridor_noodle": corridor_noodle,
    "overworld/caves/flooded_corridors": flooded_corridors,
    "overworld/caves/dry_paths": dry_paths,
    "overworld/caves/basin_level": basin_level,
    "overworld/caves/basin_floor": basin_floor,
    "overworld/high_river": lambda: noise(pv("high_river"), 0.25, 0.0),
    "overworld/high_river/across": lambda: high_river_across(HIGH_RIVER),
    "overworld/high_river/valley": lambda: high_river_valley(HIGH_RIVER, REF_ACROSS),
    "overworld/high_river/upper_across": lambda: high_river_across(HIGH_RIVER_UPPER),
    "overworld/high_river/upper_valley": lambda: high_river_valley(HIGH_RIVER_UPPER, REF_UPPER_ACROSS, below=HIGH_RIVER),
    "overworld/high_river/bank": lambda: high_river_bank(HIGH_RIVER),
    "overworld/high_river/upper_bank": lambda: high_river_bank(HIGH_RIVER_UPPER, below=HIGH_RIVER),
    "overworld/aquifer_barrier": lambda: noise(mc("aquifer_barrier"), 1.0, 0.5),
}
# Replaced by the functions above and gone: the kernels' inputs that only they read.
GONE = ["overworld/high_river/channel.json", "overworld/high_river/depth.json", "overworld/high_river/terrain.json",
        "overworld/preliminary_surface_level.json"]


def noise_settings():
    """The Players Versus overworld's noise settings (1.21.10's, in 26.3's shape): the router keeps the terrain and
    climate functions, the aquifer's inputs move to its own config, the surface rule becomes a material rule."""
    shifted = lambda name: noise(mc(name), 0.25, 0.0, shift=True)
    return {
        "aquifers": {
            "barrier": pv("overworld/aquifer_barrier"),
            "exclusion": 0.0,
            "fluid_level_floodedness": {
                "type": pv("aquifer_floodedness"),
                "depth": REF_DEPTH,
                "continentalness": noise(mc("continentalness"), 0.25, 0.1, shift=True),
                "ridge": REF_RIDGES,
                "entrances": REF_ENTRANCES,
                "surface": noise(mc("surface"), 2.0, 1.0),
                "ramen": noise(mc("noodle"), 3.0, 3.0),
                "high_river": REF_VALLEY,
                "high_river_upper": REF_UPPER_VALLEY,
            },
            "fluid_level_spread": {
                "type": pv("aquifer_spread"),
                "entrances": REF_ENTRANCES,
                "noodle": pv("overworld/caves/noodle"),
                "surface": noise(mc("surface"), 4.0, 2.0),
                "corridors": REF_FLOODED_CORRIDORS,
                "dry_paths": REF_DRY_PATHS,
                "level": REF_BASIN_LEVEL,
                "floor": REF_BASIN_FLOOR,
            },
            "lava": noise(mc("aquifer_lava"), 1.0, 1.0),
            "surface_level": mc("overworld/preliminary_surface_level"),
        },
        "default_block": mc("stone"),
        "default_fluid": mc("water"),
        "disable_mob_generation": False,
        "legacy_random_source": False,
        "material_rule": pv("overworld"),
        "noise": {"height": 336, "min_y": -64},
        "noise_router": {
            "chunk_surface_level": mc("overworld/chunk_surface_level"),
            "continents": REF_CONTINENTS,
            "depth": REF_DEPTH,
            "erosion": mc("overworld/erosion"),
            "final_density": pv("overworld/final_density"),
            "ridges": REF_RIDGES,
            "temperature": shifted("temperature"),
            "vegetation": shifted("vegetation"),
        },
        "sea_level": C["SEA_LEVEL"],
        "spawn_target": [
            {mc("overworld/continents"): [-0.11, 0.8], mc("overworld/erosion"): [-1.0, 1.0], mc("overworld/ridges"): [-1.0, -0.16],
             mc("overworld/temperature"): [-0.8, 0.6], mc("overworld/vegetation"): [-0.8, 0.8]},
            {mc("overworld/continents"): [-0.11, 0.8], mc("overworld/erosion"): [-1.0, 1.0], mc("overworld/ridges"): [0.16, 1.0],
             mc("overworld/temperature"): [-0.8, 0.6], mc("overworld/vegetation"): [-0.8, 0.8]},
        ],
    }


# --- the surface rule, as 26.3's material rule --------------------------------------------------------------------

def block_state(state):
    """1.21.10's {"Name", "Properties"} as 26.3 writes a block state: its id, or {"id", "properties"}."""
    if not isinstance(state, dict) or "Name" not in state:
        return state
    return {"id": state["Name"], "properties": state["Properties"]} if state.get("Properties") else state["Name"]


def material(rule):
    """A 1.21.10 surface rule or condition in 26.3's format: the same types and fields, block states rewritten."""
    if isinstance(rule, list):
        return [material(r) for r in rule]
    if not isinstance(rule, dict):
        return rule
    return {k: block_state(v) if k == "result_state" else material(v) for k, v in rule.items()}


def material_rule(surface_rule):
    """The surface rule with the ore veins after the bedrock floor, where vanilla 26.3 puts its own."""
    rule = material(surface_rule)
    assert rule["type"] == mc("sequence") and rule["sequence"][0]["if_true"].get("random_name") == mc("bedrock_floor")
    rule["sequence"][1:1] = [vein_rule("copper"), vein_rule("iron")]
    return rule


# --- noises and carvers ---------------------------------------------------------------------------------------------

# 1.21.10's parameters (firstOctave, amplitudes) of the Players Versus noises; 26.3 wants them as base_octave,
# octave_count, amplitude_modifiers and a base_amplitude that gives the same values (NormalNoise.createParity).
NOISES = {
    "high_river": (-7, [1.0, 2.0, 1.0]),
    "cave_basins": (-10, [1.0, 1.0]),
    "sand_beach": (-7, [1.0] * 10 + [40.0, 20.0] + [10.0] * 17),
    "gravel_beach": (-7, [1.0] * 10 + [40.0, 20.0] + [10.0] * 17),
}


def parity_base_amplitude(modifiers):
    """NormalNoise.createParity's base amplitude: 1.21.10's value factor (1/6 over the expected deviation of the span
    of non-zero octaves) over the octaves' summed amplitude and 26.3's normalization. It gives vanilla's 60 converted
    noises' base_amplitude to the last bit or within 2 ulps (the order of a few operations isn't known)."""
    amplitudes = [m * 0.5 ** i for i, m in enumerate(modifiers)]
    deviation = math.sqrt(sum((a * 0.2702247831245211) ** 2 for a in amplitudes))
    normalization = 0.3333333333333333 / math.sqrt(2.0) / deviation
    used = [i for i, m in enumerate(modifiers) if m != 0.0]
    value_factor = 0.3333333333333333 * 0.5 / (0.1 * (1.0 + 1.0 / (used[-1] - used[0] + 1)))
    return value_factor / (sum(amplitudes) * normalization)


def noise_parameters(first_octave, amplitudes):
    out = {}
    if any(a != 1.0 for a in amplitudes):
        out["amplitude_modifiers"] = amplitudes
    out["base_amplitude"] = parity_base_amplitude(amplitudes)
    out["base_octave"] = first_octave
    if len(amplitudes) > 1:
        out["octave_count"] = len(amplitudes)
    return out


def carver(old):
    """A 1.21.10 configured carver as a 26.3 carver: its settings inline; the cave carver's count, thickness and weird
    thickness bias, which 1.21.10 hard-coded, as vanilla 26.3 writes them for its caves; yScale renamed; the lava
    level, replaceable blocks (26.3 carves anything not tagged uncarvable) and debug settings gone."""
    config = dict(old["config"])
    for gone in ("lava_level", "replaceable", "debug_settings"):
        config.pop(gone, None)
    if old["type"] == mc("cave"):
        config["room_vertical_radius_multiplier"] = config.pop("yScale")
        config["count"] = {"type": mc("very_biased_to_bottom"), "max_inclusive": 14, "min_inclusive": 0}
        config["thickness"] = {"type": mc("trapezoid"), "max": 3.0, "min": 0.0, "plateau": 1.0}
        config["weird_thickness_bias"] = True
    elif old["type"] == mc("canyon"):
        config["shape"] = dict(config["shape"], y_scale=config.pop("yScale"))
    else:
        raise ValueError(old["type"])
    return dict({"type": old["type"]}, **dict(sorted(config.items())))


def outputs(sources):
    files = {DATA / "density_function" / (name + ".json"): build() for name, build in FUNCTIONS.items()}
    files[DATA / "noise_settings/overworld.json"] = noise_settings()
    files[DATA / "material_rule/overworld.json"] = material_rule(sources["surface_rule"])
    for name, (first, amplitudes) in NOISES.items():
        files[DATA / "noise" / (name + ".json")] = noise_parameters(first, amplitudes)
    for name, old in sources["carvers"].items():
        files[DATA / "carver" / name] = carver(old)
    return files


def sources():
    """The 1.21.10 files the conversion reads: the surface rule and the configured carvers, from git's last 1.21.10
    commit of them if they're gone from the tree (the conversion replaces them)."""
    import subprocess

    def read(path):
        p = DATA / path
        if p.exists():
            return json.loads(p.read_text())
        return json.loads(subprocess.run(["git", "show", "%s:%s" % (BASE, (DATA / path).relative_to(ROOT))], cwd=ROOT,
                                         check=True, capture_output=True, text=True).stdout)

    settings = read("noise_settings/overworld.json")
    if "surface_rule" not in settings:
        settings = json.loads(subprocess.run(["git", "show", "%s:%s" % (BASE, (DATA / "noise_settings/overworld.json").relative_to(ROOT))],
                                             cwd=ROOT, check=True, capture_output=True, text=True).stdout)
    carvers = {}
    listing = subprocess.run(["git", "ls-tree", "--name-only", "%s:%s" % (BASE, (DATA / "configured_carver").relative_to(ROOT))],
                             cwd=ROOT, check=True, capture_output=True, text=True).stdout.split()
    for name in listing:
        carvers[name] = read("configured_carver/" + name)
    return {"surface_rule": settings["surface_rule"], "carvers": carvers}


# The last commit with the 1.21.10 worldgen data, which the conversion reads from.
BASE = "19f0545"


def dump(value):
    return json.dumps(value, indent=2) + "\n"


def main(args):
    files = outputs(sources())
    gone = [DATA / "density_function" / g for g in GONE] + [DATA / "configured_carver"]
    if args == ["--check"]:
        stale = [p for p, v in files.items() if not p.exists() or p.read_text() != dump(v)]
        stale += [p for p in gone if p.exists()]
        for p in stale:
            print("stale:", p.relative_to(ROOT))
        sys.exit(1 if stale else 0)
    for path, value in files.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(dump(value))
    for p in gone:
        if p.is_dir():
            for child in p.iterdir():
                child.unlink()
            p.rmdir()
        else:
            p.unlink(missing_ok=True)
    print(len(files), "files written")


if __name__ == "__main__":
    main(sys.argv[1:])
