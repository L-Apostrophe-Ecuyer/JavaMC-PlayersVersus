#!/usr/bin/env python3
"""Writes the terrain's offset and factor, with the shattered terrain's place moved: run from the repository root.

    python3 tools/terrain.py [folder holding data/minecraft/worldgen/density_function/overworld]

Vanilla's offset and factor are splines of continentalness, erosion and the ridges. Each holds one bump at erosion 0.40
to 0.58 (the points at 0.45 and 0.55 are its plateau): the ground about 22 blocks higher and the factor, which sets how
much the 3D base noise shows, from 4.7 to 6.3 down to 1.4 to 1.8, so overhangs and floating pieces. The bump is vanilla's
"windswept" terrain, in every humidity and temperature. Here it belongs to the 3D-noise zone only (spec
docs/superpowers/specs/2026-11-12-noise-terrain-zone-design.md):

    offset and factor = lerp(players-versus:overworld/noise_zone, plain, shattered)

  plain      vanilla's spline with the points at erosion 0.45 and 0.55 taken out of each erosion node: it runs on from
             the plain terrain below the bump to the plain terrain above it. Elsewhere it is vanilla's spline, point
             for point.
  shattered  the bump's plateau: each erosion node that has the points is replaced by their sub-spline (equal at 0.45
             and 0.55, asserted), the others (the coast's, which have none) read erosion 0.5. A function of
             continentalness and the ridges only: inside the window the terrain doesn't depend on the erosion.

Both files keep vanilla's wrapper (the cache around lerp(blend_alpha, ...), the offset's -0.50375). The surface-level
estimates the aquifer and the material rules read are vanilla's with this offset and factor.

Vanilla's files come from the game's jar in Gradle's cache unless a folder is given.
"""
import glob
import json
import os
import sys
import zipfile

MOD = "players-versus"
DATA = os.path.join("src", "main", "resources", "data", MOD, "worldgen", "density_function", "overworld")
VANILLA = "data/minecraft/worldgen/density_function/overworld/"
EROSION = "minecraft:overworld/erosion"
BUMP = (0.45, 0.55)
ZONE = f"{MOD}:overworld/noise_zone"
# The erosion the shattered form reads where a node has no plateau of its own
PLATEAU_EROSION = 0.5


def vanilla_file(name, folder):
    """Vanilla's density function overworld/<name>, from the folder or the newest game jar in Gradle's cache."""
    if folder:
        with open(os.path.join(folder, "data", "minecraft", "worldgen", "density_function", "overworld", name + ".json")) as file:
            return json.load(file)
    jars = sorted(glob.glob(os.path.expanduser("~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-common-deobf/*/minecraft-common-deobf-*.jar")))
    jars = [jar for jar in jars if "sources" not in jar]
    if not jars:
        sys.exit("no game jar in Gradle's cache: run a build first, or give the folder of vanilla's data")
    with zipfile.ZipFile(jars[-1]) as archive:
        return json.loads(archive.read(VANILLA + name + ".json"))


def is_bump(point):
    return any(abs(point["location"] - at) < 1e-6 for at in BUMP)


def plain(spline):
    """The spline without the bump's points in its erosion nodes."""
    points = [p for p in spline["points"] if not (spline["coordinate"] == EROSION and is_bump(p))]
    return {"coordinate": spline["coordinate"], "points": [dict(p, value=plain(p["value"]) if isinstance(p["value"], dict) else p["value"]) for p in points]}


def shattered(value):
    """The spline's value inside the bump's plateau: its erosion nodes read the plateau (or erosion 0.5)."""
    if not isinstance(value, dict):
        return value
    if value["coordinate"] == EROSION:
        bump = [p for p in value["points"] if is_bump(p)]
        if bump:
            for other in bump[1:]:
                assert json.dumps(other["value"], sort_keys=True) == json.dumps(bump[0]["value"], sort_keys=True), "the plateau's two points differ"
                assert other["derivative"] == 0.0 and bump[0]["derivative"] == 0.0, "the plateau isn't flat"
            return shattered(bump[0]["value"])
        value = dict(value, coordinate=PLATEAU_EROSION)
    return {"coordinate": value["coordinate"], "points": [dict(p, value=shattered(p["value"])) for p in value["points"]]}


def replace_spline(node, make):
    """The vanilla function with its spline replaced by what `make` builds from it (the wrapper kept)."""
    if isinstance(node, dict):
        if node.get("type") == "minecraft:spline":
            return make(node["spline"])
        return {key: replace_spline(value, make) for key, value in node.items()}
    return node


def mixed(spline):
    return {"type": "minecraft:lerp", "alpha": ZONE, "first": {"type": "minecraft:spline", "spline": plain(spline)},
            "second": {"type": "minecraft:spline", "spline": shattered(spline)}}


def use_ours(node):
    """The same function reading this mod's offset and factor."""
    if isinstance(node, str):
        return {"minecraft:overworld/offset": f"{MOD}:overworld/offset", "minecraft:overworld/factor": f"{MOD}:overworld/factor",
                "minecraft:overworld/preliminary_surface_level": f"{MOD}:overworld/preliminary_surface_level"}.get(node, node)
    if isinstance(node, dict):
        return {key: use_ours(value) for key, value in node.items()}
    return node


def write(name, value):
    os.makedirs(DATA, exist_ok=True)
    with open(os.path.join(DATA, name + ".json"), "w", newline="\n") as file:
        json.dump(value, file, indent=2)
        file.write("\n")


def main(folder):
    for name in ("offset", "factor"):
        write(name, replace_spline(vanilla_file(name, folder), mixed))
    for name in ("preliminary_surface_level", "chunk_surface_level"):
        write(name, use_ours(vanilla_file(name, folder)))
    print("offset, factor, preliminary_surface_level and chunk_surface_level written")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else None)
