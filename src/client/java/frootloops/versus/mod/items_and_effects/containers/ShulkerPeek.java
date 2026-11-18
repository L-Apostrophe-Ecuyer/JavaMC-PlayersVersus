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
import net.minecraft.world.item.component.ItemContainerContents;
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
 * The peek at a placed shulker box: looking at one shows a small copy of its slots below the crosshair (the server's
 * answer, asked for when the look starts and every few ticks after), in the box's colour, only with an empty main hand.
 * Nothing is selected until the wheel selects (the hotbar's wheel is taken while the peek shows); using the box with an item selected takes that stack instead of opening
 * the box. Sneaking leaves the wheel and the use vanilla's. A key turns the peek on and off, kept between games.
 */
public final class ShulkerPeek implements HudElement {

    private static final Logger LOGGER = LoggerFactory.getLogger("players-versus");
    private static final ShulkerPeek PEEK = new ShulkerPeek();
    private static final String OPTION = "shulkerPeek";
    private static final int NAME_GAP = 3, BELOW_CROSSHAIR = 12;

    private KeyMapping toggle;
    private boolean enabled = true;
    private @Nullable BlockPos target;
    private @Nullable ItemContainerContents shown;
    private int selected = -1, askIn;
    // The use key still held from a take: the box isn't opened by its repeat
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

    /** The wheel while the peek shows: selects among the box's items instead of the hotbar's slots. */
    public static boolean scroll(int wheel) {
        if (!PEEK.active()) return false;
        PEEK.selected = ShulkerSelection.next(ShulkerItems.slots(PEEK.shown), PEEK.selected, wheel);
        return true;
    }

    /** Using the box looked at: with an item selected, takes it (and nothing else); otherwise null, vanilla's use. */
    public static @Nullable InteractionResult use(BlockPos pos) {
        if (!pos.equals(PEEK.target)) return null;
        if (PEEK.useHeld) return InteractionResult.FAIL;
        if (!PEEK.active() || PEEK.selected < 0) return null;
        ItemStack chosen = ShulkerItems.slots(PEEK.shown).get(PEEK.selected);
        if (chosen.isEmpty()) {
            PEEK.selected = -1;
            return null;
        }
        ClientPlayNetworking.send(new TakeFromShulkerPayload(pos, PEEK.selected, chosen.getItem()));
        PEEK.selected = -1;
        PEEK.useHeld = true;
        return InteractionResult.SUCCESS;
    }

    private boolean active() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean shows = this.target != null && this.shown != null && this.shown.nonEmptyItems().iterator().hasNext() && minecraft.player != null;
        return PeekRules.takesInput(this.enabled, shows, minecraft.gui.screen() != null, shows && minecraft.player.isShiftKeyDown(),
                minecraft.gui.hud.isHidden(), shows && minecraft.player.getMainHandItem().isEmpty());
    }

    private void tick(Minecraft minecraft) {
        while (this.toggle.consumeClick()) {
            this.enabled = !this.enabled;
            saveEnabled(this.enabled);
            if (minecraft.player != null) {
                minecraft.player.sendOverlayMessage(Component.translatable(VersusMod.MOD_ID + ".shulker_peek." + (this.enabled ? "on" : "off")));
            }
        }
        if (!minecraft.options.keyUse.isDown()) this.useHeld = false;
        BlockPos looked = this.looked(minecraft);
        if (!Objects.equals(looked, this.target)) {
            this.target = looked;
            this.shown = null;
            this.selected = -1;
            this.askIn = 0;
        }
        if (this.target != null && --this.askIn <= 0 && ClientPlayNetworking.canSend(PeekShulkerPayload.TYPE)) {
            ClientPlayNetworking.send(new PeekShulkerPayload(this.target));
            this.askIn = VersusSettings.Shulkers.PEEK_REFRESH_TICKS;
        }
    }

    private @Nullable BlockPos looked(Minecraft minecraft) {
        // Only with an empty main hand: with something in it, the box is used as ever
        if (!this.enabled || minecraft.player == null || minecraft.level == null || minecraft.player.isSpectator() || !minecraft.player.getMainHandItem().isEmpty()) return null;
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        return minecraft.level.getBlockState(hit.getBlockPos()).getBlock() instanceof ShulkerBoxBlock ? hit.getBlockPos().immutable() : null;
    }

    private void received(ShulkerPeekPayload payload) {
        if (!payload.pos().equals(this.target)) return;
        this.shown = payload.contents();
        // The selected item gone since: nothing selected, rather than whatever came to be there
        if (this.selected >= 0 && ShulkerItems.slots(this.shown).get(this.selected).isEmpty()) this.selected = -1;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!this.enabled || this.target == null || this.shown == null || minecraft.player == null || minecraft.level == null
                || minecraft.gui.screen() != null || minecraft.gui.hud.isHidden()) return;
        if (!this.shown.nonEmptyItems().iterator().hasNext()) return;
        if (!minecraft.player.getMainHandItem().isEmpty()) return;
        List<ItemStack> slots = ShulkerItems.slots(this.shown);
        int selected = this.active() ? this.selected : -1;
        DyeColor dye = minecraft.level.getBlockState(this.target).getBlock() instanceof ShulkerBoxBlock box ? box.getColor() : null;
        int left = graphics.guiWidth() / 2 - ShulkerGrid.WIDTH / 2, top = graphics.guiHeight() / 2 + BELOW_CROSSHAIR;
        ShulkerGrid.draw(graphics, minecraft.font, left, top, slots, selected, ShulkerColors.of(dye));
        if (selected >= 0) {
            graphics.centeredText(minecraft.font, slots.get(selected).getStyledHoverName(), graphics.guiWidth() / 2, top + ShulkerGrid.HEIGHT + NAME_GAP, -1);
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
