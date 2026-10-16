const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..", "..", "src", "main", "resources", "data");
const apply = process.argv.includes("--apply");
const knownFlags = new Set(["--apply", "--check"]);
const unknownFlags = process.argv.slice(2).filter((arg) => !knownFlags.has(arg));
if (unknownFlags.length || (apply && process.argv.includes("--check"))) {
  console.error("Usage: node tools/port-26.3/convert_data.js [--check | --apply]");
  process.exit(2);
}

const featureRenames = new Map([
  ["minecraft:patch_dead_bush", "minecraft:dead_bush"],
  ["minecraft:patch_grass", "minecraft:grass"],
  ["minecraft:patch_grass_meadow", "minecraft:grass"],
]);
const recipeRenames = new Map([
  ["minecraft:cherry_raft", "minecraft:cherry_boat"],
  ["minecraft:cherry_chest_raft", "minecraft:cherry_chest_boat"],
  ["minecraft:spruce_raft", "minecraft:spruce_boat"],
  ["minecraft:spruce_chest_raft", "minecraft:spruce_chest_boat"],
]);
const unavailableRecipeIds = new Set([
  "players-versus:building_blocks/brown_mud",
  "players-versus:building_blocks/brown_mud_bricks",
  "players-versus:building_blocks/packed_mud_bricks",
]);
const explorationMapDestinations = new Map([
  ["minecraft:red_x", "minecraft:buried_treasure"],
  ["banner_black", "minecraft:mansion"],
  ["mansion", "minecraft:mansion"],
  ["minecraft:banner_black", "minecraft:mansion"],
  ["minecraft:woodland_mansion", "minecraft:mansion"],
]);

function renameFeatureReference(feature) {
  return featureRenames.get(feature) ?? feature;
}

function jsonFiles(directory) {
  if (!fs.existsSync(directory)) return [];
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const file = path.join(directory, entry.name);
    return entry.isDirectory() ? jsonFiles(file) : file.endsWith(".json") ? [file] : [];
  });
}

function randomPatchData(feature) {
  const config = feature.config ?? feature;
  if (config.tries === undefined || !config.feature?.feature || !Array.isArray(config.feature.placement)) return null;
  return {
    feature: config.feature.feature,
    placement: config.feature.placement,
    tries: config.tries,
    xzSpread: config.xz_spread ?? 0,
    ySpread: config.y_spread ?? 0,
  };
}

function randomPatchModifiers(patch) {
  const spread = (value) => value <= 0
    ? value
    : { type: "minecraft:uniform", min_inclusive: -value, max_inclusive: value };
  return [
    { type: "minecraft:count", count: patch.tries },
    {
      type: "minecraft:offset",
      x: spread(patch.xzSpread),
      y: spread(patch.ySpread),
      z: spread(patch.xzSpread),
    },
  ];
}

function blockState(state) {
  if (!state || typeof state !== "object" || Array.isArray(state)) return state;
  const result = {};
  if (state.id !== undefined) result.id = state.id;
  else if (state.Name !== undefined) result.id = state.Name;
  if (state.properties !== undefined) result.properties = state.properties;
  else if (state.Properties !== undefined) result.properties = state.Properties;
  return result;
}

