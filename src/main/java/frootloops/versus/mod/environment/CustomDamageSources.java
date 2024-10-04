package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.blocks.*;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;


public class CustomDamageSources {

    private static final RegistryKey<DamageType> MUD_SUFFOCATION_DAMAGE_TYPE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(VersusMod.MOD_ID, "mud_suffocation"));
    //private static DamageSource MUD_SUFFOCATION = null;

    public static DamageSource getMudSuffocation(World world) {
        return new DamageSource(world.getDamageSources().registry.entryOf(MUD_SUFFOCATION_DAMAGE_TYPE));
    }

}
