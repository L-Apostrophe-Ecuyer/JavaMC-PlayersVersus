#!/bin/bash
# The worldgen gallery on a machine without a display: runs the real client under a virtual one and saves screenshots.
#
#     tools/gallery.sh <shots> [output directory] [extra Gradle options]
#
# <shots> is what -Ppv.gallery takes (see WorldgenGallery), or @<file> with one shot a line, e.g.
#     tools/gallery.sh "biome:players-versus:tuscan_grove;biome:swamp" /tmp/shots -Ppv.gallery.seed=42
# Needs xvfb and Mesa's software OpenGL (apt-get install xvfb libgl1-mesa-dri libegl1). The game is made to use OpenGL
# through EGL: Xvfb's GLX has none of the sRGB framebuffers the game asks for, and Mesa's software Vulkan takes minutes
# a frame with this renderer.
set -euo pipefail
cd "$(dirname "$0")/.."
shots="$1"
out="${2:-build/gallery}"
shift $(( $# > 1 ? 2 : 1 ))
export XDG_RUNTIME_DIR="${XDG_RUNTIME_DIR:-$(mktemp -d)}"
export SDL_VIDEO_FORCE_EGL=1 VK_LOADER_DRIVERS_DISABLE='*'
xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew runClientGameTest "-Ppv.gallery=$shots" "$@"
mkdir -p "$out"
cp build/run/clientGameTest/screenshots/*.png "$out"/
echo "Screenshots in $out:"
ls "$out"