function migrateBlockStateProvider(value) {
  if (value.type === "minecraft:simple_state_provider") return blockState(value.state);

  if (value.type === "minecraft:weighted_state_provider") {
    return {
      type: "minecraft:weighted",
      entries: value.entries.map(({ data, weight }) => ({ data: blockState(data), weight })),
    };
  }

  if (
    value.type === "minecraft:weighted"
    && Array.isArray(value.entries)
    && value.entries.length > 0
    && value.entries.every((entry) => entry.data?.type === "minecraft:rule_based")
  ) {
    const rules = value.entries[0].data.rules;
    if (value.entries.every((entry) => JSON.stringify(entry.data.rules) === JSON.stringify(rules))) {
      return {
        type: "minecraft:rule_based",
        fallback: {
          type: "minecraft:weighted",
          entries: value.entries.map(({ data, weight }) => ({ data: data.fallback, weight })),
        },
        rules,
      };
    }
  }

  if (value.fallback !== undefined && Array.isArray(value.rules)) {
    const rules = value.rules.map((rule) => ({
      if_true: rule.if_true,
      then: blockState(rule.then),
    }));
    const fallback = value.fallback;
    if (fallback.type === "minecraft:weighted_state_provider" || fallback.type === "minecraft:weighted") {
      return {
        type: "minecraft:rule_based",
        fallback: {
          type: "minecraft:weighted",
          entries: fallback.entries.map(({ data, weight }) => ({ data: blockState(data), weight })),
        },
        rules,
      };
    }
    return {
      type: "minecraft:rule_based",
      fallback: blockState(fallback.state ?? fallback),
      rules,
    };
  }

  if (value.type === "minecraft:noise_threshold_provider") {
    return {
      type: "minecraft:noise_threshold",
      seed: value.seed,
      noise: {
        base_amplitude: value.noise?.amplitudes?.[0] ?? 1,
        base_octave: value.noise?.firstOctave ?? 0,
      },
      scale: value.scale,
      threshold: value.threshold,
      high_chance: value.high_chance,
      default_state: blockState(value.default_state),
      low_states: value.low_states.map(blockState),
      high_states: value.high_states.map(blockState),
    };
  }

  if (value.type === "minecraft:noise_provider") {
    const states = value.states.map(blockState);
    const midpoint = Math.floor(states.length / 2);
    const noise = value.noise ?? {};
    return {
      type: "minecraft:noise_threshold",
      default_state: states[midpoint],
      high_chance: (states.length - midpoint - 1) / Math.max(1, states.length - 1),
      high_states: states.slice(midpoint + 1),
      low_states: states.slice(0, midpoint),
      noise: {
        base_amplitude: noise.amplitudes?.[0] ?? 1,
        base_octave: noise.firstOctave ?? 0,
      },
      scale: value.scale,
      seed: value.seed,
      threshold: 0,
    };
  }

  if (value.type === "minecraft:dual_noise_provider") {
    const noiseParameters = (noise) => ({
      base_amplitude: noise.amplitudes?.[0] ?? 1,
      base_octave: noise.firstOctave ?? 0,
    });
    return {
      type: "minecraft:dual_noise",
      seed: value.seed,
      noise: noiseParameters(value.noise),
      scale: value.scale,
      variety: value.variety,
      slow_noise: noiseParameters(value.slow_noise),
      slow_scale: value.slow_scale,
      states: value.states.map(blockState),
    };
  }

  if (value.type === "minecraft:rotated_block_provider") {
    return { type: "minecraft:rotated", source: migrateBlockStateProvider(value.source) };
  }

  if (value.type === "minecraft:randomized_int_state_provider") {
    return {
      type: "minecraft:randomized_int",
      property: value.property,
      source: migrateBlockStateProvider(value.source),
      values: value.values,
    };
  }

  return value;
}

function migratePredicate(value) {
  return visit(value, (object) => {
    if (object.predicate_type !== undefined) {
      object.type = object.predicate_type;
      delete object.predicate_type;
    }
    if (object.state !== undefined) object.state = blockState(object.state);
    if (object.block_state !== undefined) object.block_state = blockState(object.block_state);
    return object;
  });
}

function migrateEnchantment(value) {
  return visit(value, (object) => {
    if (object.condition !== undefined) {
      object.type = object.condition;
      delete object.condition;
    }
    if (Array.isArray(object.requirements)) {
      object.requirements = { type: "minecraft:all_of", terms: object.requirements };
    }
    if (Array.isArray(object.tags)) {
      for (const tag of object.tags) {
        if (typeof tag.id === "string" && !tag.id.startsWith("#")) tag.id = `#${tag.id}`;
      }
    }
    if (typeof object.type === "string" && (
      object.type.endsWith("_state_provider") || object.type === "minecraft:noise_provider"
    )) {
      return migrateBlockStateProvider(object);
    }
    if (object.Name !== undefined || object.Properties !== undefined) return blockState(object);
    if (object.predicate_type !== undefined) {
      object.type = object.predicate_type;
      delete object.predicate_type;
    }
    if (object.primary_items === "#minecraft:enchantable/sword") {
      object.primary_items = "#minecraft:enchantable/melee_weapon";
    }
    return object;
  });
}

