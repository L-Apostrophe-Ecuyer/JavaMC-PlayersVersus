package frootloops.versus.mod.environment;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.environment.blocks.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import java.util.Optional;


public class CustomDamageSources {

    private static final ResourceKey<DamageType> MUD_SUFFOCATION_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "mud_suffocation"));

    public static DamageSource getMudSuffocation(Level world) {
        Holder.Reference<DamageType> entry = world.damageSources().damageTypes.get(ResourceLocation.fromNamespaceAndPath(VersusMod.MOD_ID, "mud_suffocation")).get();
        return new DamageSource(entry);
    }
}
