package frootloops.versus.mod.mobs.hostile.end;

import frootloops.versus.mod.environment.CustomSpecialEffects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * A watchling's static heard: hiss, crackle and a low drone (sounds/watchling/static.ogg, tools/watchling-static-sound.py)
 * looping in the ears of whoever looks at one, louder and higher as the static thickens (WatchlingStatic), dying away
 * with it.
 */
@Environment(EnvType.CLIENT)
public final class WatchlingStaticSound extends AbstractTickableSoundInstance {
    /** Below this much static there's nothing to hear. */
    private static final float SILENT = 0.01F;

    private static WatchlingStaticSound playing;

    private WatchlingStaticSound() {
        super(CustomSpecialEffects.WATCHLING_STATIC, SoundSource.HOSTILE, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = this.y = this.z = 0.0;
        this.follow(WatchlingStatic.shown());
    }

    /** Starts the sound when static begins (each client tick). */
    public static void tickClient(Minecraft minecraft) {
        if (WatchlingStatic.shown() <= SILENT || minecraft.level == null) return;
        if (playing != null && !playing.isStopped() && minecraft.getSoundManager().isActive(playing)) return;
        playing = new WatchlingStaticSound();
        minecraft.getSoundManager().play(playing);
    }

    @Override
    public void tick() {
        float stare = WatchlingStatic.shown();
        if (stare <= SILENT || Minecraft.getInstance().level == null) {
            this.stop();
            return;
        }
        this.follow(stare);
    }

    /** Louder and higher the thicker the static: from a faint low hiss to a loud shrill one. */
    private void follow(float stare) {
        this.volume = 0.1F + 0.9F * (float) Math.pow(stare, 0.8);
        this.pitch = 0.75F + 0.55F * stare;
    }
}
