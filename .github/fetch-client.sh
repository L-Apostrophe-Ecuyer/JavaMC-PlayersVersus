#!/usr/bin/env bash
# Downloads a Minecraft version's client jar to <dir>/client.jar, and with "libraries" its libraries to <dir>/libs (for
# javap to resolve supertypes). Used by api-diff.yml.
#
# fetch-client.sh <version id> <dir> [libraries]
set -euo pipefail
version="$1"
dir="$2"
mkdir -p "$dir"
url=$(curl -fsSL https://piston-meta.mojang.com/mc/game/version_manifest_v2.json | python3 -c '
import json, sys
urls = [v["url"] for v in json.load(sys.stdin)["versions"] if v["id"] == sys.argv[1]]
if not urls:
    sys.exit("no version " + sys.argv[1] + " in the manifest")
print(urls[0])' "$version")
curl -fsSL "$url" -o "$dir/version.json"
curl -fsSL "$(python3 -c 'import json, sys; print(json.load(open(sys.argv[1]))["downloads"]["client"]["url"])' "$dir/version.json")" -o "$dir/client.jar"
if [ "${3:-}" = "libraries" ]; then
  python3 - "$dir" <<'EOF'
import json, os, sys, urllib.request
d = sys.argv[1]
os.makedirs(os.path.join(d, "libs"), exist_ok=True)
for library in json.load(open(os.path.join(d, "version.json")))["libraries"]:
    artifact = library.get("downloads", {}).get("artifact")
    if artifact and not library.get("rules"):
        urllib.request.urlretrieve(artifact["url"], os.path.join(d, "libs", artifact["path"].split("/")[-1]))
EOF
fi
echo "$version: $(ls -la "$dir/client.jar")"
