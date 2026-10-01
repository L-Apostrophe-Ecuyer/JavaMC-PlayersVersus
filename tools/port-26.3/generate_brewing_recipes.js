const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "../..");
const output = path.join(root, "src/main/resources/data/players-versus/recipe/brewing/generated");
const mc = name => `minecraft:${name}`;
const pv = name => `players-versus:${name}`;

const containers = {
  potion: mc("potion"),
  splash_potion: mc("splash_potion"),
  lingering_potion: mc("lingering_potion"),
};

const customPotions = [
  "darkness", "darkness_strong", "darkness_long", "night_vision_strong",
  "levitation", "levitation_strong", "levitation_long", "buoyancy",
  "buoyancy_strong", "buoyancy_long", "largeness", "largeness_strong",
  "largeness_long", "smallness", "smallness_strong", "smallness_long",
  "glowing", "glowing_strong", "glowing_long", "fire_resistance_strong",
  "decay", "decay_strong", "decay_long", "haste", "haste_strong",
  "haste_long", "mining_fatigue", "mining_fatigue_strong",
  "mining_fatigue_long", "vulnerability", "vulnerability_long", "haunting",
  "unluck",
];

const vanillaPotions = [
  "water", "thick", "awkward", "healing", "strong_healing", "harming",
  "strong_harming", "regeneration", "strong_regeneration", "long_regeneration",
  "turtle_master", "strong_turtle_master", "long_turtle_master", "night_vision",
  "long_night_vision", "leaping", "strong_leaping", "long_leaping",
  "slow_falling", "long_slow_falling", "swiftness", "strong_swiftness",
  "long_swiftness", "slowness", "long_slowness", "water_breathing",
  "long_water_breathing", "invisibility", "long_invisibility", "strength",
  "strong_strength", "long_strength", "weakness", "long_weakness",
  "fire_resistance", "long_fire_resistance", "wind_charged", "oozing",
  "infested", "poison", "strong_poison", "long_poison", "weaving", "luck",
];

const potions = Object.fromEntries([
  ...vanillaPotions.map(name => [name, mc(name)]),
  ...customPotions.map(name => [name, pv(name)]),
]);

const ingredientNames = new Set([
  "concentrate_of_death", "concentrate_of_health", "concentrate_of_harm",
  "concentrate_of_regeneration", "concentrate_of_decay",
  "concentrate_of_mining_speed", "concentrate_of_mining_fatigue",
  "concentrate_of_toughness", "concentrate_of_vulnerability",
  "concentrate_of_vision", "concentrate_of_darkness", "concentrate_of_leaping",
  "concentrate_of_slow_fall", "concentrate_of_levitation",
  "concentrate_of_speed", "concentrate_of_slowness", "concentrate_of_breath",
  "concentrate_of_buoyancy", "concentrate_of_largeness",
  "concentrate_of_smallness", "concentrate_of_invisibility",
  "concentrate_of_glowing", "concentrate_of_weakness", "concentrate_of_wind",
  "concentrate_of_ooze", "concentrate_of_infestation", "concentrate_of_poison",
  "concentrate_of_weaving", "concentrate_of_luck", "concentrate_of_unluck",
]);

const item = name => ingredientNames.has(name) ? pv(name) : mc(name);
const related = {
  healing: ["strong_healing", "regeneration", "harming"],
  harming: ["strong_harming", "decay", "healing"],
  regeneration: ["strong_regeneration", "long_regeneration", "decay"],
  decay: ["decay_strong", "decay_long", "regeneration"],
  haste: ["haste_strong", "haste_long", "mining_fatigue"],
  mining_fatigue: ["mining_fatigue_strong", "mining_fatigue_long", "haste"],
  turtle_master: ["strong_turtle_master", "long_turtle_master", "vulnerability"],
  vulnerability: ["mining_fatigue_strong", "vulnerability_long", "turtle_master"],
  darkness: ["darkness_strong", "darkness_long", "glowing"],
  night_vision: ["night_vision_strong", "long_night_vision", "darkness"],
  leaping: ["strong_leaping", "long_leaping", "slow_falling"],
  slow_falling: ["long_slow_falling", "long_slow_falling", "leaping"],
  levitation: ["levitation_strong", "levitation_long", "slow_falling"],
  swiftness: ["strong_swiftness", "long_swiftness", "slowness"],
  slowness: ["long_slowness", "long_slowness", "swiftness"],
  water_breathing: ["long_water_breathing", "long_water_breathing", "buoyancy"],
  buoyancy: ["buoyancy_strong", "buoyancy_long", "water_breathing"],
  largeness: ["largeness_strong", "largeness_long", "smallness"],
  smallness: ["smallness_strong", "smallness_long", "largeness"],
  invisibility: ["long_invisibility", "long_invisibility", "glowing"],
  glowing: ["glowing_strong", "glowing_long", "invisibility"],
  strength: ["strong_strength", "long_strength", "slowness"],
  weakness: [null, "long_weakness", "strength"],
  fire_resistance: ["fire_resistance_strong", "long_fire_resistance", "wind_charged"],
  wind_charged: [null, null, "fire_resistance"],
  oozing: [null, null, "infested"],
  infested: [null, null, "oozing"],
  poison: ["strong_poison", "long_poison", "weaving"],
  weaving: [null, null, "poison"],
  luck: ["luck", "luck", "unluck"],
  unluck: ["unluck", "unluck", "luck"],
};

