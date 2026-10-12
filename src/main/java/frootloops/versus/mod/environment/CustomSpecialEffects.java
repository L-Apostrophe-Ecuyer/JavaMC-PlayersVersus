package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;


public class CustomSpecialEffects {

    public static SoundEvent RAIL_TURNING_SOUND;
    public static SoundEvent FOG_WIND_SOUND;
    public static SoundEvent DEEP_CAVES_MUSIC;
    /** Leaves pushed through: vanilla's leafy sounds, by sounds.json, under a subtitle of their own. */
    public static SoundEvent LEAVES_RUSTLE;
    public static Holder.Reference<SoundEvent> SWORD_BLOCKING_WOOD, SWORD_BLOCKING_STONE, SWORD_BLOCKING_METAL, SWORD_BLOCKING_DIAMOND;
    public static final SimpleParticleType SPARKS_PARTICLE = FabricParticleTypes.simple();
    public static final SimpleParticleType BUYANCY_EFFECT_PARTICLE = FabricParticleTypes.simple();

    public static void onInitialize() {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "sparks"), SPARKS_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.fromNamespaceAndPath(VersusMod.MOD_ID, "buyancy_effect"), BUYANCY_EFFECT_PARTICLE);
        RAIL_TURNING_SOUND = registerSoundEvent("rail_turning_sound");
        FOG_WIND_SOUND = registerSoundEvent("fog_wind_sound");
        DEEP_CAVES_MUSIC = registerSoundEvent("music.overworld.deep_caves");
        LEAVES_RUSTLE = registerSoundEvent("block.leaves.rustle");
        SWORD_BLOCKING_WOOD = registerSoundEventReference("sword_blocking_wood");
        SWORD_BLOCKING_STONE = registerSoundEventReference("sword_blocking_stone");
        SWORD_BLOCKING_METAL = registerSoundEventReference("sword_blocking_metal");
        SWORD_BLOCKING_DIAMOND = registerSoundEventReference("sword_blocking_diamond");
    }

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID,name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    private static Holder.Reference<SoundEvent> registerSoundEventReference(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(VersusMod.MOD_ID,name);
        return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }
}
