#!/usr/bin/env python3
"""The mod's tag files, as the generators write them: run on its own from the repository root, prunes them all.

    python3 tools/tagfiles.py <vanilla>     (<vanilla> holds data/minecraft, from the game's jars)

The game merges the mod's tag files with its own and follows the tags nested in them, so an entry is said once: never
twice in a file, never when vanilla's own file of the tag lists it, never when a tag nested in the mod's file or in
vanilla's already holds it (the tag would hold it anyway). The generators add entries with add_to_tag, which keeps to
that once use_vanilla has told it where the game's data is; a tag a generator owns whole (the climate variants, from
tools/animals.py) it writes with set_tag, which no other generator's add_to_tag touches (OWNED). Pruning a file rewrites
it by the same rule; a file left with nothing to add is deleted. TagAuditTest checks the files keep to it.
"""
import json
import os
import sys

DATA = os.path.join("src", "main", "resources", "data")
# The registries the mod has tags of, longest first so that worldgen/biome isn't taken for worldgen
REGISTRIES = ["worldgen/world_preset", "worldgen/structure", "worldgen/biome", "entity_type", "damage_type", "enchantment", "block", "item"]
# Tags a generator writes whole (set_tag): add_to_tag leaves them alone
OWNED = {f"minecraft/tags/worldgen/biome/{name}.json" for name in
         ["spawns_warm_variant_farm_animals", "spawns_warm_variant_frogs", "spawns_cold_variant_farm_animals", "spawns_cold_variant_frogs"]}

_vanilla = None


def use_vanilla(root):
    """Where the game's data is (its data/minecraft): without it, add_to_tag only keeps an entry from being said twice."""
    global _vanilla
    _vanilla = root


def _read(path):
    with open(path) as file:
        return json.load(file)


def _write(path, value):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as file:
        json.dump(value, file, indent=2)
        file.write("\n")


def _ids(tag):
    return [value["id"] if isinstance(value, dict) else value for value in (tag or {}).get("values", [])]


def _split(path):
    """namespace, registry and name of a tag file's path under DATA (minecraft/tags/worldgen/biome/is_forest.json)."""
    namespace, _, rest = path.replace(os.sep, "/").split("/", 2)
    rest = rest[:-len(".json")]
    for registry in REGISTRIES:
        if rest.startswith(registry + "/"):
            return namespace, registry, rest[len(registry) + 1:]
    raise ValueError(f"no registry for {path}")


def _file(root, namespace, registry, name):
    path = os.path.join(root, namespace, "tags", registry, name + ".json")
    return _read(path) if os.path.exists(path) else None


def _resolve(namespace, registry, name, seen):
    """What a tag holds, vanilla's file and the mod's merged, nested tags followed."""
    if (namespace, name) in seen:
        return set()
    seen.add((namespace, name))
    mod = _file(DATA, namespace, registry, name)
    vanilla = None if mod and mod.get("replace") else _file(os.path.join(_vanilla, "data"), namespace, registry, name) if _vanilla else None
    held = set()
    for entry in _ids(vanilla) + _ids(mod):
        if entry.startswith("#"):
            nested_namespace, nested_name = entry[1:].split(":", 1)
            held |= _resolve(nested_namespace, registry, nested_name, seen)
        else:
            held.add(entry)
    return held


def _held_already(path, entries):
    """What the tag at this file holds without the file's own plain entries: vanilla's file, and the nested tags of both."""
    namespace, registry, name = _split(path)
    mod = _file(DATA, namespace, registry, name)
    vanilla = None if (mod and mod.get("replace")) or not _vanilla else _file(os.path.join(_vanilla, "data"), namespace, registry, name)
    held = {entry for entry in _ids(vanilla) if not entry.startswith("#")}
    for entry in _ids(vanilla) + list(entries):
        if entry.startswith("#"):
            nested_namespace, nested_name = entry[1:].split(":", 1)
            held |= _resolve(nested_namespace, registry, nested_name, set())
    return held


def _tidy(path, values, replace=False):
    """The entries said once: no duplicates, none the tag holds already; the file written, or deleted when it adds nothing."""
    full = os.path.join(DATA, path)
    kept, seen = [], set()
    for value in values:
        key = value["id"] if isinstance(value, dict) else value
        if key in seen:
            continue
        seen.add(key)
        kept.append(value)
    held = _held_already(path, [value for value in kept if isinstance(value, str)])
    kept = [value for value in kept if (value["id"] if isinstance(value, dict) else value).startswith("#")
            or (value["id"] if isinstance(value, dict) else value) not in held]
    if not kept and not replace:
        if os.path.exists(full):
            os.remove(full)
        return
    _write(full, {"replace": replace, "values": kept})


def add_to_tag(path, *values):
    """Adds these to the mod's tag file at `path` (under DATA), each once and only if the tag doesn't hold it already."""
    if path.replace(os.sep, "/") in OWNED:
        return
    full = os.path.join(DATA, path)
    tag = _read(full) if os.path.exists(full) else {"replace": False, "values": []}
    _tidy(path, tag["values"] + [value for value in values if value not in tag["values"]], tag.get("replace", False))


def set_tag(path, values):
    """Writes the mod's tag file at `path` whole: these entries, less what the tag holds already."""
    _tidy(path, list(values))


def prune_all():
    """Rewrites every tag file of the mod by the rule: no duplicates, nothing the tag holds already."""
    for namespace in sorted(os.listdir(DATA)):
        base = os.path.join(DATA, namespace, "tags")
        if not os.path.isdir(base):
            continue
        for folder, _, files in sorted(os.walk(base)):
            for file in sorted(files):
                if file.endswith(".json"):
                    path = os.path.relpath(os.path.join(folder, file), DATA)
                    tag = _read(os.path.join(DATA, path))
                    _tidy(path, tag.get("values", []), tag.get("replace", False))


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    use_vanilla(sys.argv[1])
    prune_all()