const recipes = [];
const mix = (input, reagent, output) => recipes.push({ kind: "mix", input, reagent, output });
const container = (input, reagent, output) => recipes.push({ kind: "container", input, reagent, output });

mix("water", "glowstone_dust", "thick");
mix("water", "sugar", "awkward");
container("potion", "gunpowder", "splash_potion");
container("potion", "redstone", "lingering_potion");
container("lingering_potion", "gunpowder", "splash_potion");
container("splash_potion", "redstone", "lingering_potion");

const targets = [
  ["concentrate_of_death", "haunting"], ["concentrate_of_health", "healing"],
  ["concentrate_of_harm", "harming"], ["concentrate_of_regeneration", "regeneration"],
  ["concentrate_of_decay", "decay"], ["concentrate_of_mining_speed", "haste"],
  ["concentrate_of_mining_fatigue", "mining_fatigue"], ["concentrate_of_toughness", "turtle_master"],
  ["concentrate_of_vulnerability", "vulnerability"], ["concentrate_of_vision", "night_vision"],
  ["concentrate_of_darkness", "darkness"], ["concentrate_of_leaping", "leaping"],
  ["concentrate_of_slow_fall", "slow_falling"], ["concentrate_of_levitation", "levitation"],
  ["concentrate_of_speed", "swiftness"], ["concentrate_of_slowness", "slowness"],
  ["concentrate_of_breath", "water_breathing"], ["concentrate_of_buoyancy", "buoyancy"],
  ["concentrate_of_largeness", "largeness"], ["concentrate_of_smallness", "smallness"],
  ["concentrate_of_invisibility", "invisibility"], ["concentrate_of_glowing", "glowing"],
  ["blaze_powder", "strength"], ["concentrate_of_weakness", "weakness"],
  ["magma_cream", "fire_resistance"], ["concentrate_of_wind", "wind_charged"],
  ["concentrate_of_ooze", "oozing"], ["concentrate_of_infestation", "infested"],
  ["concentrate_of_poison", "poison"], ["concentrate_of_weaving", "weaving"],
  ["concentrate_of_luck", "luck"], ["concentrate_of_unluck", "unluck"],
];

for (const [ingredient, potion] of targets) {
  mix("water", ingredient, potion);
  mix(potion, "concentrate_of_decay", "decay");
  const relation = related[potion];
  if (!relation) continue;
  const [strong, long, inverted] = relation;
  if (long && long !== potion) {
    mix(potion, "sugar", long);
    mix("thick", ingredient, long);
    mix(long, "concentrate_of_decay", "decay_long");
  }
  if (strong && strong !== potion) {
    mix(potion, "glowstone_dust", strong);
    mix("awkward", ingredient, strong);
    mix(strong, "concentrate_of_decay", "decay_strong");
  }
  if (inverted && inverted !== potion) {
    mix(potion, "fermented_spider_eye", inverted);
    const invertedRelation = related[inverted];
    if (invertedRelation) {
      if (long && long !== potion) mix(long, "fermented_spider_eye", invertedRelation[1] || inverted);
      if (strong && strong !== potion) mix(strong, "fermented_spider_eye", invertedRelation[0] || inverted);
    }
  }
}

const potionInput = (containerId, potion) => ({
  item: containerId,
  potion_contents: { potions: potions[potion] },
});
const potionOutput = (containerId, potion) => ({
  components: { "minecraft:potion_contents": { potion: potions[potion] } },
  id: containerId,
});

fs.mkdirSync(output, { recursive: true });
for (const file of fs.readdirSync(output)) fs.unlinkSync(path.join(output, file));

let count = 0;
recipes.forEach((recipe, index) => {
  if (recipe.kind === "mix") {
    for (const [containerName, containerId] of Object.entries(containers)) {
      const data = {
        type: "minecraft:brewing",
        input: potionInput(containerId, recipe.input),
        output: potionOutput(containerId, recipe.output),
        reagent: { item: item(recipe.reagent) },
      };
      fs.writeFileSync(path.join(output, `graph_${String(index + 1).padStart(4, "0")}_${containerName}.json`), `${JSON.stringify(data, null, 2)}\n`);
      count++;
    }
  } else {
    for (const potion of Object.keys(potions)) {
      const data = {
        type: "minecraft:brewing",
        input: potionInput(mc(recipe.input), potion),
        output: potionOutput(mc(recipe.output), potion),
        reagent: { item: item(recipe.reagent) },
      };
      fs.writeFileSync(path.join(output, `graph_${String(index + 1).padStart(4, "0")}_${recipe.input}_${potion}_${recipe.reagent}_${recipe.output}.json`), `${JSON.stringify(data, null, 2)}\n`);
      count++;
    }
  }
});

console.log(`generated ${count} brewing recipes from ${recipes.length} graph entries`);