function migratePlacement(value) {
  return visit(value, (object) => {
    if (object.predicate_type !== undefined) {
      object.type = object.predicate_type;
      delete object.predicate_type;
    }
    if (object.type === "minecraft:random_offset") {
      const offsetProvider = (spread) => {
        if (spread && typeof spread === "object") return spread;
        if (spread <= 0) return spread;
        return {
          type: "minecraft:uniform",
          min_inclusive: -spread,
          max_inclusive: spread,
        };
      };
      const xzSpread = object.xz_spread ?? 0;
      const ySpread = object.y_spread ?? 0;
      return {
        type: "minecraft:offset",
        x: offsetProvider(xzSpread),
        y: offsetProvider(ySpread),
        z: offsetProvider(xzSpread),
      };
    }
    if (object.type === "minecraft:offset") {
      for (const axis of ["x", "y", "z"]) {
        if (object[axis]?.type === "minecraft:constant") object[axis] = object[axis].value;
        if (
          object[axis]?.type === "minecraft:uniform"
          && object[axis].min_inclusive === null
          && object[axis].max_inclusive
          && typeof object[axis].max_inclusive === "object"
        ) {
          object[axis] = object[axis].max_inclusive;
        }
      }
    }
    if (object.type === "minecraft:block_predicate_filter" && object.predicate) {
      object.predicate = migratePredicate(object.predicate);
    }
    if (object.state !== undefined) object.state = blockState(object.state);
    if (object.block_state !== undefined) object.block_state = blockState(object.block_state);
    return object;
  });
}

function rgbColor(color) {
  if (typeof color === "string") return color;
  return `#${color.toString(16).padStart(6, "0")}`;
}

function visit(value, fn) {
  if (Array.isArray(value)) return value.map((item) => visit(item, fn));
  if (!value || typeof value !== "object") return value;
  const updated = {};
  for (const [key, child] of Object.entries(value)) updated[key] = visit(child, fn);
  return fn(updated);
}

function migrateConditionDiscriminator(object) {
  if (typeof object.condition !== "string" || Object.keys(object).length === 1) return object;
  object.type = object.condition;
  delete object.condition;
  return object;
}

function migrateLoot(value) {
  return visit(value, (object) => {
    if (Array.isArray(object.functions)) {
      const functions = object.functions;
      delete object.functions;
      object.modifier = functions.length === 1 ? functions[0] : functions;
    }
    if (Array.isArray(object.conditions)) {
      const conditions = object.conditions;
      delete object.conditions;
      if (conditions.length === 1) object.condition = conditions[0];
      else if (conditions.length > 1) object.condition = { type: "minecraft:all_of", terms: conditions };
    }
    migrateConditionDiscriminator(object);
    if (
      typeof object.type === "string"
      && object.type.startsWith("minecraft:")
      && object.condition?.type === "minecraft:block_state_property"
    ) {
      object.condition.type = "minecraft:match_block";
      object.condition.blocks = object.condition.block;
      delete object.condition.block;
      object.condition.state = object.condition.properties;
      delete object.condition.properties;
    }
    if (object.type === "minecraft:block_state_property") {
      object.type = "minecraft:match_block";
      object.blocks = object.block;
      delete object.block;
      object.state = object.properties;
      delete object.properties;
    }
    const sourceEntity = object.predicate?.source_entity;
    if (typeof sourceEntity?.type === "string") {
      sourceEntity.entity_type = sourceEntity.type;
      delete sourceEntity.type;
    }
    if (
      object.type === "minecraft:exploration_map"
      && (object.decoration !== undefined || object.destination !== undefined)
    ) {
      const oldDestination = object.decoration ?? object.destination;
      object.destination = explorationMapDestinations.get(oldDestination) ?? oldDestination;
      delete object.decoration;
    }
    if (
      ["count", "rolls", "bonus_rolls"].some((key) =>
        object[key]
        && typeof object[key] === "object"
        && object[key].type === undefined
        && object[key].min !== undefined
        && object[key].max !== undefined
      )
    ) {
      for (const key of ["count", "rolls", "bonus_rolls"]) {
        if (
          object[key]
          && typeof object[key] === "object"
          && object[key].type === undefined
          && object[key].min !== undefined
          && object[key].max !== undefined
        ) {
          object[key].type = "minecraft:uniform";
        }
      }
    }
    if (
      object.type === "minecraft:limit_count"
      && typeof object.limit === "number"
    ) {
      object.limit = { type: "minecraft:uniform", min: object.limit, max: object.limit };
    }
    if (typeof object.function === "string") {
      object.type = object.function;
      delete object.function;
    }
    return object;
  });
}

