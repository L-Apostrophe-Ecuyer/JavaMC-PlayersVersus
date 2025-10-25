package frootloops.versus.mixin.environment.worldgen;

import frootloops.versus.mod.environment.worldgen.CustomWorldgen;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.WorldPresets;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldPresets.class)
public abstract class WorldPresetsMixin {
    @Redirect(
            method = {
                    "createDemoOptions",
                    "getDefaultOverworldOptions",
                    "createDemoOptions"
            },
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETSTATIC,
                    target = "Lnet/minecraft/world/gen/WorldPresets;DEFAULT:Lnet/minecraft/registry/RegistryKey;"
            )
    )
    private static RegistryKey<WorldPreset> redirectDefaultWorldPreset() {
        return CustomWorldgen.BETTER_WORLDGEN_PRESET;
    }
}