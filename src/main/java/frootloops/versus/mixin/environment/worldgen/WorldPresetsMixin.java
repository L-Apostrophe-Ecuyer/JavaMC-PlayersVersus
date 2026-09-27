package frootloops.versus.mixin.environment.worldgen;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import frootloops.versus.mod.environment.worldgen.CustomWorldgen;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.WorldPresets;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Makes "Improved" the default preset for demo worlds and the default overworld. Chains with other mods' changes. */
@Mixin(WorldPresets.class)
public abstract class WorldPresetsMixin {
    @ModifyExpressionValue(
            method = {"createDemoOptions", "getDefaultOverworldOptions"},
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETSTATIC,
                    target = "Lnet/minecraft/world/gen/WorldPresets;DEFAULT:Lnet/minecraft/registry/RegistryKey;"
            )
    )
    private static RegistryKey<WorldPreset> playersVersus$useImprovedByDefault(RegistryKey<WorldPreset> original) {
        return CustomWorldgen.BETTER_WORLDGEN_PRESET;
    }
}
