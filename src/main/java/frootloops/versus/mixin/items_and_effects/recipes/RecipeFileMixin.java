package frootloops.versus.mixin.items_and_effects.recipes;

import com.google.gson.JsonElement;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import frootloops.versus.mod.items_and_effects.recipes.RecipeRules;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Each recipe file, vanilla's, a mod's or a data pack's, read through the mod's rules ({@link RecipeRules}) just before
 * it's decoded. 26.4 loads recipes as a registry, every element of which is read from its file here.
 */
@Mixin(targets = "net.minecraft.resources.RegistryLoadTask$PendingRegistration")
public abstract class RecipeFileMixin {
    @WrapOperation(method = "loadFromResource", at = @At(value = "INVOKE",
            target = "Lcom/mojang/serialization/Decoder;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"))
    private static DataResult<?> playersVersus$recipeRules(Decoder<?> decoder, DynamicOps<?> ops, Object input, Operation<DataResult<?>> original,
                                                          @Local(argsOnly = true) ResourceKey<?> key) {
        if (key.isFor(Registries.RECIPE) && input instanceof JsonElement json) input = RecipeRules.apply(key.identifier(), json);
        return original.call(decoder, ops, input);
    }
}
