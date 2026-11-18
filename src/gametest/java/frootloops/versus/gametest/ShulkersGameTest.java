package frootloops.versus.gametest;

import frootloops.versus.mod.items_and_effects.containers.ShulkerColors;
import frootloops.versus.mod.items_and_effects.containers.ShulkerItems;
import frootloops.versus.mod.items_and_effects.containers.ShulkerPeeking;
import frootloops.versus.mod.items_and_effects.containers.ShulkerSelection;
import frootloops.versus.mod.items_and_effects.containers.ShulkerTooltip;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.LockCode;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.TooltipFlag;
import java.util.Optional;

import java.util.List;
import java.util.function.Consumer;

/**
 * Bundles and shulker boxes: the bundle's grid of 5 by 4; a shulker box's tooltip of its slots, scrolled through, taken
 * from and put into in the inventory; the peek at a placed box.
 */
public class ShulkersGameTest {

    private static ItemStack bundleOf(int kinds) {
        List<ItemStackTemplate> items = BuiltInRegistries.ITEM.stream().filter(item -> item != Items.AIR).limit(kinds)
                .map(item -> ItemStackTemplate.fromNonEmptyStack(new ItemStack(item))).toList();
        ItemStack bundle = new ItemStack(Items.BUNDLE);
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(items));
        return bundle;
    }

    @GameTest
    public void aBundleShowsNineteenOfMany(GameTestHelper helper) {
        helper.assertValueEqual(BundleItem.getNumberOfItemsToShow(bundleOf(25)), 19, "items shown of 25");
        helper.assertValueEqual(BundleItem.getNumberOfItemsToShow(bundleOf(20)), 20, "items shown of 20");
        helper.assertValueEqual(BundleItem.getNumberOfItemsToShow(bundleOf(13)), 13, "items shown of 13");
        helper.succeed();
    }

    // The inventory menu's slots: 9 and 10 are the main inventory's first two
    private static final int BOX_SLOT = 9, OTHER_SLOT = 10, LEFT = 0, RIGHT = 1;

    /** A shulker box holding these (slot, item, count) triples. */
    private static ItemStack box(Object... contents) {
        NonNullList<ItemStack> slots = NonNullList.withSize(ShulkerItems.SIZE, ItemStack.EMPTY);
        for (int at = 0; at < contents.length; at += 3) slots.set((Integer) contents[at], new ItemStack((Item) contents[at + 1], (Integer) contents[at + 2]));
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(slots));
        return box;
    }

    private static NonNullList<ItemStack> slotsOf(ItemStack box) {
        return ShulkerItems.slots(box.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
    }

    private static void assertHolds(GameTestHelper helper, ItemStack stack, Item item, int count, String what) {
        helper.assertTrue(stack.is(item) && stack.getCount() == count, what + " should be " + count + " " + item + ", was " + stack);
    }

    /** The test with a player on the server (the inventory menu's crafting grid wants one), who leaves after. */
    private static void withPlayer(GameTestHelper helper, Consumer<ServerPlayer> test) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            test.accept(player);
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    private static void click(ServerPlayer player, int slot, int button) {
        player.containerMenu.clicked(slot, button, ContainerInput.PICKUP, player);
    }

    @GameTest
    public void aCarriedItemGoesIntoTheBox(GameTestHelper helper) {
        withPlayer(helper, player -> {
            AbstractContainerMenu menu = player.containerMenu;
            menu.getSlot(BOX_SLOT).set(box(0, Items.DIRT, 5, 4, Items.STONE, 60));
            menu.setCarried(new ItemStack(Items.STONE, 10));
            click(player, BOX_SLOT, LEFT);
            helper.assertTrue(menu.getCarried().isEmpty(), "the stone carried should all have gone in, was " + menu.getCarried());
            NonNullList<ItemStack> slots = slotsOf(menu.getSlot(BOX_SLOT).getItem());
            assertHolds(helper, slots.get(4), Items.STONE, 64, "the box's stone stack");
            assertHolds(helper, slots.get(1), Items.STONE, 6, "the box's first empty slot");
            assertHolds(helper, slots.get(0), Items.DIRT, 5, "the box's dirt");
        });
    }

    @GameTest
    public void aCarriedBoxTakesInTheClickedItem(GameTestHelper helper) {
        withPlayer(helper, player -> {
            AbstractContainerMenu menu = player.containerMenu;
            menu.getSlot(OTHER_SLOT).set(new ItemStack(Items.APPLE, 7));
            menu.setCarried(box(3, Items.APPLE, 5));
            click(player, OTHER_SLOT, LEFT);
            helper.assertTrue(menu.getSlot(OTHER_SLOT).getItem().isEmpty(), "the apples should have gone into the box");
            helper.assertTrue(menu.getCarried().is(Items.SHULKER_BOX), "the box should still be carried, was " + menu.getCarried());
            assertHolds(helper, slotsOf(menu.getCarried()).get(3), Items.APPLE, 12, "the box's apples");
        });
    }

    @GameTest
    public void theSelectedItemComesOutOnRightClick(GameTestHelper helper) {
        withPlayer(helper, player -> {
            AbstractContainerMenu menu = player.containerMenu;
            menu.getSlot(BOX_SLOT).set(box(4, Items.DIAMOND, 3, 7, Items.EMERALD, 9));
            ShulkerSelection.selectInMenu(menu, BOX_SLOT, 7);
            helper.assertValueEqual(ShulkerSelection.selected(menu.getSlot(BOX_SLOT).getItem()), 7, "the selection");
            click(player, BOX_SLOT, RIGHT);
            assertHolds(helper, menu.getCarried(), Items.EMERALD, 9, "the cursor");
            ItemStack box = menu.getSlot(BOX_SLOT).getItem();
            helper.assertTrue(box.is(Items.SHULKER_BOX), "the box should stay in its slot, was " + box);
            helper.assertTrue(slotsOf(box).get(7).isEmpty(), "the emeralds should be out of the box");
            assertHolds(helper, slotsOf(box).get(4), Items.DIAMOND, 3, "the box's diamonds");
            helper.assertValueEqual(ShulkerSelection.selected(box), -1, "the selection after taking");
        });
    }

    @GameTest
    public void withNothingSelectedRightClickPicksUpTheBox(GameTestHelper helper) {
        withPlayer(helper, player -> {
            AbstractContainerMenu menu = player.containerMenu;
            menu.getSlot(BOX_SLOT).set(box(4, Items.DIAMOND, 3));
            click(player, BOX_SLOT, RIGHT);
            helper.assertTrue(menu.getCarried().is(Items.SHULKER_BOX), "the box should be picked up, the cursor was " + menu.getCarried());
            assertHolds(helper, slotsOf(menu.getCarried()).get(4), Items.DIAMOND, 3, "the box's diamonds");
            helper.assertTrue(menu.getSlot(BOX_SLOT).getItem().isEmpty(), "the slot should be empty");
        });
    }

    @GameTest
    public void aSelectionOfAnEmptySlotIsNone(GameTestHelper helper) {
        withPlayer(helper, player -> {
            AbstractContainerMenu menu = player.containerMenu;
            menu.getSlot(BOX_SLOT).set(box(4, Items.DIAMOND, 3));
            ShulkerSelection.selectInMenu(menu, BOX_SLOT, 5);
            helper.assertValueEqual(ShulkerSelection.selected(menu.getSlot(BOX_SLOT).getItem()), -1, "the selection of an empty slot");
            ShulkerSelection.selectInMenu(menu, BOX_SLOT, 40);
            helper.assertValueEqual(ShulkerSelection.selected(menu.getSlot(BOX_SLOT).getItem()), -1, "the selection of a slot past the box's");
        });
    }

    @GameTest
    public void aBoxNeverGoesIntoABox(GameTestHelper helper) {
        withPlayer(helper, player -> {
            AbstractContainerMenu menu = player.containerMenu;
            menu.getSlot(BOX_SLOT).set(box(0, Items.DIRT, 5));
            ItemStack red = new ItemStack(Items.DYED_SHULKER_BOX.pick(DyeColor.RED));
            menu.setCarried(red);
            click(player, BOX_SLOT, LEFT);
            helper.assertTrue(menu.getSlot(BOX_SLOT).getItem().is(Items.DYED_SHULKER_BOX.pick(DyeColor.RED)), "the two boxes should swap, the slot held " + menu.getSlot(BOX_SLOT).getItem());
            helper.assertTrue(menu.getCarried().is(Items.SHULKER_BOX), "the two boxes should swap, the cursor held " + menu.getCarried());
            assertHolds(helper, slotsOf(menu.getCarried()).get(0), Items.DIRT, 5, "the box's dirt");
            // And the other way: the box carried onto a box
            click(player, BOX_SLOT, LEFT);
            helper.assertTrue(menu.getCarried().is(Items.DYED_SHULKER_BOX.pick(DyeColor.RED)), "the boxes should swap back, the cursor held " + menu.getCarried());
        });
    }

    @GameTest
    public void aFullBoxSwaps(GameTestHelper helper) {
        withPlayer(helper, player -> {
            AbstractContainerMenu menu = player.containerMenu;
            Object[] full = new Object[ShulkerItems.SIZE * 3];
            for (int slot = 0; slot < ShulkerItems.SIZE; slot++) {
                full[slot * 3] = slot;
                full[slot * 3 + 1] = Items.STONE;
                full[slot * 3 + 2] = 64;
            }
            menu.getSlot(BOX_SLOT).set(box(full));
            menu.setCarried(new ItemStack(Items.STONE, 10));
            click(player, BOX_SLOT, LEFT);
            assertHolds(helper, menu.getSlot(BOX_SLOT).getItem(), Items.STONE, 10, "the slot, swapped");
            helper.assertTrue(menu.getCarried().is(Items.SHULKER_BOX), "the full box should be on the cursor, was " + menu.getCarried());
            // And the full box carried onto stone: they swap again
            click(player, BOX_SLOT, LEFT);
            helper.assertTrue(menu.getSlot(BOX_SLOT).getItem().is(Items.SHULKER_BOX), "the full box should be back in the slot");
        });
    }

    @GameTest
    public void aStackOfBoxesIsLeftAlone(GameTestHelper helper) {
        withPlayer(helper, player -> {
            AbstractContainerMenu menu = player.containerMenu;
            ItemStack boxes = box(0, Items.DIRT, 5);
            boxes.setCount(2);
            menu.getSlot(BOX_SLOT).set(boxes);
            menu.setCarried(new ItemStack(Items.STONE, 10));
            click(player, BOX_SLOT, LEFT);
            helper.assertTrue(menu.getCarried().is(Items.SHULKER_BOX) && menu.getCarried().getCount() == 2, "the stack of boxes should be swapped, the cursor held " + menu.getCarried());
            helper.assertTrue(slotsOf(menu.getCarried()).get(1).isEmpty(), "nothing should go into a stack of boxes");
        });
    }

    // Putting into and taking out of a box's 27 slots (ShulkerItems; item stacks need the game's components)

    private static NonNullList<ItemStack> emptySlots() {
        return NonNullList.withSize(ShulkerItems.SIZE, ItemStack.EMPTY);
    }

    @GameTest
    public void anItemGoesOntoItsOwnStacksFirstThenTheFirstEmptySlots(GameTestHelper helper) {
        NonNullList<ItemStack> slots = emptySlots();
        slots.set(4, new ItemStack(Items.STONE, 60));
        slots.set(0, new ItemStack(Items.DIRT, 5));
        ItemStack from = new ItemStack(Items.STONE, 10);
        helper.assertValueEqual(ShulkerItems.insert(slots, from), 10, "stone put in");
        helper.assertTrue(from.isEmpty(), "what went in should be taken from the stack it came from");
        assertHolds(helper, slots.get(4), Items.STONE, 64, "the stone stack");
        assertHolds(helper, slots.get(1), Items.STONE, 6, "the first empty slot");
        assertHolds(helper, slots.get(0), Items.DIRT, 5, "the dirt");
        helper.succeed();
    }

    @GameTest
    public void stacksKeepToTheirSize(GameTestHelper helper) {
        NonNullList<ItemStack> slots = emptySlots();
        int most = new ItemStack(Items.OAK_SIGN).getMaxStackSize();
        helper.assertTrue(most < 20, "signs should stack to fewer than 20");
        helper.assertValueEqual(ShulkerItems.insert(slots, new ItemStack(Items.OAK_SIGN, 2 * most + 3)), 2 * most + 3, "signs put in");
        assertHolds(helper, slots.get(0), Items.OAK_SIGN, most, "slot 0");
        assertHolds(helper, slots.get(1), Items.OAK_SIGN, most, "slot 1");
        assertHolds(helper, slots.get(2), Items.OAK_SIGN, 3, "slot 2");
        helper.succeed();
    }

    @GameTest
    public void aShulkerBoxNeverFitsInOne(GameTestHelper helper) {
        helper.assertFalse(ShulkerItems.fits(new ItemStack(Items.SHULKER_BOX)), "a shulker box fits");
        helper.assertFalse(ShulkerItems.fits(new ItemStack(Items.DYED_SHULKER_BOX.pick(DyeColor.RED))), "a red shulker box fits");
        helper.assertFalse(ShulkerItems.fits(ItemStack.EMPTY), "nothing fits");
        helper.assertTrue(ShulkerItems.fits(new ItemStack(Items.BUNDLE)), "a bundle should fit");
        NonNullList<ItemStack> slots = emptySlots();
        helper.assertValueEqual(ShulkerItems.insert(slots, new ItemStack(Items.SHULKER_BOX)), 0, "shulker boxes put in");
        helper.assertTrue(slots.stream().allMatch(ItemStack::isEmpty), "the box should stay empty");
        helper.succeed();
    }

    @GameTest
    public void aFullBoxTakesNothing(GameTestHelper helper) {
        NonNullList<ItemStack> slots = emptySlots();
        for (int slot = 0; slot < ShulkerItems.SIZE; slot++) slots.set(slot, new ItemStack(Items.STONE, 64));
        ItemStack from = new ItemStack(Items.STONE, 10);
        helper.assertValueEqual(ShulkerItems.room(slots, from), 0, "the room");
        helper.assertValueEqual(ShulkerItems.insert(slots, from), 0, "stone put in");
        helper.assertValueEqual(from.getCount(), 10, "the stone left");
        helper.succeed();
    }

    @GameTest
    public void theRoomIsWhatWouldGoIn(GameTestHelper helper) {
        NonNullList<ItemStack> slots = emptySlots();
        for (int slot = 0; slot < ShulkerItems.SIZE - 1; slot++) slots.set(slot, new ItemStack(Items.STONE, 64));
        slots.set(3, new ItemStack(Items.STONE, 60));
        helper.assertValueEqual(ShulkerItems.room(slots, new ItemStack(Items.STONE, 100)), 68, "the room for 100 stone");
        helper.assertValueEqual(ShulkerItems.room(slots, new ItemStack(Items.DIRT, 64)), 64, "the room for 64 dirt");
        helper.succeed();
    }

    @GameTest
    public void takingEmptiesTheSlot(GameTestHelper helper) {
        NonNullList<ItemStack> slots = emptySlots();
        slots.set(7, new ItemStack(Items.DIAMOND, 3));
        assertHolds(helper, ShulkerItems.take(slots, 7), Items.DIAMOND, 3, "what was taken");
        helper.assertTrue(slots.get(7).isEmpty(), "the slot should be empty");
        helper.assertTrue(ShulkerItems.take(slots, 7).isEmpty(), "an empty slot gives nothing");
        helper.assertTrue(ShulkerItems.take(slots, 99).isEmpty(), "a slot past the box's gives nothing");
        helper.assertTrue(ShulkerItems.take(slots, -1).isEmpty(), "no slot gives nothing");
        helper.succeed();
    }

    // The box's tooltip: its slots as a grid, not as lines of text

    private static List<String> tooltipKeys(GameTestHelper helper, ItemStack stack) {
        return stack.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), null, TooltipFlag.NORMAL).stream()
                .map(Component::getContents).filter(TranslatableContents.class::isInstance).map(contents -> ((TranslatableContents) contents).getKey()).toList();
    }

    @GameTest
    public void aShulkerBoxsTooltipIsItsSlots(GameTestHelper helper) {
        ItemStack box = box(4, Items.DIAMOND, 3, 7, Items.EMERALD, 9);
        ShulkerSelection.select(box, 7);
        Optional<TooltipComponent> image = box.getTooltipImage();
        helper.assertTrue(image.isPresent() && image.get() instanceof ShulkerTooltip, "the box's tooltip image should be its slots, was " + image);
        ShulkerTooltip tooltip = (ShulkerTooltip) image.get();
        helper.assertValueEqual(tooltip.slots().size(), ShulkerItems.SIZE, "the slots shown");
        assertHolds(helper, tooltip.slots().get(4), Items.DIAMOND, 3, "slot 4");
        assertHolds(helper, tooltip.slots().get(7), Items.EMERALD, 9, "slot 7");
        for (int slot = 0; slot < ShulkerItems.SIZE; slot++) {
            if (slot != 4 && slot != 7) helper.assertTrue(tooltip.slots().get(slot).isEmpty(), "slot " + slot + " should be empty");
        }
        helper.assertValueEqual(tooltip.selected(), 7, "the selection shown");
        helper.succeed();
    }

    @GameTest
    public void anEmptyBoxHasNoTooltipImage(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(Items.SHULKER_BOX).getTooltipImage().isEmpty(), "an empty box should show no grid");
        ItemStack hidden = box(0, Items.DIRT, 1);
        hidden.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.CONTAINER, true));
        helper.assertTrue(hidden.getTooltipImage().isEmpty(), "a box that hides its contents should show no grid");
        helper.succeed();
    }

    @GameTest
    public void aBoxsContentsAreNotAlsoListedAsText(GameTestHelper helper) {
        List<String> keys = tooltipKeys(helper, box(0, Items.DIRT, 1, 1, Items.STONE, 2, 2, Items.APPLE, 3, 3, Items.BREAD, 4, 4, Items.COAL, 5, 5, Items.FLINT, 6));
        helper.assertFalse(keys.contains("item.container.item_count") || keys.contains("item.container.more_items"), "the box's contents listed as text: " + keys);
        helper.succeed();
    }

    @GameTest
    public void aChestStillListsItsContents(GameTestHelper helper) {
        ItemStack chest = new ItemStack(Items.CHEST);
        chest.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIRT, 4))));
        helper.assertTrue(tooltipKeys(helper, chest).contains("item.container.item_count"), "a chest should still list its contents");
        helper.assertTrue(chest.getTooltipImage().isEmpty(), "a chest should show no grid");
        helper.succeed();
    }

    // The peek: what the server shows of a placed box, and lets be taken

    private static final BlockPos BOX = new BlockPos(1, 1, 1);

    /** A shulker box placed at this spot of the test, holding these (slot, item, count) triples; its place in the world. */
    private static BlockPos placeBox(GameTestHelper helper, BlockPos at, Object... contents) {
        helper.setBlock(at, Blocks.SHULKER_BOX);
        ShulkerBoxBlockEntity box = helper.getBlockEntity(at, ShulkerBoxBlockEntity.class);
        for (int i = 0; i < contents.length; i += 3) box.setItem((Integer) contents[i], new ItemStack((Item) contents[i + 1], (Integer) contents[i + 2]));
        return helper.absolutePos(at);
    }

    /** The player standing this far south of the box, facing it. */
    private static void standBy(ServerPlayer player, BlockPos box, double distance) {
        player.snapTo(box.getX() + 0.5, box.getY(), box.getZ() + 0.5 + distance, 180.0F, 20.0F);
    }

    private static ShulkerBoxBlockEntity boxAt(GameTestHelper helper, BlockPos absolute) {
        return (ShulkerBoxBlockEntity) helper.getLevel().getBlockEntity(absolute);
    }

    @GameTest
    public void aPeekShowsAnOpenableBox(GameTestHelper helper) {
        BlockPos box = placeBox(helper, BOX, 2, Items.DIAMOND, 3);
        withPlayer(helper, player -> {
            standBy(player, box, 2.0);
            Optional<ItemContainerContents> contents = ShulkerPeeking.contents(player, box);
            helper.assertTrue(contents.isPresent(), "a box in reach should be shown");
            assertHolds(helper, ShulkerItems.slots(contents.get()).get(2), Items.DIAMOND, 3, "slot 2 shown");
        });
    }

    @GameTest
    public void aPeekShowsNothingLockedOutOfReachOrUnrolled(GameTestHelper helper) {
        BlockPos box = placeBox(helper, BOX, 2, Items.DIAMOND, 3);
        BlockPos unrolled = placeBox(helper, new BlockPos(3, 1, 1));
        withPlayer(helper, player -> {
            standBy(player, box, 9.0);
            helper.assertTrue(ShulkerPeeking.contents(player, box).isEmpty(), "a box out of reach should show nothing");
            standBy(player, box, 2.0);
            ItemStack lock = new ItemStack(Items.SHULKER_BOX);
            lock.set(DataComponents.LOCK, new LockCode(ItemPredicate.Builder.item().of(helper.getLevel().registryAccess().lookupOrThrow(Registries.ITEM), Items.NETHER_STAR).build()));
            boxAt(helper, box).applyComponentsFromItemStack(lock);
            boxAt(helper, box).setItem(2, new ItemStack(Items.DIAMOND, 3));
            helper.assertTrue(ShulkerPeeking.contents(player, box).isEmpty(), "a box locked to a key the player hasn't should show nothing");
            helper.assertValueEqual(ShulkerPeeking.take(player, box, 2, new ItemStack(Items.DIAMOND)), 0, "taken from a locked box");
            boxAt(helper, unrolled).setLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
            standBy(player, unrolled, 2.0);
            helper.assertTrue(ShulkerPeeking.contents(player, unrolled).isEmpty(), "a box with a loot table still to roll should show nothing");
            helper.assertTrue(boxAt(helper, unrolled).getLootTable() != null, "peeking shouldn't roll the loot table");
        });
    }

    @GameTest
    public void aPeekShowsNothingWithTheLidBlocked(GameTestHelper helper) {
        BlockPos box = placeBox(helper, BOX, 2, Items.DIAMOND, 3);
        helper.setBlock(BOX.above(), Blocks.STONE);
        withPlayer(helper, player -> {
            standBy(player, box, 2.0);
            helper.assertTrue(ShulkerPeeking.contents(player, box).isEmpty(), "a box that can't open should show nothing");
            helper.assertValueEqual(ShulkerPeeking.take(player, box, 2, new ItemStack(Items.DIAMOND)), 0, "taken from a box that can't open");
        });
    }

    @GameTest
    public void takingMovesTheStackIntoTheInventory(GameTestHelper helper) {
        BlockPos box = placeBox(helper, BOX, 2, Items.DIAMOND, 3, 5, Items.EMERALD, 7);
        withPlayer(helper, player -> {
            standBy(player, box, 2.0);
            helper.assertValueEqual(ShulkerPeeking.take(player, box, 2, new ItemStack(Items.DIAMOND)), 3, "diamonds taken");
            helper.assertTrue(boxAt(helper, box).getItem(2).isEmpty(), "the box's diamonds should be gone");
            helper.assertValueEqual(player.getInventory().countItem(Items.DIAMOND), 3, "diamonds in the inventory");
            assertHolds(helper, boxAt(helper, box).getItem(5), Items.EMERALD, 7, "the box's emeralds");
        });
    }

    @GameTest
    public void takingFillsTheSlotTheEmptyHandLeavesFree(GameTestHelper helper) {
        // The main hand is empty (a peek needs it), so even an otherwise full inventory has its one slot: the whole stack
        // goes there, none lost and none left
        BlockPos box = placeBox(helper, BOX, 2, Items.DIAMOND, 64);
        withPlayer(helper, player -> {
            standBy(player, box, 2.0);
            for (int slot = 1; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            helper.assertValueEqual(ShulkerPeeking.take(player, box, 2, new ItemStack(Items.DIAMOND)), 64, "diamonds taken");
            helper.assertTrue(boxAt(helper, box).getItem(2).isEmpty(), "the box's diamonds should be gone");
            helper.assertValueEqual(player.getInventory().countItem(Items.DIAMOND), 64, "diamonds in the inventory");
            helper.assertTrue(player.getInventory().getItem(0).is(Items.DIAMOND), "the diamonds should be in the held slot");
        });
    }

    @GameTest
    public void takingTheWrongItemTakesNothing(GameTestHelper helper) {
        BlockPos box = placeBox(helper, BOX, 2, Items.DIAMOND, 3);
        withPlayer(helper, player -> {
            standBy(player, box, 2.0);
            helper.assertValueEqual(ShulkerPeeking.take(player, box, 2, new ItemStack(Items.EMERALD)), 0, "taken when the slot holds something else");
            helper.assertValueEqual(ShulkerPeeking.take(player, box, 5, new ItemStack(Items.DIAMOND)), 0, "taken from an empty slot");
            helper.assertValueEqual(ShulkerPeeking.take(player, box, 40, new ItemStack(Items.DIAMOND)), 0, "taken from a slot past the box's");
            standBy(player, box, 9.0);
            helper.assertValueEqual(ShulkerPeeking.take(player, box, 2, new ItemStack(Items.DIAMOND)), 0, "taken out of reach");
            assertHolds(helper, boxAt(helper, box).getItem(2), Items.DIAMOND, 3, "the box's diamonds");
        });
    }

    // After the final review: a selection never outlives its menu or its box's leaving the slot; the peek keeps to where
    // the player may use blocks

    @GameTest
    public void aSelectionDoesNotOutliveItsMenu(GameTestHelper helper) {
        BlockPos chestAt = new BlockPos(2, 1, 1);
        helper.setBlock(chestAt, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(chestAt, ChestBlockEntity.class);
        chest.setItem(0, box(4, Items.DIAMOND, 3));
        withPlayer(helper, player -> {
            standBy(player, helper.absolutePos(chestAt), 2.0);
            // The player's own inventory, closed with a box selected
            player.containerMenu.getSlot(BOX_SLOT).set(box(4, Items.EMERALD, 5));
            ShulkerSelection.selectInMenu(player.containerMenu, BOX_SLOT, 4);
            player.doCloseContainer();
            click(player, BOX_SLOT, RIGHT);
            helper.assertTrue(player.containerMenu.getCarried().is(Items.SHULKER_BOX), "after the inventory closed, a right click should pick up the box, the cursor held "
                    + player.containerMenu.getCarried());
            player.containerMenu.getSlot(BOX_SLOT).set(player.containerMenu.getCarried());
            player.containerMenu.setCarried(ItemStack.EMPTY);
            // A chest's box, selected, the chest closed and opened again
            player.openMenu(chest);
            ShulkerSelection.selectInMenu(player.containerMenu, 0, 4);
            helper.assertValueEqual(ShulkerSelection.selected(player.containerMenu.getSlot(0).getItem()), 4, "the chest's box selected");
            player.doCloseContainer();
            player.openMenu(chest);
            click(player, 0, RIGHT);
            helper.assertTrue(player.containerMenu.getCarried().is(Items.SHULKER_BOX), "after the chest closed, a right click should pick up the box, the cursor held "
                    + player.containerMenu.getCarried());
            player.doCloseContainer();
        });
    }

    @GameTest
    public void aBoxSwappedOntoTheCursorIsUnselected(GameTestHelper helper) {
        withPlayer(helper, player -> {
            AbstractContainerMenu menu = player.containerMenu;
            menu.getSlot(BOX_SLOT).set(box(4, Items.DIAMOND, 3));
            ShulkerSelection.selectInMenu(menu, BOX_SLOT, 4);
            menu.setCarried(new ItemStack(Items.DIRT, 5));
            // A right click with something on the cursor: the two swap, as ever
            click(player, BOX_SLOT, RIGHT);
            helper.assertTrue(menu.getCarried().is(Items.SHULKER_BOX), "the box should be swapped onto the cursor, was " + menu.getCarried());
            helper.assertValueEqual(ShulkerSelection.selected(menu.getCarried()), -1, "the swapped box's selection");
        });
    }

    @GameTest
    public void aPeekShowsNothingPastTheWorldBorder(GameTestHelper helper) {
        BlockPos box = placeBox(helper, BOX, 2, Items.DIAMOND, 3);
        withPlayer(helper, player -> {
            standBy(player, box, 2.0);
            WorldBorder border = helper.getLevel().getWorldBorder();
            double centerX = border.getCenterX(), centerZ = border.getCenterZ(), size = border.getSize();
            try {
                // The border a long way off: the box outside it (set and restored within the tick, so no other test sees it)
                border.setCenter(box.getX() + 1000, box.getZ() + 1000);
                border.setSize(16);
                helper.assertTrue(ShulkerPeeking.contents(player, box).isEmpty(), "a box past the world border should show nothing");
                helper.assertValueEqual(ShulkerPeeking.take(player, box, 2, new ItemStack(Items.DIAMOND)), 0, "taken past the world border");
            } finally {
                border.setCenter(centerX, centerZ);
                border.setSize(size);
            }
        });
    }

    // The empty hand, and the box's colour

    @GameTest
    public void aPeekNeedsAnEmptyHand(GameTestHelper helper) {
        BlockPos box = placeBox(helper, BOX, 2, Items.DIAMOND, 3);
        withPlayer(helper, player -> {
            standBy(player, box, 2.0);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 5));
            helper.assertTrue(ShulkerPeeking.contents(player, box).isEmpty(), "a peek with something in the hand should show nothing");
            helper.assertValueEqual(ShulkerPeeking.take(player, box, 2, new ItemStack(Items.DIAMOND)), 0, "taken with something in the hand");
            assertHolds(helper, boxAt(helper, box).getItem(2), Items.DIAMOND, 3, "the box's diamonds");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            helper.assertTrue(ShulkerPeeking.contents(player, box).isPresent(), "an empty hand should see the box");
            // Only the main hand is the one that uses the box: something in the other hand is no matter
            player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
            helper.assertTrue(ShulkerPeeking.contents(player, box).isPresent(), "something in the off hand should still let the main hand see the box");
        });
    }

    @GameTest
    public void aBoxsTooltipCarriesItsColour(GameTestHelper helper) {
        ItemStack red = new ItemStack(Items.DYED_SHULKER_BOX.pick(DyeColor.RED));
        red.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIRT, 2))));
        ItemStack plain = box(0, Items.DIRT, 2);
        ShulkerTooltip redTooltip = (ShulkerTooltip) red.getTooltipImage().orElseThrow(), plainTooltip = (ShulkerTooltip) plain.getTooltipImage().orElseThrow();
        helper.assertValueEqual(redTooltip.color(), ShulkerColors.of(DyeColor.RED), "the red box's colour");
        helper.assertValueEqual(plainTooltip.color(), ShulkerColors.of(null), "the undyed box's colour");
        helper.succeed();
    }

    // The peek, refined: the held slot kept free, the whole stack seen

    @GameTest
    public void aTakeKeepsTheHeldSlotFree(GameTestHelper helper) {
        BlockPos box = placeBox(helper, BOX, 2, Items.DIAMOND, 3, 5, Items.EMERALD, 7);
        withPlayer(helper, player -> {
            standBy(player, box, 2.0);
            // The hand holds slot 0, the inventory empty: the game's own order would put the diamonds in the hand
            player.getInventory().setSelectedSlot(0);
            helper.assertValueEqual(ShulkerPeeking.take(player, box, 2, new ItemStack(Items.DIAMOND)), 3, "diamonds taken");
            helper.assertTrue(player.getMainHandItem().isEmpty(), "the hand should stay empty, held " + player.getMainHandItem());
            assertHolds(helper, player.getInventory().getItem(1), Items.DIAMOND, 3, "the first free slot past the held one");
            // Onto a stack of the same item first, wherever it is
            player.getInventory().setSelectedSlot(4);
            player.getInventory().setItem(20, new ItemStack(Items.EMERALD, 10));
            helper.assertValueEqual(ShulkerPeeking.take(player, box, 5, new ItemStack(Items.EMERALD)), 7, "emeralds taken");
            assertHolds(helper, player.getInventory().getItem(20), Items.EMERALD, 17, "the emeralds' own stack");
            helper.assertTrue(player.getMainHandItem().isEmpty(), "the hand should stay empty");
        });
    }

    @GameTest
    public void takingAnotherOfTheSameKindTakesNothing(GameTestHelper helper) {
        BlockPos box = placeBox(helper, BOX);
        ItemStack named = new ItemStack(Items.DIAMOND, 3);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("The heirloom"));
        withPlayer(helper, player -> {
            standBy(player, box, 2.0);
            boxAt(helper, box).setItem(2, named.copy());
            // The player saw plain diamonds there; the heirloom came since
            helper.assertValueEqual(ShulkerPeeking.take(player, box, 2, new ItemStack(Items.DIAMOND)), 0, "taken when the stack differs from the one seen");
            helper.assertValueEqual(ShulkerPeeking.take(player, box, 2, named.copyWithCount(1)), 3, "taken when it is the one seen (whatever its count)");
        });
    }
}
