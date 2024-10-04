package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;


public class CustomDamageSources {

    private static final RegistryKey<DamageType> MUD_SUFFOCATION_DAMAGE_TYPE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(VersusMod.MOD_ID, "mud_suffocation"));
    //private static DamageSource MUD_SUFFOCATION = null;

    public static DamageSource getMudSuffocation(World world) {
        return new DamageSource(world.getDamageSources().registry.entryOf(MUD_SUFFOCATION_DAMAGE_TYPE));
    }

}
