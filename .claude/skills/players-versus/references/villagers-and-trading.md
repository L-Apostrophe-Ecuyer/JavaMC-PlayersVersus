# Villagers, Wandering Trader & Trading Economy

This doc covers the *mechanism* in full; the actual trade tables (`RevampedVillagerOffers`,
567 lines, and `RevampedWandererOffers`) are large data literals — read those files directly for
exact prices/counts per profession/level rather than trusting a transcription here.

## Design intent
A from-scratch replacement of vanilla's trades (data-driven trade sets on 26.3, which the mod's
`updateTrades` overrides never read) via a small custom factory DSL (`RevampedTradeFactories`),
covering every profession (Butcher, Farmer, Fisherman, Shepherd, Fletcher, Mason, Cleric, Librarian,
Armorer, plus a `// TODO` Cartographer that's already built out with the full explorer-map set) and
the Wandering Trader, tied into a reworked restock/leveling/gossip system on `Villager` itself.

## Trade factory DSL (`RevampedTradeFactories`)
A small hierarchy of `Factory` implementations (`BuyItemFactory`, `SellItemFactory`,
`ProcessItemFactory`, `SellEnchantedToolFactory`, `SellDyedArmorFactory`, `SellMapFactory`,
`SellPotionHoldingItemFactory`, `SellSusStewFactory`, `EnchantBookFactory`, `TypedWrapperFactory`, ...)
that each produce a vanilla `MerchantOffer` (or `null`, skipped) given the villager + a `RandomSource`.
`TypedWrapperFactory` is the key building block for **biome-flavored trades**: wraps a
`Map<ResourceKey<VillagerType>, Factory>` (or a varargs overload applying one factory to several
types) so a Desert villager's librarian offers differ from a Snow villager's, keyed off the
villager's `VillagerType` (biome-of-origin), using the per-biome enchantment-trade tags
(`EnchantmentTags.TRADES_<BIOME>_COMMON`, and `TradeKeys.TRADES_<BIOME>_SPECIAL`: 26.3 dropped the
special tags' and the armorer enchantment providers' constants, so `TradeKeys` names them and the mod
ships them as data under `data/minecraft/`) and structure tags (`StructureTags.ON_*_VILLAGE_MAPS`,
`ON_SWAMP_HUT_MAPS`, `ON_WOODLAND_MANSION_MAPS`, ...) for cartographer maps.
`createLibrarianTradeFactory`/`createMasterLibrarianTradeFactory` are the concrete librarian-tier
builders using this wrapper — **the template to copy** for any new profession/level that should vary
by villager biome type.

Gotchas:
- **A cost (`ItemCost`) matches by item, so it must name an item players can have.** A vanilla item
  the mod replaces (`VanillaItems`' replacement map: fermented spider eye, blaze powder, magma cream,
  clay, mud, ladders, copper gear, ...) can never pay for a trade, and its cost even shows the vanilla
  item (`ItemCost` builds its stack with the 3-argument `ItemStack` constructor, which `ItemStackMixin`
  doesn't swap). Name the replacement: the wandering trader buys corrupted wart powder, masons buy
  brown clay balls. Sold items are fine: `new ItemStack(vanillaItem)` becomes the replacement.
- **`SellDyedArmorFactory` dyes unconditionally.** 26.x has no dyeable item tag,
  `DyedItemColor.applyDyes` dyes any stack, and an undyed leather item has no `DYED_COLOR` to check
  for (the first 26.3 port checked `has(DYED_COLOR)`, and leatherworkers sold plain leather).
- **`SellPotionHoldingItemFactory`** (the fletcher's tipped arrows) draws a harmful potion the brewing
  stand can make: `brewablePotions` reads the server's brewing recipes, as 1.21's `isBrewable` did;
  Haunting is left out.

## `RevampedVillagerOffers.PROFESSION_TO_LEVELED_TRADE`
`Map<ResourceKey<VillagerProfession>, Int2ObjectMap<Factory[]>>` — profession → level (1-5) →
array of possible trade factories for that level. Populated per-profession via `get*Offers()`
static builders (`getButcherOffers`, `getFarmerOffers`, etc.) plus the inline Cartographer table.
**This map, not vanilla's trade sets, is what `VillagerEntityMixin.updateTrades` (override) actually
reads.**

## `VillagerEntityMixin` — restocking, leveling & gossip rework (target `Villager`)
26.3 levels a villager up the moment a trade (or gossip) gives it enough XP: no 40-tick timer that
waited for the trading screen to close, as 1.21 had. The mixin follows, and so must anything that
adds offers.
- **`updateTrades(ServerLevel)`** (full override, not `@Overwrite` — a real Java override of
  `AbstractVillager`'s abstract method, replacing `Villager`'s):
  picks `2 + max(0, 4 - level)` random, non-duplicate offers from the level's factory pool (skips a
  candidate if an equivalent sell-item+buy-item pair, or an enchanted book of the same enchantment at
  any level, is already offered — a book's enchantment is a stored one, which `isEnchanted` and
  `getEnchantments` don't read, hence `EnchantmentHelper.getEnchantmentsForCrafting`), and **inserts
  them in the right list position** relative to existing "conversion" offers (dual-buy-item trades)
  and "buy" offers (villager buys, i.e. sells emeralds) rather than just appending — conversion offers
  stay first, buy-for-emerald offers stay grouped after them, ordinary sell offers go last. Then, like
  vanilla's, it calls `updateSpecialPrices` for the player still trading, which reprices every offer
  for them and sends them the new list: without it a level-up mid-trade hides the new trades until the
  screen reopens, and the player picks (by index) from a list the insertions have shifted.
- **`talkWithVillager`** (`@Inject TAIL` on `gossip`): villager-to-villager socializing now grants trade
  **restocks and experience** based on **profession affinity** (`PROFESSION_AFFINITY_MAP` — e.g.
  Armorer↔{Weaponsmith, Leatherworker, Librarian}, Cleric↔{Nitwit, Librarian}): talking to a villager
  of your *own* profession gives a full restock + 9 affinity XP if you need restocking; talking to
  an affinity-listed profession gives 15 XP; any conversation grants a flat +3 XP on top and can
  trigger a level-up (with 10 s of Regeneration) if past threshold. This is a deliberate departure
  from vanilla's largely-cosmetic gossip system — professions now have an explicit, designed social
  graph that materially speeds up restocking and leveling, themed around real economic relationships
  (armorer needs weaponsmith/leatherworker materials info, cleric needs library research, etc.).
- **`restockTimeWithoutGossiping`** (`@ModifyConstant` on `allowedToRestock`'s `2400L`): base restock
  cooldown raised to 6000 ticks (5 minutes, vs. vanilla's 2 minutes) — restocking via idle time alone
  is slower, pushing players toward the trading-hall/gossip loop above rather than just waiting.
- **`rewardTradeXp`** (override): grants the offer's merchant XP, records the trading player as
  vanilla's `lastTradedPlayer` (the villager's next tick turns it into trade reputation, i.e. better
  prices, with happy particles) and levels up right away like vanilla, but villagers spawn
  `level × 8` XP orbs on a level-up trade (else the offer's XP) instead of vanilla's 3-6 — leveling up
  is worth noticeably more XP at higher villager levels.

## Wandering Trader (`RevampedWandererOffers`, `WanderingTraderEntityMixin`)
`WANDERING_TRADER_TRADES`: a flat `List<Pair<Factory[], Integer>>`, one pair per tier: from a tier
whose number is `n`, `WanderingTraderEntityMixin.updateTrades` (override) draws `n - 1` to `n + 1`
distinct trades. Notably includes **buy-side offers** (the trader *buys* Water Bottles, Water/Milk
Buckets, Corrupted Wart Powder (fermented spider eyes' replacement), Baked Potatoes, Hay Blocks for
emeralds) alongside an expanded sell catalog (all wood log types, Long Invisibility potion,
sea/aquatic goods, enchanted iron pickaxe).

**Settling down** (`notifyTrade` override, after vanilla's): after a trade with a player, by day
(`WorldTime.dayTime % 24000 <= 12000`: the clock isn't wrapped at a day) in a dimension with raids and
sky light, a trader in a village, or near the player's respawn point (32 blocks) with a job site
within 24 of the player (without a respawn point: two within 16), converts into a villager: 60%
unemployed, 20% cartographer, 10% fisherman, 10% farmer; 40% of the local biome's type, else an
"exotic" one by temperature. A shift-click keeps trading after the conversion, and the conversion of
a removed trader gives `null`, so it stops there.

## Client trading UI
`mixin/client/mobs/passive/VillagerTradingScreenMixin.java` (`extractLabels` TAIL: a tooltip,
`players-versus.tradeScreen.experienceBarHover`, over the XP bar) + `VillagerRessemblingModelMixin`
(model mixin, likely a villager-type/profession outfit-resemblance tweak) — not read this pass.

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
5. Name items players can have in costs: the mod's replacement, never a vanilla item it replaces
   (see the gotchas under the factory DSL).