function migrateAdvancement(value) {
  const advancement = structuredClone(value);
  if (advancement.criteria?.has_used_bonemeal?.trigger === "minecraft:item_used_on_block") {
    delete advancement.criteria.has_used_bonemeal;
    advancement.requirements = advancement.requirements
      .map((requirement) => requirement.filter((name) => name !== "has_used_bonemeal"))
      .filter((requirement) => requirement.length > 0);
  }
  for (const criterion of Object.values(advancement.criteria ?? {})) {
    const conditions = criterion.conditions;
    if (!conditions || typeof conditions !== "object") continue;

    if (["minecraft:recipe_unlocked", "minecraft:recipe_crafted", "minecraft:crafter_recipe_crafted"].includes(criterion.trigger)) {
      if (conditions.recipe !== undefined) {
        conditions.recipes = conditions.recipe;
        delete conditions.recipe;
      }
      if (conditions.recipe_id !== undefined) {
        conditions.recipes = conditions.recipe_id;
        delete conditions.recipe_id;
      }
      if (conditions.recipes !== undefined) {
        const wasArray = Array.isArray(conditions.recipes);
        const recipes = (wasArray ? conditions.recipes : [conditions.recipes])
          .map((recipe) => recipeRenames.get(recipe) ?? recipe)
          .filter((recipe) => !unavailableRecipeIds.has(recipe));
        if (recipes.length === 0) criterion.removeForMissingRecipe = true;
        else conditions.recipes = wasArray ? recipes : recipes[0];
      }
    }

    criterion.conditions = visit(conditions, (object) => {
      migrateConditionDiscriminator(object);
      if (object.block !== undefined) {
        object.blocks = object.block;
        delete object.block;
      }
      return object;
    });
  }
  for (const [name, criterion] of Object.entries(advancement.criteria ?? {})) {
    if (criterion.removeForMissingRecipe) delete advancement.criteria[name];
  }
  if (Array.isArray(advancement.requirements)) {
    advancement.requirements = advancement.requirements
      .map((requirement) => requirement.filter((name) => advancement.criteria[name] !== undefined))
      .filter((requirement) => requirement.length > 0);
  }
  if (advancement.rewards?.recipes) {
    advancement.rewards.recipes = advancement.rewards.recipes
      .map((recipe) => recipeRenames.get(recipe) ?? recipe)
      .filter((recipe) => !unavailableRecipeIds.has(recipe));
    if (advancement.rewards.recipes.length === 0) delete advancement.rewards.recipes;
  }
  return advancement;
}

