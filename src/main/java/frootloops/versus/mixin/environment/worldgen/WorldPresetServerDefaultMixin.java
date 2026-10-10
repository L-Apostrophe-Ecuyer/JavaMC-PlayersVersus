package frootloops.versus.mixin.environment.worldgen;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import frootloops.versus.mod.environment.worldgen.CustomWorldgen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Makes "Improved" the dedicated server's level-type when server.properties doesn't set one. Chains with other mods' changes. */
@Mixin(DedicatedServerProperties.class)
public abstract class WorldPresetServerDefaultMixin {
    @ModifyExpressionValue(
            method = "<init>",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETSTATIC,
                    target = "Lnet/minecraft/world/level/levelgen/presets/WorldPresets;NORMAL:Lnet/minecraft/resources/ResourceKey;"
            )
    )
    private ResourceKey<WorldPreset> playersVersus$useImprovedByDefault(ResourceKey<WorldPreset> original) {
        return CustomWorldgen.BETTER_WORLDGEN_PRESET;
    }
}
