package frootloops.versus.mod.items_and_effects.conversion;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.items_and_effects.brewing.CustomStatusEffects;
import frootloops.versus.mod.mobs.hostile.end.WatchlingStatic;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * What the pale do to a player's sight, as screen effects (assets/players-versus/post_effect, from tools/post-effects.py):
 * the world greys as Conversion whitens their hearts (Conversion#greyStep), and breaks into big dull pixels under a
 * watchling's stare (as thick as its static, WatchlingStatic). Asked of the game renderer every tick, with whatever
 * else it shows (a spectated creeper's view).
 */
@Environment(EnvType.CLIENT)
public final class PaleSight {
    private static final Logger LOGGER = LoggerFactory.getLogger("players-versus/pale-sight");
    private static boolean broken;

    private PaleSight() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(PaleSight::tick);
    }

    private static void tick(Minecraft minecraft) {
        if (broken) return;
        try {
            List<Identifier> requested = minecraft.gameRenderer.getRequestedPostEffects();
            requested.removeIf(id -> id.getNamespace().equals(VersusMod.MOD_ID));
            Player player = minecraft.player;
            if (player == null || minecraft.level == null) return;
            MobEffectInstance conversion = player.getEffect(CustomStatusEffects.CONVERSION);
            int grey = conversion == null ? 0 : Conversion.greyStep(conversion.getAmplifier(), player.getMaxHealth(), conversion.getDuration());
            if (grey > 0) requested.add(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "conversion_" + grey));
            float stare = WatchlingStatic.shown();
            if (stare > 0.02F) requested.add(Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "watchling_" + Math.min(4, (int) Math.ceil(stare * 4.0F))));
        } catch (UnsupportedOperationException e) {
            // The renderer's list turned out not to be ours to change: go without
            broken = true;
            LOGGER.warn("Can't request screen effects; the world won't grey nor break up", e);
        }
    }
}
