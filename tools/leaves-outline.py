#!/usr/bin/env python3
"""Draws the leaves view's frame (LeavesView): textures/misc/leaves_outline.png. Run from the repository root.

As vanilla's powder snow outline, a 256 x 256 frame stretched over the screen: pixel-art leaves thick in the corners
and along the edges, thinning toward a clear round middle, a few loose ones further in. Grey, lit from the top left,
so the game can tint it with the colour of the leaves the camera is in. Seeded, so the same picture every run.
"""
import math
import os
import random

from PIL import Image, ImageDraw

SIZE = 256
SEED = 26_4
OUT = os.path.join("src", "main", "resources", "assets", "players-versus", "textures", "misc", "leaves_outline.png")


def leaf_outline(cx, cy, length, width, angle):
    """The points round a leaf: pointed at both ends, widest a third of the way from its stem."""
    points = []
    steps = max(8, int(length))
    for side in (1, -1):
        for i in range(steps + 1):
            t = i / steps if side == 1 else 1 - i / steps
            half = width / 2 * math.sin(math.pi * t ** 0.8) ** 0.9
            along, across = (t - 0.5) * length, side * half
            points.append((cx + along * math.cos(angle) - across * math.sin(angle),
                           cy + along * math.sin(angle) + across * math.cos(angle)))
    return points


def draw_leaf(image, rng, cx, cy, length, angle):
    width = length * rng.uniform(0.42, 0.55)
    base = rng.randint(150, 225)
    # The whole leaf in its outline's shade, then the face inset a pixel, its shaded half, and the midrib
    layer = Image.new("L", (SIZE, SIZE), 0)
    ImageDraw.Draw(layer).polygon(leaf_outline(cx, cy, length, width, angle), fill=255)
    inner = Image.new("L", (SIZE, SIZE), 0)
    ImageDraw.Draw(inner).polygon(leaf_outline(cx, cy, length - 2.5, width - 2.5, angle), fill=255)
    pixels, mask, face = image.load(), layer.load(), inner.load()
    # Lit from the top left: the half of the leaf facing away is darker
    light = (math.cos(angle + math.pi / 2), math.sin(angle + math.pi / 2))
    shade_sign = 1 if light[0] + light[1] > 0 else -1
    for y in range(max(0, int(cy - length)), min(SIZE, int(cy + length) + 1)):
        for x in range(max(0, int(cx - length)), min(SIZE, int(cx + length) + 1)):
            if not mask[x, y]:
                continue
            if not face[x, y]:
                grey = max(40, base - 85)
            else:
                across = (x - cx) * -math.sin(angle) + (y - cy) * math.cos(angle)
                grey = base - (22 if across * shade_sign > 0 else 0) + rng.randint(-9, 9)
                if abs(across) < 0.6:
                    grey = base - 55
            pixels[x, y] = (max(0, min(255, grey)),) * 3 + (255,)


def main():
    rng = random.Random(SEED)
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    leaves = []
    for _ in range(9000):
        x, y = rng.uniform(-8, SIZE + 8), rng.uniform(-8, SIZE + 8)
        r = math.hypot(x - SIZE / 2, y - SIZE / 2) / (SIZE / 2)
        chance = min(1.0, max(0.0, (r - 0.74) / 0.3)) ** 1.4
        if r > 0.62 and rng.random() < 0.006:
            chance = 1.0  # a loose leaf further in
        if rng.random() >= chance * 0.3:
            continue
        inward = math.atan2(SIZE / 2 - y, SIZE / 2 - x)
        length = rng.uniform(9, 15) + 9 * min(1.0, max(0.0, r - 0.8))
        leaves.append((r, x, y, length, inward + rng.uniform(-1.0, 1.0)))
    # The inner leaves first, so the thick of the frame lies over them
    for r, x, y, length, angle in sorted(leaves):
        draw_leaf(image, rng, x, y, length, angle)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    image.save(OUT, optimize=True)
    print(f"{OUT}: {len(leaves)} leaves")


if __name__ == "__main__":
    main()
