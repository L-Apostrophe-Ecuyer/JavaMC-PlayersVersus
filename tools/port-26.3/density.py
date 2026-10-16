"""Density functions from the Minecraft 1.21.10 format to 26.3's.

26.3 renamed fields (argument -> input, argument1/argument2 -> left/right), merged the four caches into `cache`,
gave `interpolated` its cell size, and replaced some types: y_clamped_gradient by gradient on the y axis, shifted_noise
by noise with shifts, end_islands by end_outer_islands, invert by reciprocal, weird_scaled_sampler by an interval_select
of the noise at the rarity's scales (abs of rarity * noise(pos / rarity), as vanilla 26.3 writes its spaghetti caves).
Checked against vanilla: `python3 density.py --check` converts vanilla 1.21.10's density functions and compares them
with 26.3's.
"""
import json
import sys

MAPPED = {"abs", "square", "cube", "half_negative", "quarter_negative", "squeeze"}
BINARY = {"add", "mul", "min", "max"}
CACHES = {"flat_cache", "cache_2d", "cache_once", "cache_all_in_cell"}
# Rarity mappers of weird_scaled_sampler (1.21.10 NoiseRouterData.QuantizedSpaghettiRarity): thresholds and rarities.
RARITY = {
    "type_1": ([-0.5, 0.0, 0.5], [0.75, 1.0, 1.5, 2.0]),
    "type_2": ([-0.75, -0.5, 0.5, 0.75], [0.5, 0.75, 1.0, 2.0, 3.0]),
}


def mc(name):
    return name if ":" in name else "minecraft:" + name


def convert(value, cell_xz=4, cell_y=8):
    """Converts one density function (a number, a reference or an object)."""
    if not isinstance(value, dict):
        return value
    kind = mc(value["type"]).split(":", 1)[1] if value["type"].startswith("minecraft:") or ":" not in value["type"] else None
    c = lambda v: convert(v, cell_xz, cell_y)
    if kind is None:  # another namespace's type: convert its fields as far as they are functions
        return {k: (c(v) if isinstance(v, dict) else v) for k, v in value.items()}
    if kind in MAPPED:
        return {"type": mc(kind), "input": c(value["argument"])}
    if kind == "invert":
        return {"type": "minecraft:reciprocal", "input": c(value["argument"])}
    if kind in BINARY:
        return {"type": mc(kind), "left": c(value["argument1"]), "right": c(value["argument2"])}
    if kind in CACHES:
        return {"type": "minecraft:cache", "input": c(value["argument"])}
    if kind == "blend_density":
        return {"type": "minecraft:blend_density", "input": c(value["argument"])}
    if kind == "interpolated":
        return {"type": "minecraft:interpolated", "cell_size_xz": cell_xz, "cell_size_y": cell_y, "input": c(value["argument"])}
    if kind == "y_clamped_gradient":
        return {"type": "minecraft:gradient", "axis": "y", "from_coordinate": value["from_y"], "from_value": value["from_value"],
                "to_coordinate": value["to_y"], "to_value": value["to_value"]}
    if kind in ("shift_a", "shift_b", "shift"):
        return {"type": mc(kind), "noise": value["argument"]}
    if kind == "shifted_noise":
        if value.get("shift_y", 0) not in (0, 0.0):
            raise ValueError("shifted_noise with a y shift has no 26.3 form: " + json.dumps(value)[:200])
        return {"type": "minecraft:noise", "noise": value["noise"], "shift_x": c(value["shift_x"]), "shift_z": c(value["shift_z"]),
                "xz_scale": value["xz_scale"], "y_scale": value["y_scale"]}
    if kind == "end_islands":
        return {"type": "minecraft:end_outer_islands"}
    if kind == "weird_scaled_sampler":
        thresholds, rarities = RARITY[value["rarity_value_mapper"]]
        return {"type": "minecraft:abs", "input": {
            "type": "minecraft:interval_select",
            "functions": [{"type": "minecraft:mul", "left": {"type": "minecraft:noise", "noise": value["noise"],
                                                            "xz_scale": 1.0 / r, "y_scale": 1.0 / r}, "right": r} for r in rarities],
            "input": c(value["input"]),
            "thresholds": thresholds}}
    if kind in ("clamp", "range_choice", "spline", "noise", "old_blended_noise", "find_top_surface", "blend_alpha", "blend_offset",
                "beardifier", "constant"):
        return {k: (c(v) if k in ("input", "when_in_range", "when_out_of_range", "density", "upper_bound", "lower_bound",
                                  "argument") else convert_spline(v, cell_xz, cell_y) if k == "spline" else v)
                for k, v in value.items()}
    raise ValueError("unknown density function type " + value["type"])


def convert_spline(spline, cell_xz, cell_y):
    """Splines keep their format; their coordinates are density functions."""
    if not isinstance(spline, dict):
        return spline
    out = dict(spline)
    if "coordinate" in out:
        out["coordinate"] = convert(out["coordinate"], cell_xz, cell_y)
    if "points" in out:
        out["points"] = [{**p, "value": convert_spline(p["value"], cell_xz, cell_y)} for p in out["points"]]
    return out


def check():
    """Converts vanilla 1.21.10's overworld density functions and compares them with vanilla 26.3's."""
    import pathlib
    old = pathlib.Path("/home/user/misode/1.21.10-data/data/minecraft/worldgen/density_function")
    new = pathlib.Path("/home/user/misode/26.3-data/data/minecraft/worldgen/density_function")
    same = differ = 0
    for p in sorted(old.rglob("*.json")):
        q = new / p.relative_to(old)
        if not q.exists():
            print("gone in 26.3:", p.relative_to(old)); continue
        a = convert(json.loads(p.read_text()))
        b = json.loads(q.read_text())
        if a == b:
            same += 1
        else:
            differ += 1
            print("differs:", p.relative_to(old))
    print(same, "the same,", differ, "different")


if __name__ == "__main__":
    if sys.argv[1:] == ["--check"]:
        check()