function migrateBiome(value) {
  const biome = structuredClone(value);
  const attributes = biome.attributes ?? {};

  if (biome.spawners !== undefined || biome.spawn_costs !== undefined) {
    const spawnSettings = {};
    if (biome.spawners !== undefined) {
      spawnSettings.spawns_by_category = {};
      for (const [category, spawns] of Object.entries(biome.spawners)) {
        spawnSettings.spawns_by_category[category] = spawns.map((spawn) => {
          const { minCount, maxCount, ...rest } = spawn;
          const count = minCount === maxCount
            ? minCount
            : { type: "minecraft:uniform", min_inclusive: minCount, max_inclusive: maxCount };
          return { ...rest, count };
        });
      }
    }
    spawnSettings.spawn_costs = biome.spawn_costs ?? {};
    attributes["minecraft:gameplay/natural_mob_spawns"] = { argument: spawnSettings, modifier: "overlay" };
    delete biome.spawners;
    delete biome.spawn_costs;
  }

  if (biome.creature_spawn_probability !== undefined) {
    attributes["minecraft:gameplay/creature_world_gen_spawn_probability"] = biome.creature_spawn_probability;
    delete biome.creature_spawn_probability;
  }

  const effects = biome.effects ?? {};
  const attributeNames = {
    sky_color: "minecraft:visual/sky_color",
    fog_color: "minecraft:visual/fog_color",
    water_fog_color: "minecraft:visual/water_fog_color",
  };
  for (const [field, name] of Object.entries(attributeNames)) {
    if (effects[field] !== undefined) {
      attributes[name] = rgbColor(effects[field]);
      delete effects[field];
    }
  }
  for (const field of ["water_color", "foliage_color", "dry_foliage_color", "grass_color"]) {
    if (effects[field] !== undefined) effects[field] = rgbColor(effects[field]);
  }
  if (effects.mood_sound !== undefined) {
    attributes["minecraft:audio/ambient_sounds"] = { mood: effects.mood_sound };
    delete effects.mood_sound;
  }
  if (effects.music_volume !== undefined) {
    attributes["minecraft:audio/music_volume"] = effects.music_volume;
    delete effects.music_volume;
  }
  if (effects.music !== undefined) {
    const music = effects.music;
    if (Array.isArray(music) && music.length > 0) {
      // 26.3 accepts one Music value per background-music situation, so retain the most common track.
      const primaryMusic = music.reduce((primary, candidate) =>
        candidate.weight > primary.weight ? candidate : primary
      );
      attributes["minecraft:audio/background_music"] = { default: primaryMusic.data };
    }
    delete effects.music;
  }
  biome.attributes = attributes;
  return biome;
}

function migrateFeature(value, relativePath, nestedVegetationFeature) {
  const randomPatch = randomPatchData(value);
  const feature = visit(value, (object) => {
    if (object.config && typeof object.config === "object") {
      Object.assign(object, object.config);
      delete object.config;
    }
    if (typeof object.feature === "string") object.feature = renameFeatureReference(object.feature);
    if (object.type === "minecraft:weighted") return migrateBlockStateProvider(object);
    if (typeof object.type === "string" && object.type.endsWith("_state_provider")) {
      return migrateBlockStateProvider(object);
    }
    if (
      object.type === "minecraft:noise_provider"
      || object.type === "minecraft:noise_threshold_provider"
      || object.type === "minecraft:dual_noise_provider"
    ) return migrateBlockStateProvider(object);
    if (object.fallback !== undefined && Array.isArray(object.rules)) {
      return migrateBlockStateProvider(object);
    }
    if (["minecraft:block_match", "minecraft:blockstate_match", "minecraft:random_block_match", "minecraft:tag_match"].includes(object.type)) {
      object.predicate_type = object.type;
      delete object.type;
    }
    if (
      object.predicate_type !== undefined
      && !["minecraft:block_match", "minecraft:blockstate_match", "minecraft:random_block_match", "minecraft:tag_match"].includes(object.type)
      && !["minecraft:block_match", "minecraft:blockstate_match", "minecraft:random_block_match", "minecraft:tag_match"].includes(object.predicate_type)
    ) {
      object.type = object.predicate_type;
      delete object.predicate_type;
    }
    if (object.type === "minecraft:lake") {
      object.can_place_feature ??= { type: "minecraft:true" };
      object.can_replace_with_air_or_fluid ??= {
        type: "minecraft:not",
        predicate: { type: "minecraft:matching_block_tag", tag: "minecraft:features_cannot_replace" },
      };
      object.can_replace_with_barrier ??= {
        type: "minecraft:not",
        predicate: { type: "minecraft:matching_block_tag", tag: "minecraft:lava_pool_stone_cannot_replace" },
      };
    }
    if (
      object.vegetation_feature?.feature
      && typeof object.vegetation_feature.feature === "object"
    ) {
      object.vegetation_feature.feature = nestedVegetationFeature;
    }
    const nestedPatch = object.feature && typeof object.feature === "object"
      ? randomPatchData(object.feature)
      : null;
    if (nestedPatch) {
      object.feature = nestedPatch.feature;
      object.placement = [
        ...(object.placement ?? []),
        ...randomPatchModifiers(nestedPatch),
        ...migratePlacement(nestedPatch.placement),
      ];
    }
    if (object.Name !== undefined || object.Properties !== undefined) return blockState(object);
    if (object.dirt_provider !== undefined) {
      object.below_trunk_provider = object.dirt_provider;
      delete object.dirt_provider;
    }
    return object;
  });
  if (randomPatch) return feature.feature.feature;
  if (!feature.type && feature.feature && Array.isArray(feature.placement)) return feature.feature;
  return feature;
}

