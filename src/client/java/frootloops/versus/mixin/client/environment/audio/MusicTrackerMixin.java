package frootloops.versus.mixin.client.environment.audio;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MusicTracker;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.MusicSound;
import net.minecraft.sound.MusicType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(MusicTracker.class)
public abstract class MusicTrackerMixin {

    @Shadow @Nullable private final MinecraftClient client;

    @Shadow @Nullable private SoundInstance current;
    @Shadow private int timeUntilNextSong = 100;

    @Shadow private final Random random;

    protected MusicTrackerMixin(@Nullable MinecraftClient client, @Nullable SoundInstance current, Random random) {
        this.client = client;
        this.current = current;
        this.random = random;
    }

    @Inject(method = "play", at= @At("HEAD"), cancellable = true)
    public void play(MusicSound type, CallbackInfo info) {
        if(type == MusicType.GAME) {
            if(client.player.getY() < -24.0) {
                int randomValue = random.nextBetween(0, 10);
                if(randomValue >= 7) {
                    current = PositionedSoundInstance.music(SoundEvents.MUSIC_OVERWORLD_DEEP_DARK.value());
                    this.timeUntilNextSong = Integer.MAX_VALUE;
                }
                else {
                    if(randomValue >= 5) current = PositionedSoundInstance.ambient(SoundEvents.AMBIENT_SOUL_SAND_VALLEY_ADDITIONS.value());
                    else current = PositionedSoundInstance.music(SoundEvents.AMBIENT_BASALT_DELTAS_MOOD.value());
                    this.timeUntilNextSong = 6000;
                }
                this.client.getSoundManager().play(this.current);
                info.cancel();
            }
            if(client.player.getY() < 40.0) {
                int randomValue = random.nextBetween(0, 10);
                if(randomValue >= 6) current = PositionedSoundInstance.ambient(SoundEvents.AMBIENT_CAVE.value());
                else current = PositionedSoundInstance.ambient(SoundEvents.AMBIENT_BASALT_DELTAS_MOOD.value());
                this.client.getSoundManager().play(this.current);
                this.timeUntilNextSong = 6000;
                info.cancel();
            }
            else if(client.world.isNight() || client.world.isRaining() || client.player.getHealth() < 14f) {
                if(random.nextBoolean()) {
                    this.timeUntilNextSong = 2000; // Skip entirely
                    info.cancel();
                }
                else {
                    current = PositionedSoundInstance.ambient(SoundEvents.MUSIC_OVERWORLD_DRIPSTONE_CAVES.value());
                    this.client.getSoundManager().play(this.current);
                    this.timeUntilNextSong = Integer.MAX_VALUE;
                    info.cancel();
                }
            }
        }
    }
}