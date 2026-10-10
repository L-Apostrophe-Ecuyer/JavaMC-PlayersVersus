package frootloops.versus.mixin.client.environment.worldgen;

import frootloops.versus.mod.environment.worldgen.CustomWorldgen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Environment(EnvType.CLIENT)
@Mixin(CreateWorldScreen.class)
public class DefaultWorldPresetMixin {

    @ModifyArg(
            method = "openFresh(Lnet/minecraft/client/Minecraft;Ljava/lang/Runnable;Lnet/minecraft/client/gui/screens/worldselection/CreateWorldCallback;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;openCreateWorldScreen(Lnet/minecraft/client/Minecraft;Ljava/lang/Runnable;Ljava/util/function/Function;Lnet/minecraft/client/gui/screens/worldselection/WorldCreationContextMapper;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/gui/screens/worldselection/CreateWorldCallback;)V"
            ),
            index = 4 // 5th argument (0-indexed)
    )
    private static ResourceKey<WorldPreset> modifyDefaultWorldPreset(ResourceKey<WorldPreset> original) {
        return CustomWorldgen.BETTER_WORLDGEN_PRESET;
    }
}