function migrateProcessorList(value) {
  return visit(value, (object) => {
    if (object.output_state !== undefined) object.output_state = blockState(object.output_state);
    if (object.state !== undefined) object.state = blockState(object.state);
    if (object.block_state !== undefined) object.block_state = blockState(object.block_state);
    const processorPredicateTypes = new Set([
      "minecraft:always_true",
      "minecraft:block_match",
      "minecraft:blockstate_match",
      "minecraft:random_block_match",
      "minecraft:tag_match",
    ]);
    if (processorPredicateTypes.has(object.type)) {
      object.predicate_type = object.type;
      delete object.type;
    }
    return object;
  });
}

function migrateTrimMaterial(value) {
  const material = structuredClone(value);
  if (material.asset_name !== undefined) {
    material.palette_id = material.asset_name;
    delete material.asset_name;
  }
  return material;
}

function migrate(value, relativePath, sourceFiles, nestedVegetationFeatures) {
  const segments = relativePath.split(path.sep);
  const normalizedPath = relativePath.split(path.sep).join("/");
  if (segments[1] === "recipe") {
    const recipe = structuredClone(value);
    delete recipe.category;
    return recipe;
  }
  if (segments[1] === "loot_table") return migrateLoot(value);
  if (segments[1] === "advancement") return migrateAdvancement(value);
  if (segments[1] === "worldgen" && segments[2] === "biome") return migrateBiome(value);
  if (segments[1] === "enchantment") return migrateEnchantment(value);
  if (segments[1] === "trim_material") return migrateTrimMaterial(value);
  if (segments[1] === "worldgen" && segments[2] === "placed_feature") {
    const placedFeature = migratePlacement(value);
    if (typeof placedFeature.feature === "string") {
      placedFeature.feature = renameFeatureReference(placedFeature.feature);
    }
    const featurePath = normalizedPath.replace("/worldgen/placed_feature/", "/worldgen/feature/");
    const legacyFeature = value.feature && typeof value.feature === "object"
      ? value.feature
      : sourceFiles.get(featurePath.split("/").join(path.sep));
    const patch = legacyFeature && randomPatchData(legacyFeature);
    if (patch) {
      placedFeature.placement.push(...randomPatchModifiers(patch), ...migratePlacement(patch.placement));
    }
    if (value.feature && typeof value.feature === "object") {
      const [namespace, ...resourcePath] = featurePath.split("/");
      placedFeature.feature = `${namespace}:${resourcePath.slice(2).join("/").replace(/\.json$/, "")}`;
    }
    if (typeof placedFeature.feature === "string") {
      placedFeature.feature = placedFeature.feature.replace(/\.json$/, "");
    }
    return placedFeature;
  }
  if (segments[1] === "worldgen" && segments[2] === "processor_list") return migrateProcessorList(value);
  if (
    segments[1] === "worldgen"
    && (segments[2] === "configured_feature" || segments[2] === "feature")
  ) {
    return migrateFeature(value, normalizedPath, nestedVegetationFeatures.get(normalizedPath));
  }
  return value;
}

