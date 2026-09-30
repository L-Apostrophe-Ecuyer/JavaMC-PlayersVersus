# Villagers, Wandering Trader & Trading Economy

This doc covers the *mechanism* in full; the actual trade tables (`RevampedVillagerOffers`,
567 lines, and `RevampedWandererOffers`) are large data literals — read those files directly for
exact prices/counts per profession/level rather than trusting a transcription here.

## Design intent
A from-scratch replacement of vanilla's `TradeOffers` tables via a small custom factory DSL
(`RevampedTradeFactories`), covering every profession (Butcher, Farmer, Fisherman, Shepherd,
Fletcher, Mason, Cleric, Librarian, Armorer, plus a `// TODO` Cartographer that's already built out
with the full explorer-map set) and the Wandering Trader, tied into a reworked restock/leveling/
gossip system on `VillagerEntity` itself.

## Trade factory DSL (`RevampedTradeFactories`)
A small hierarchy of `Factory` implementations (seen: `BuyItemFactory`, `SellItemFactory`,
`SellEnchantedToolFactory`, `SellMapFactory`, `EnchantBookFactory`, `TypedWrapperFactory`) that each
produce a vanilla `TradeOffer` given a villager + `Random`. `TypedWrapperFactory` is the key
building block for **biome-flavored trades**: wraps a `Map<RegistryKey<VillagerType>, Factory>` (or
a varargs overload applying one factory to several types) so a Desert villager's librarian offers
differ from a Snow villager's, keyed off the villager's `VillagerType` (biome-of-origin), reusing
vanilla's per-biome enchantment-trade tags (`EnchantmentTags.{DESERT,JUNGLE,PLAINS,SAVANNA,SNOW,
SWAMP,TAIGA}_{COMMON,SPECIAL}_TRADE`) and structure tags (`StructureTags.ON_*_VILLAGE_MAPS`/
`ON_*_EXPLORER_MAPS`) for cartographer maps. `createLibrarianTradeFactory`/`createMasterLibrarianTradeFactory`
are the concrete librarian-tier builders using this wrapper — **the template to copy** for any new
profession/level that should vary by villager biome type.

## `RevampedVillagerOffers.PROFESSION_TO_LEVELED_TRADE`
`Map<RegistryKey<VillagerProfession>, Int2ObjectMap<Factory[]>>` — profession → level (1-5) →
array of possible trade factories for that level. Populated per-profession via `get*Offers()`
static builders (`getButcherOffers`, `getFarmerOffers`, etc.) plus the inline Cartographer table.
**This map, not vanilla's `TradeOffers.PROFESSION_TO_LEVELED_TRADE`, is what `VillagerEntityMixin.fillRecipes`
(override) actually reads.**

## `VillagerEntityMixin` — restocking, leveling & gossip rework (target `VillagerEntity`)
- **`fillRecipes`** (full override, not `@Overwrite` — a real Java override of a non-final method):
  picks `2 + max(0, 4 - level)` random, non-duplicate offers from the level's factory pool (skips a
  candidate if an equivalent sell-item+buy-item pair, or an enchanted book with the same enchant, is
  already offered), and **inserts them in the right list position** relative to existing "conversion"
  offers (dual-buy-item trades) and "buy" offers (villager buys, i.e. sells emeralds) rather than
  just appending — conversion offers stay first, buy-for-emerald offers stay grouped after them,
  ordinary sell offers go last.
- **`talkWithVillager`** (`@Inject TAIL`): villager-to-villager socializing now grants trade
  **restocks and experience** based on **profession affinity** (`PROFESSION_AFFINITY_MAP` — e.g.
  Armorer↔{Weaponsmith, Leatherworker, Librarian}, Cleric↔{Nitwit, Librarian}): talking to a villager
  of your *own* profession gives a full restock + 9 affinity XP if you need restocking; talking to
  an affinity-listed profession gives 15 XP; any conversation grants a flat +3 XP on top and can
  trigger a level-up if past threshold. This is a deliberate departure from vanilla's largely-cosmetic
  gossip system — professions now have an explicit, designed social graph that materially speeds up
  restocking and leveling, themed around real economic relationships (armorer needs weaponsmith/
  leatherworker materials info, cleric needs library research, etc.).
- **`restockTimeWithoutGossiping`** (`@ModifyConstant` on `canRestock`'s `2400L`): base restock
  cooldown raised to 6000 ticks (5 minutes, vs. vanilla's 2 minutes) — restocking via idle time alone
  is slower, pushing players toward the trading-hall/gossip loop above rather than just waiting.
- **`afterUsing`** (override): still grants merchant XP and possibly triggers level-up exactly like
  vanilla, but villagers now spawn `level × 8` XP orbs on a level-up trade instead of vanilla's flat
  amount — leveling up is worth noticeably more XP at higher villager levels.

## Wandering Trader (`RevampedWandererOffers`, `WanderingTraderEntityMixin`)
`WANDERING_TRADER_TRADES`: a flat `List<Pair<Factory[], Integer>>` (tier → weight-ish tier count,
second element is `2` for every entry seen so far — likely "number of trades drawn from this tier",
not a probability weight; verify against the mixin that consumes it before assuming otherwise).
Notably includes **new buy-side offers** (the trader now *buys* Water Bottles, Water/Milk Buckets,
Fermented Spider Eyes, Baked Potatoes, Hay Blocks for emeralds — vanilla's Wandering Trader has no
buy offers at all beyond leads/lead-related, this is a substantial economic addition) alongside an
expanded sell catalog (all wood log types, Long Invisibility potion, sea/aquatic goods, enchanted
iron pickaxe). `WanderingTraderEntityMixin` (not read this pass) is presumably where this list is
actually spliced into the entity's `fillRecipes` — check it directly before assuming the data list
alone is sufficient to change trader behavior.

## Client trading UI
`mixin/client/mobs/passive/VillagerTradingScreenMixin.java` + `VillagerRessemblingModelMixin` (model
mixin, likely a villager-type/profession outfit-resemblance tweak) — not read this pass.

## Extension recipe: adding/rebalancing a trade
1. For a new profession or level tier, add/extend a `get*Offers()` builder in
   `RevampedVillagerOffers` following the existing per-profession pattern (plain `Factory[]` for a
   biome-agnostic tier, `TypedWrapperFactory` for a biome-flavored one).
2. For a biome-specific variant of an existing trade, wrap it in `TypedWrapperFactory` rather than
   duplicating the whole level's array per biome.
3. If the new trade should participate in the profession-affinity gossip bonus, check whether the
   profession pair already exists in `VillagerEntityMixin.PROFESSION_AFFINITY_MAP` — add it if the
   new trade creates a new logical economic relationship worth rewarding.
4. Wandering Trader trades go in `RevampedWandererOffers.WANDERING_TRADER_TRADES` instead.
