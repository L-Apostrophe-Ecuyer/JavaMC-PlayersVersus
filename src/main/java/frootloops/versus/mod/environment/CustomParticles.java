package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;


public class CustomParticles {

    public static final SimpleParticleType SPARKS = FabricParticleTypes.simple();

    public static void onInitialize() {
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of(VersusMod.MOD_ID, "sparks"), SPARKS);
    }

}
