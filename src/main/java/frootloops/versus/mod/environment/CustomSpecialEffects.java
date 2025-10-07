package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.item.Items;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;


public class CustomSpecialEffects {

    public static SoundEvent RAIL_TURNING_SOUND;
    public static SoundEvent FOG_WIND_SOUND;
    public static SoundEvent DEEP_CAVES_MUSIC;
    public static RegistryEntry.Reference<SoundEvent> SWORD_BLOCKING_WOOD, SWORD_BLOCKING_STONE, SWORD_BLOCKING_METAL, SWORD_BLOCKING_DIAMOND;
    public static final SimpleParticleType SPARKS_PARTICLE = FabricParticleTypes.simple();
    public static final SimpleParticleType BUYANCY_EFFECT_PARTICLE = FabricParticleTypes.simple();

    public static void onInitialize() {
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of(VersusMod.MOD_ID, "sparks"), SPARKS_PARTICLE);
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of(VersusMod.MOD_ID, "buyancy_effect"), BUYANCY_EFFECT_PARTICLE);
        RAIL_TURNING_SOUND = registerSoundEvent("rail_turning_sound");
        FOG_WIND_SOUND = registerSoundEvent("fog_wind_sound");
        DEEP_CAVES_MUSIC = registerSoundEvent("music.overworld.deep_caves");
        SWORD_BLOCKING_WOOD = registerSoundEventReference("sword_blocking_wood");
        SWORD_BLOCKING_STONE = registerSoundEventReference("sword_blocking_stone");
        SWORD_BLOCKING_METAL = registerSoundEventReference("sword_blocking_metal");
        SWORD_BLOCKING_DIAMOND = registerSoundEventReference("sword_blocking_diamond");
    }

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.of(VersusMod.MOD_ID,name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    private static RegistryEntry.Reference<SoundEvent> registerSoundEventReference(String name) {
        Identifier id = Identifier.of(VersusMod.MOD_ID,name);
        return Registry.registerReference(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
}
