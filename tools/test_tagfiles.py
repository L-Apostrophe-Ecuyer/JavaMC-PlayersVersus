#!/usr/bin/env python3
"""tools/tagfiles.py's rules, on tag files in a scratch folder: run from the repository root.

    python3 -m unittest tools/test_tagfiles.py
"""
import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import tagfiles


class TagFilesTest(unittest.TestCase):

    def setUp(self):
        self.scratch = tempfile.TemporaryDirectory()
        self.data = os.path.join(self.scratch.name, "data")
        self.vanilla = os.path.join(self.scratch.name, "vanilla")
        self.saved = tagfiles.DATA
        tagfiles.DATA = self.data
        tagfiles.use_vanilla(self.vanilla)

    def tearDown(self):
        tagfiles.DATA = self.saved
        tagfiles.use_vanilla(None)
        self.scratch.cleanup()

    def write(self, root, path, values):
        full = os.path.join(root, path)
        os.makedirs(os.path.dirname(full), exist_ok=True)
        with open(full, "w") as file:
            json.dump({"replace": False, "values": values}, file)

    def values(self, path):
        with open(os.path.join(self.data, path)) as file:
            return json.load(file)["values"]

    def test_a_tag_vanilla_lacks_keeps_its_file_when_it_holds_nothing(self):
        # A structure names #players-versus:has_structure/rock_formation: the tag must exist even empty
        mod = "players-versus/tags/worldgen/biome/has_structure/rock_formation.json"
        new = "minecraft/tags/worldgen/biome/has_new_thing.json"
        self.write(self.data, mod, [])
        self.write(self.data, new, [])
        tagfiles.prune_all()
        self.assertTrue(os.path.exists(os.path.join(self.data, mod)), "a tag only the mod has was deleted")
        self.assertTrue(os.path.exists(os.path.join(self.data, new)), "a minecraft tag vanilla lacks was deleted")
        self.assertEqual([], self.values(mod))

    def test_a_tag_vanilla_has_loses_a_file_that_adds_nothing(self):
        self.write(os.path.join(self.vanilla, "data"), "minecraft/tags/block/mud.json", ["minecraft:mud"])
        self.write(self.data, "minecraft/tags/block/mud.json", ["minecraft:mud"])
        tagfiles.prune_all()
        self.assertFalse(os.path.exists(os.path.join(self.data, "minecraft/tags/block/mud.json")))

    def test_an_entry_a_nested_tag_comes_to_hold_later_goes_by_the_end_of_the_run(self):
        # A generator adds a biome to the mineshafts (which nest #is_forest), then to #is_forest: by the end of its run
        # (prune_all), the mineshafts' own entry is gone, the biome still a mineshaft biome through #is_forest
        biome = "players-versus:new_forest"
        self.write(os.path.join(self.vanilla, "data"), "minecraft/tags/worldgen/biome/has_structure/mineshaft.json", ["#minecraft:is_forest"])
        self.write(os.path.join(self.vanilla, "data"), "minecraft/tags/worldgen/biome/is_forest.json", ["minecraft:forest"])
        tagfiles.add_to_tag("minecraft/tags/worldgen/biome/has_structure/mineshaft.json", biome)
        tagfiles.add_to_tag("minecraft/tags/worldgen/biome/is_forest.json", biome)
        tagfiles.prune_all()
        self.assertFalse(os.path.exists(os.path.join(self.data, "minecraft/tags/worldgen/biome/has_structure/mineshaft.json")))
        self.assertEqual([biome], self.values("minecraft/tags/worldgen/biome/is_forest.json"))


if __name__ == "__main__":
    unittest.main()
