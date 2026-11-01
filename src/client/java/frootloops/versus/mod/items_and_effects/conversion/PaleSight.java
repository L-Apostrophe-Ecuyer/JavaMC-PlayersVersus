package frootloops.versus.mod.items_and_effects.conversion;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.mobs.hostile.end.WatchlingStatic;
import frootloops.versus.mod.mobs.hostile.end.WatchlingStaticSound;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * What the pale do to a player's sight, as screen effects (assets/players-versus/post_effect, from tools/post-effects.py):
 * the world greys as Conversion whitens their hearts (Conversion#greyStrength), easing in and out over a few seconds
 * through twenty shades of grey, and breaks into big drab pixels under a watchling's stare (as thick as its static,
 * WatchlingStatic, through sixteen steps); the static is heard too (WatchlingStaticSound). Asked of the game renderer
 * every tick, with whatever else it shows (a spectated creeper's view).
 */
@Environment(EnvType.CLIENT)
public final class PaleSight {
    private static final Logger LOGGER = LoggerFactory.getLogger("players-versus/pale-sight");
    /** Grey steps and watchling steps there are (post_effect conversion_1.._20, watchling_1.._16). */
    private static final int GREY_STEPS = 20, WATCHLING_STEPS = 16;
    /** How far the grey shown moves toward the grey due each tick: all the way in five seconds. */
    private static final float GREY_EASE = 0.01F;
    private static boolean broken;
    private static float grey;

    private PaleSight() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(PaleSight::tick);
    }

    private static void tick(Minecraft minecraft) {
        WatchlingStaticSound.tickClient(minecraft);
        if (broken) return;
        try {
            List<Identifier> requested = minecraft.gameRenderer.getRequestedPostEffects();
            requested.removeIf(id -> id.getNamespace().equals(VersusMod.MOD_ID));
            Player player = minecraft.player;
            if (player == null || minecraft.level == null) {
                grey = 0.0F;
                return;
            }
            MobEffectInstance conversion = player.getEffect(CustomStatusEffects.CONVERSION);
            float due = conversion == null ? 0.0F : Conversion.greyStrength(conversion.getAmplifier(), player.getMaxHealth(), conversion.getDuration());
            grey = Mth.approach(grey, due, GREY_EASE);
            int greyStep = Math.round(grey * GREY_STEPS);
            if (greyStep > 0) requested.add(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "conversion_" + greyStep));
            int stareStep = Math.round(WatchlingStatic.shown() * WATCHLING_STEPS);
            if (stareStep > 0) requested.add(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "watchling_" + Math.min(WATCHLING_STEPS, stareStep)));
        } catch (UnsupportedOperationException e) {
            // The renderer's list turned out not to be ours to change: go without
            broken = true;
            LOGGER.warn("Can't request screen effects; the world won't grey nor break up", e);
        }
    }
}
