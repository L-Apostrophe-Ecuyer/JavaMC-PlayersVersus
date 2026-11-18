#!/bin/bash
# Draws a rock without the game: compiles the rocks' model and RockPreview with javac alone and runs it.
#
#     tools/rock-preview.sh out=rock.png shape=monolith height=30 radius=9 seed=5 [orbit=40,48,3] [look=12]
#     tools/rock-preview.sh out=rock.png template=some-rock.json seed=5 [water=2] [size=960x540]
#
# Block textures come from the mod's own, then from the game's client jar in Gradle's cache (run a build once first).
set -euo pipefail
cd "$(dirname "$0")/.."
classes=build/rock-preview
gson=$(find ~/.gradle -name 'gson-2*.jar' ! -name '*sources*' 2>/dev/null | head -1)
client=$(find ~/.gradle .gradle -name 'minecraft-client*only*.jar' ! -name '*sources*' 2>/dev/null | head -1)
rocks=src/main/java/frootloops/versus/mod/environment/worldgen/rocks
mkdir -p "$classes"
javac -nowarn -d "$classes" -cp "$gson" "$rocks"/Rock{Noise,Material,Palette,Plan,Shape,Model,Canvas,Stamper,Template,Dice,Block,Voxels,Light}.java \
    src/test/java/frootloops/versus/mod/environment/worldgen/rocks/RockPreview.java
java -cp "$classes:$gson" frootloops.versus.mod.environment.worldgen.rocks.RockPreview \
    "textures=src/main/resources/assets/minecraft/textures/block:$client" "$@"