const changes = [];
const blockers = [];
const sources = new Map();
for (const file of jsonFiles(root)) {
  const relative = path.relative(root, file);
  let source;
  try {
    source = fs.readFileSync(file, "utf8");
    sources.set(relative, JSON.parse(source));
  } catch (error) {
    console.error(`${relative}: ${error.message}`);
    process.exitCode = 1;
  }
}
for (const [relative, value] of [...sources]) {
  const segments = relative.split(path.sep);
  if (
    segments[1] === "worldgen"
    && segments[2] === "placed_feature"
    && value.feature
    && typeof value.feature === "object"
  ) {
    const featurePath = relative.replace(
      `${path.sep}worldgen${path.sep}placed_feature${path.sep}`,
      `${path.sep}worldgen${path.sep}feature${path.sep}`,
    );
    if (!sources.has(featurePath)) sources.set(featurePath, value.feature);
  }
}
const nestedVegetationFeatures = new Map();
for (const [relative, value] of [...sources]) {
  const segments = relative.split(path.sep);
  if (
    segments[1] !== "worldgen"
    || segments[2] !== "feature"
    || !value.vegetation_feature?.feature
    || typeof value.vegetation_feature.feature !== "object"
  ) continue;

  const featurePath = relative.replace(/\.json$/, "_vegetation.json");
  const [namespace, ...resourcePath] = featurePath.split(path.sep);
  const featureId = `${namespace}:${resourcePath.slice(2).join("/").replace(/\.json$/, "")}`;
  if (!sources.has(featurePath)) sources.set(featurePath, value.vegetation_feature.feature);
  nestedVegetationFeatures.set(relative.split(path.sep).join("/"), featureId);
}
for (const [relative, parsed] of sources) {
  const file = path.join(root, relative);
  const exists = fs.existsSync(file);
  const source = exists ? fs.readFileSync(file, "utf8") : "";
  let converted;
  try {
    converted = migrate(parsed, relative, sources, nestedVegetationFeatures);
  } catch (error) {
    blockers.push(`${relative}: ${error.message}`);
    continue;
  }
  if (!exists || JSON.stringify(converted) !== JSON.stringify(parsed)) {
    let output = `${JSON.stringify(converted, null, 2)}\n`;
    if (source.includes("\r\n")) output = output.replaceAll("\n", "\r\n");
    changes.push({ file, relative, output });
    console.log(relative);
  }
}

if (apply) {
  for (const change of changes) {
    fs.mkdirSync(path.dirname(change.file), { recursive: true });
    fs.writeFileSync(change.file, change.output);
  }
  const namespaces = fs.readdirSync(root, { withFileTypes: true }).filter((entry) => entry.isDirectory());
  for (const namespace of namespaces) {
    const oldDirectory = path.join(root, namespace.name, "worldgen", "configured_feature");
    if (!fs.existsSync(oldDirectory)) continue;
    const newDirectory = path.join(root, namespace.name, "worldgen", "feature");
    fs.mkdirSync(newDirectory, { recursive: true });
    for (const file of jsonFiles(oldDirectory)) {
      const destination = path.join(newDirectory, path.relative(oldDirectory, file));
      fs.mkdirSync(path.dirname(destination), { recursive: true });
      fs.renameSync(file, destination);
    }
    fs.rmSync(oldDirectory, { recursive: true, force: true });
  }
  console.log(`Converted ${changes.length} JSON files and moved configured-feature registries.`);
} else {
  console.log(`${changes.length} JSON files need conversion. Use --apply to write changes.`);
}
for (const blocker of blockers) console.error(`BLOCKED: ${blocker}`);
if (blockers.length) process.exitCode = 1;
