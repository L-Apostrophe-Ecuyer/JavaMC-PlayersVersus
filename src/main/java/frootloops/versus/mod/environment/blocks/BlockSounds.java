package frootloops.versus.mod.environment.blocks;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.sounds.BlockSoundSet;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block sounds since 26.4: a block state names its sound set by key ({@code BlockSoundSets}), and the set itself, its
 * sounds, volume and pitch, comes from a registry ({@code block_sound_set}, data).
 */
public final class BlockSounds {

    private BlockSounds() {
    }

    /** Whether the state's block sounds like this set (26.3's {@code getSoundType() == SoundType.X}). */
    public static boolean is(BlockState state, ResourceKey<BlockSoundSet> sounds) {
        return state.getSounds().filter(sounds::equals).isPresent();
    }

    /** Plays one of the state's sounds at the entity, at the set's volume and pitch scaled by these factors. */
    public static void play(Entity entity, BlockState state, Function<BlockSoundSet, Optional<Holder<SoundEvent>>> which,
                            float volumeScale, float pitchScale) {
        BlockSoundSet sounds = state.getSounds(entity.level());
        which.apply(sounds).ifPresent(sound -> entity.playSound(sound.value(), sounds.volume() * volumeScale, sounds.pitch() * pitchScale));
    }
}
