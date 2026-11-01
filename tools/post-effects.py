#!/usr/bin/env python3
"""The mod's screen effects (assets/players-versus/post_effect), made of vanilla's own post shaders.

Usage: post-effects.py <assets/players-versus dir>

- conversion_1 to _4: the world greying as Conversion whitens the hearts (client PaleSight picks the step). One pass of
  minecraft:post/color_convolve: each colour row mixes the grey (0.3, 0.59, 0.11) with the colour's own, by k. The
  shader then multiplies what colour is left by 1.8 (its Saturation), so k = saturation / 1.8 leaves exactly that much
  colour, and the brightness as it was. Then minecraft:post/blit back to the screen.
- watchling_1 to _4: the screen breaking up under a watchling's stare: colour drained the same way, then
  minecraft:post/bits, the creeper's view: big pixels (MosaicSize) and few shades (Resolution); bits multiplies the
  colour left by 1.5 again, so k = saturation / 2.7. (The static itself is the HUD's, WatchlingStatic.)
"""
import json
import sys
from pathlib import Path

GREY = (0.3, 0.59, 0.11)


def matrix(k):
    rows = []
    for i in range(3):
        rows.append([round((1 - k) * GREY[j] + (k if i == j else 0.0), 5) for j in range(3)])
    return rows


def convolve(k, output):
    r, g, b = matrix(k)
    return {
        "vertex_shader": "minecraft:core/screentriangle",
        "fragment_shader": "minecraft:post/color_convolve",
        "inputs": [{"sampler_name": "In", "target": "minecraft:main"}],
        "output": output,
        "uniforms": {"ColorConfig": [
            {"name": "RedMatrix", "type": "vec3", "value": r},
            {"name": "GreenMatrix", "type": "vec3", "value": g},
            {"name": "BlueMatrix", "type": "vec3", "value": b},
        ]},
    }


def blit():
    return {
        "vertex_shader": "minecraft:core/screentriangle",
        "fragment_shader": "minecraft:post/blit",
        "inputs": [{"sampler_name": "In", "target": "swap"}],
        "uniforms": {"BlitConfig": [{"name": "ColorModulate", "type": "vec4", "value": [1.0, 1.0, 1.0, 1.0]}]},
        "output": "minecraft:main",
    }


def bits(resolution, mosaic):
    return {
        "vertex_shader": "minecraft:core/screentriangle",
        "fragment_shader": "minecraft:post/bits",
        "inputs": [{"sampler_name": "In", "target": "swap"}],
        "output": "minecraft:main",
        "uniforms": {"BitsConfig": [
            {"name": "Resolution", "type": "float", "value": float(resolution)},
            {"name": "MosaicSize", "type": "float", "value": float(mosaic)},
        ]},
    }


def main(assets):
    out = Path(assets) / "post_effect"
    out.mkdir(parents=True, exist_ok=True)
    for step in range(1, 5):
        saturation = 1.0 - step / 4.0
        effect = {"targets": {"swap": {}}, "passes": [convolve(saturation / 1.8, "swap"), blit()]}
        (out / f"conversion_{step}.json").write_text(json.dumps(effect, indent=4) + "\n")
    for step, (resolution, mosaic) in enumerate([(32, 2), (16, 3), (10, 5), (6, 8)], start=1):
        saturation = 1.0 - 0.2 * step
        effect = {"targets": {"swap": {}}, "passes": [convolve(saturation / 2.7, "swap"), bits(resolution, mosaic)]}
        (out / f"watchling_{step}.json").write_text(json.dumps(effect, indent=4) + "\n")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
