#!/usr/bin/env python3
"""The mod's screen effects (assets/players-versus/post_effect), made of vanilla's own post shaders.

Usage: post-effects.py <assets/players-versus dir>

- blight_1 to _20: the world greying as Blight whitens the hearts, a twentieth more each (client PaleSight
  eases between them, so the grey comes and goes smoothly). One pass of
  minecraft:post/color_convolve: each colour row mixes the grey (0.3, 0.59, 0.11) with the colour's own, by k. The
  shader then multiplies what colour is left by 1.8 (its Saturation), so k = saturation / 1.8 leaves exactly that much
  colour, and the brightness as it was. Then minecraft:post/blit back to the screen.
- watchling_1 to _16: the screen breaking up under a watchling's stare, a sixteenth more each: colour drained the same way, then
  minecraft:post/bits, the creeper's view: big pixels (MosaicSize) and few shades (Resolution); bits multiplies the
  colour left by 1.5 again, so k = saturation / 2.7. The pixels grow from 1 to 8 screen pixels, the shades fall from 64
  to 6, the colour drains to a fifth, each a little more every step. (The static itself is the HUD's, WatchlingStatic.)
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


BLIGHT_STEPS, WATCHLING_STEPS = 20, 16


def main(assets):
    out = Path(assets) / "post_effect"
    out.mkdir(parents=True, exist_ok=True)
    for old in out.glob("blight_*.json"):
        old.unlink()
    for old in out.glob("watchling_*.json"):
        old.unlink()
    for step in range(1, BLIGHT_STEPS + 1):
        saturation = 1.0 - step / BLIGHT_STEPS
        effect = {"targets": {"swap": {}}, "passes": [convolve(saturation / 1.8, "swap"), blit()]}
        (out / f"blight_{step}.json").write_text(json.dumps(effect, indent=4) + "\n")
    for step in range(1, WATCHLING_STEPS + 1):
        strength = step / WATCHLING_STEPS
        mosaic = round(1.0 + 7.0 * strength ** 1.5, 3)
        resolution = round(64.0 * (6.0 / 64.0) ** strength, 2)
        saturation = 1.0 - 0.8 * strength
        effect = {"targets": {"swap": {}}, "passes": [convolve(saturation / 2.7, "swap"), bits(resolution, mosaic)]}
        (out / f"watchling_{step}.json").write_text(json.dumps(effect, indent=4) + "\n")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])
