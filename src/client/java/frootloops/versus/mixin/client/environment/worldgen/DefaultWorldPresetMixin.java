package frootloops.versus.mixin.client.environment.worldgen;

import frootloops.versus.mod.environment.worldgen.CustomWorldgen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.gen.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin(CreateWorldScreen.class)
public class DefaultWorldPresetMixin {

    @ModifyArg(
            method = "Lnet/minecraft/client/gui/screen/world/CreateWorldScreen;show(Lnet/minecraft/client/MinecraftClient;Ljava/lang/Runnable;Lnet/minecraft/client/gui/screen/world/CreateWorldCallback;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/world/CreateWorldScreen;show(Lnet/minecraft/client/MinecraftClient;Ljava/lang/Runnable;Ljava/util/function/Function;Lnet/minecraft/client/world/GeneratorOptionsFactory;Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/client/gui/screen/world/CreateWorldCallback;)V"
            ),
            index = 4 // 5th argument (0-indexed)
    )
    private static RegistryKey<WorldPreset> modifyDefaultWorldPreset(RegistryKey<WorldPreset> original) {
        return CustomWorldgen.BETTER_WORLDGEN_PRESET;
    }
}