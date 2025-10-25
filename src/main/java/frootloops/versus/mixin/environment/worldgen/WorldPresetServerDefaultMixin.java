package frootloops.versus.mixin.environment.worldgen;

import frootloops.versus.mod.environment.worldgen.CustomWorldgen;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.dedicated.ServerPropertiesHandler;
import net.minecraft.world.gen.WorldPreset;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerPropertiesHandler.class)
public abstract class WorldPresetServerDefaultMixin {
    @Redirect(method = "<init>", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC, target = "Lnet/minecraft/world/gen/WorldPresets;DEFAULT:Lnet/minecraft/registry/RegistryKey;"))
    private RegistryKey<WorldPreset> modifyDefaultWorldgen() {
        return CustomWorldgen.BETTER_WORLDGEN_PRESET;
    }
}