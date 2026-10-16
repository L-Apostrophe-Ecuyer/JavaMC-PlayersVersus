package frootloops.versus.mixin.environment.worldgen;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import frootloops.versus.mod.environment.worldgen.CustomWorldgen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Makes "Improved" the default preset for demo worlds and the default overworld. Chains with other mods' changes. */
@Mixin(WorldPresets.class)
public abstract class WorldPresetsMixin {
    @ModifyExpressionValue(
            method = {"createNormalWorldDimensions", "getNormalOverworld"},
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETSTATIC,
                    target = "Lnet/minecraft/world/level/levelgen/presets/WorldPresets;NORMAL:Lnet/minecraft/resources/ResourceKey;"
            )
    )
    private static ResourceKey<WorldPreset> playersVersus$useImprovedByDefault(ResourceKey<WorldPreset> original) {
        return CustomWorldgen.BETTER_WORLDGEN_PRESET;
    }
}
