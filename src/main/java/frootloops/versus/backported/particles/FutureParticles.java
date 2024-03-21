package frootloops.versus.backported.particles;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class FutureParticles {

    public static final DefaultParticleType GUST = FabricParticleTypes.simple();
    public static final DefaultParticleType GUST_EMITTER = FabricParticleTypes.simple();

    public static void onInitialize() {
        Registry.register(Registries.PARTICLE_TYPE, new Identifier("minecraft", "gust"), GUST);
        Registry.register(Registries.PARTICLE_TYPE, new Identifier("minecraft", "gust_emitter"), GUST_EMITTER);
    }
}
