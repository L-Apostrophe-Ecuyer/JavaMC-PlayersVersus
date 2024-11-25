package frootloops.versus.mixin.client.environment.audio;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.CustomSpecialEffects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MusicTracker;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.sound.MusicSound;
import net.minecraft.sound.MusicType;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    @Inject(method = "tick", at= @At("HEAD"), cancellable = true)
    public void tick(CallbackInfo info) {
        if (current != null && (current.getId().getNamespace() == VersusMod.MOD_ID)) {
            if(this.client.getSoundManager().isPlaying(this.current)) {
                info.cancel();
            }
        }
    }

    @Inject(method = "isPlayingType", at= @At("HEAD"), cancellable = true)
    public void isPlayingType(MusicSound type, CallbackInfoReturnable<Boolean> cir) {
        if(this.current == null) cir.setReturnValue(false);
        else cir.setReturnValue(((SoundEvent)type.getSound().value()).id().equals(this.current.getId()));
    }

    @Inject(method = "play", at= @At("HEAD"), cancellable = true)
    public void play(MusicSound type, CallbackInfo info) {
        if(type == MusicType.GAME) {
            if(client.player.getY() < -24.0) {
                current = PositionedSoundInstance.music(SoundEvents.MUSIC_OVERWORLD_DEEP_DARK.value());
                this.client.getSoundManager().play(this.current);
                info.cancel();
            }
            if(client.player.getY() < 40.0 || (client.player.getHealth() < 14f && (client.world.isNight() || client.world.isRaining()))) {
                int randomValue = random.nextInt( 10);
                if(randomValue > 6) current = PositionedSoundInstance.ambient(SoundEvents.AMBIENT_CAVE.value());
                else current = PositionedSoundInstance.ambient(SoundEvents.MUSIC_OVERWORLD_DRIPSTONE_CAVES.value());
                this.client.getSoundManager().play(this.current);
                this.timeUntilNextSong = 4000;
                info.cancel();
            }
        }
    }
}