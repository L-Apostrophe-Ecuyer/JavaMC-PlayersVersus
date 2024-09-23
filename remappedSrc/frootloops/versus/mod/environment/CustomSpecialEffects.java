package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;


public class CustomSpecialEffects {

    public static SoundEvent RAIL_TURNING_SOUND;

    public static final SimpleParticleType SPARKS_PARTICLE = FabricParticleTypes.simple();


    public static void onInitialize() {
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of(VersusMod.MOD_ID, "sparks"), SPARKS_PARTICLE);
        RAIL_TURNING_SOUND = registerSoundEvent("rail_turning_sound");
    }

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = Identifier.of(VersusMod.MOD_ID,name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
}
