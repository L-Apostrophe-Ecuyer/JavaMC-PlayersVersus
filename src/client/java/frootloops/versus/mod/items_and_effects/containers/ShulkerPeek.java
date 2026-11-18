package frootloops.versus.mod.items_and_effects.containers;

import com.mojang.blaze3d.platform.InputConstants;
import frootloops.versus.VersusMod;
import frootloops.versus.VersusSettings;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * The peek at a placed shulker box: looking at one with an empty main hand shows a small grid of its slots below the
 * crosshair, in the box's colour (the server's answer, asked for when the look starts and every few ticks after).
 * Nothing is selected until the wheel selects (PeekCursor: from item to item, and past them to nothing); using the box
 * with an item selected takes that stack instead of opening the box, and needs a fresh press of the use key.
 *
 * <p>Where the player's intent and the input could part: the wheel is the hotbar's while a scroll through the hotbar
 * goes on (PeekRules.takesWheel), sneaking leaves the wheel and the use vanilla's, a slip of the crosshair off the box
 * for a few ticks keeps the peek (drawn, its selection kept, the input the game's), and a take keeps the held slot free
 * (the server's ShulkerPeeking) so that the peek stays. A key turns the peek on and off, kept between games.
 */
public final class ShulkerPeek implements HudElement {

    private static final Logger LOGGER = LoggerFactory.getLogger("players-versus");
    private static final ShulkerPeek PEEK = new ShulkerPeek();
    private static final String OPTION = "shulkerPeek";
    private static final int NAME_GAP = 3, BELOW_CROSSHAIR = 12, CLEAR_OF_CROSSHAIR = 8, LINE = 9;
    // The action bar's text sits from 72 pixels above the bottom of the screen
    private static final int ACTION_BAR = 72;
    private static final int HINT = 0xFFA0A0A0, NAME = 0xFFFFFFFF;

    private KeyMapping toggle;
    private boolean enabled = true;
    private @Nullable BlockPos target;
    // Whether the crosshair is on the target now (not during a slip), and for how long it has slipped off
    private boolean looking;
    private int slipped;
    private @Nullable List<ItemStack> slots;
    // The selected stack as it was when selected: a slot holding something else since unselects
    private @Nullable ItemStack seen;
    private final PeekCursor cursor = new PeekCursor();
    private int askIn, ticks, lastHotbarWheel = -VersusSettings.Shulkers.PEEK_WHEEL_REST_TICKS;
    // The use key still held from a take, or from before the selection: no take, and no opening, until it is let go
    private boolean useHeld;

    private ShulkerPeek() {
    }

    public static void register() {
        PEEK.enabled = loadEnabled();
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "controls"));
        PEEK.toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping("key." + VersusMod.MOD_ID + ".shulker_peek", InputConstants.Type.KEYBOARD,
                InputConstants.UNKNOWN.getValue(), category));
        ClientTickEvents.END_CLIENT_TICK.register(PEEK::tick);
        ClientPlayNetworking.registerGlobalReceiver(ShulkerPeekPayload.TYPE, (payload, context) -> PEEK.received(payload));
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "shulker_peek"), PEEK);
    }

    /** A turn of the wheel: the peek's (choosing in the box) or, returning false, the hotbar's. */
    public static boolean scroll(int wheel) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!PeekRules.takesWheel(PEEK.takesInput(minecraft), PEEK.ticks - PEEK.lastHotbarWheel)) {
            PEEK.lastHotbarWheel = PEEK.ticks;
            return false;
        }
        List<ItemStack> slots = PEEK.slots;
        PEEK.cursor.scroll(slots.size(), slot -> !slots.get(slot).isEmpty(), wheel);
        int selected = PEEK.cursor.selected();
        PEEK.seen = selected >= 0 ? slots.get(selected).copy() : null;
        // A use key already down doesn't take what was just chosen: a fresh press does
        if (minecraft.options.keyUse.isDown()) PEEK.useHeld = true;
        return true;
    }

    /** Using the box looked at: with an item selected, takes it (and nothing else); otherwise null, vanilla's use. */
    public static @Nullable InteractionResult use(BlockPos pos) {
        if (!pos.equals(PEEK.target)) return null;
        if (PEEK.useHeld) return InteractionResult.FAIL;
        int selected = PEEK.cursor.selected();
        if (selected < 0 || !PEEK.takesInput(Minecraft.getInstance())) return null;
        ItemStack chosen = PEEK.slots.get(selected);
        if (chosen.isEmpty()) {
            PEEK.cursor.taken();
            return null;
        }
        ClientPlayNetworking.send(new TakeFromShulkerPayload(pos, selected, chosen.copy()));
        PEEK.cursor.taken();
        PEEK.seen = null;
        PEEK.useHeld = true;
        return InteractionResult.SUCCESS;
    }

    /** Whether the peek may take the wheel and the use now (the crosshair on a box with something in it, and the rest). */
    private boolean takesInput(Minecraft minecraft) {
        boolean shows = this.looking && this.hasItems() && minecraft.player != null;
        return PeekRules.takesInput(this.enabled, shows, minecraft.gui.screen() != null, shows && minecraft.player.isShiftKeyDown(),
                minecraft.gui.hud.isHidden(), shows && minecraft.player.getMainHandItem().isEmpty());
    }

    private boolean hasItems() {
        return this.target != null && this.slots != null && this.slots.stream().anyMatch(stack -> !stack.isEmpty());
    }

    private void tick(Minecraft minecraft) {
        this.ticks++;
        while (this.toggle.consumeClick()) {
            this.enabled = !this.enabled;
            saveEnabled(this.enabled);
            if (minecraft.player != null) {
                minecraft.player.sendOverlayMessage(Component.translatable(VersusMod.MOD_ID + ".shulker_peek." + (this.enabled ? "on" : "off")));
            }
        }
        if (!minecraft.options.keyUse.isDown()) this.useHeld = false;
        BlockPos looked = this.canPeek(minecraft) ? this.looked(minecraft) : null;
        if (looked != null) {
            if (!looked.equals(this.target)) this.start(looked);
            this.looking = true;
            this.slipped = 0;
        } else if (this.target != null) {
            this.looking = false;
            // Given up (the hand filled, the peek turned off, the box gone): at once; a slip of the crosshair: after a moment
            boolean gone = minecraft.level == null || !(minecraft.level.getBlockState(this.target).getBlock() instanceof ShulkerBoxBlock);
            if (!this.canPeek(minecraft) || gone || ++this.slipped > VersusSettings.Shulkers.PEEK_SLIP_TICKS) this.clear();
        }
        if (this.target != null && this.looking && --this.askIn <= 0 && ClientPlayNetworking.canSend(PeekShulkerPayload.TYPE)) {
            ClientPlayNetworking.send(new PeekShulkerPayload(this.target));
            this.askIn = VersusSettings.Shulkers.PEEK_REFRESH_TICKS;
        }
    }

    /** The peek on, and a player who could peek: not a spectator, nothing in the main hand. */
    private boolean canPeek(Minecraft minecraft) {
        return this.enabled && minecraft.player != null && minecraft.level != null && !minecraft.player.isSpectator()
                && minecraft.player.getMainHandItem().isEmpty();
    }

    private @Nullable BlockPos looked(Minecraft minecraft) {
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        return minecraft.level.getBlockState(hit.getBlockPos()).getBlock() instanceof ShulkerBoxBlock ? hit.getBlockPos().immutable() : null;
    }

    private void start(BlockPos pos) {
        this.clear();
        this.target = pos;
        this.askIn = 0;
    }

    private void clear() {
        this.target = null;
        this.looking = false;
        this.slipped = 0;
        this.slots = null;
        this.seen = null;
        this.cursor.reset();
    }

    private void received(ShulkerPeekPayload payload) {
        if (!payload.pos().equals(this.target)) return;
        List<ItemStack> slots = ShulkerItems.slots(payload.contents());
        this.slots = slots;
        // The selected stack gone, or something else there since: nothing selected, rather than whatever came to be there
        ItemStack seen = this.seen;
        this.cursor.refresh(slot -> !slots.get(slot).isEmpty(), slot -> seen == null || ItemStack.isSameItemSameComponents(slots.get(slot), seen));
        if (this.cursor.selected() < 0) this.seen = null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!this.hasItems() || !this.canPeek(minecraft) || minecraft.gui.screen() != null || minecraft.gui.hud.isHidden()) return;
        List<ItemStack> slots = this.slots;
        // Sneaking gives the input back to the game: no highlight, so that what a use will do (open the box) shows
        int selected = minecraft.player.isShiftKeyDown() ? -1 : this.cursor.selected();
        DyeColor dye = minecraft.level.getBlockState(this.target).getBlock() instanceof ShulkerBoxBlock box ? box.getColor() : null;
        int middle = graphics.guiHeight() / 2, height = ShulkerGrid.HEIGHT + NAME_GAP + LINE;
        // Below the crosshair; higher on a small screen, to stay above the action bar, but never onto the crosshair
        int top = Math.max(middle + CLEAR_OF_CROSSHAIR, Math.min(middle + BELOW_CROSSHAIR, graphics.guiHeight() - ACTION_BAR - height));
        int left = graphics.guiWidth() / 2 - ShulkerGrid.WIDTH / 2, line = top + ShulkerGrid.HEIGHT + NAME_GAP;
        ShulkerGrid.draw(graphics, minecraft.font, left, top, slots, selected, ShulkerColors.of(dye));
        if (selected >= 0) {
            ItemStack stack = slots.get(selected);
            Component name = stack.getCount() > 1 ? Component.empty().append(stack.getStyledHoverName()).append(" \u00d7" + stack.getCount()) : stack.getStyledHoverName();
            graphics.centeredText(minecraft.font, name, graphics.guiWidth() / 2, line, NAME);
        } else if (PeekRules.takesWheel(this.takesInput(minecraft), this.ticks - this.lastHotbarWheel)) {
            graphics.centeredText(minecraft.font, Component.translatable(VersusMod.MOD_ID + ".shulker_peek.choose"), graphics.guiWidth() / 2, line, HINT);
        }
    }

    private static Path optionFile() {
        return FabricLoader.getInstance().getConfigDir().resolve(VersusMod.MOD_ID + "-client.properties");
    }

    private static boolean loadEnabled() {
        Path file = optionFile();
        if (!Files.exists(file)) return true;
        Properties options = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            options.load(reader);
        } catch (IOException exception) {
            LOGGER.warn("Couldn't read {}", file, exception);
        }
        return !"false".equals(options.getProperty(OPTION));
    }

    private static void saveEnabled(boolean enabled) {
        Path file = optionFile();
        Properties options = new Properties();
        try {
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file)) {
                    options.load(reader);
                }
            }
            options.setProperty(OPTION, Boolean.toString(enabled));
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                options.store(writer, "Players Versus, the client's options");
            }
        } catch (IOException exception) {
            LOGGER.warn("Couldn't save {}", file, exception);
        }
    }
}
