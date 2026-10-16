package frootloops.versus.mixin.environment.blocks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import frootloops.versus.mod.environment.blocks.CreakingHearts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CreakingHeartBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Creaking hearts wake, spawn their creaking and keep it at night, as in vanilla, or at any time in a cave
 * ({@link CreakingHearts#inCave}). Both reads of vanilla's {@code creaking_active} attribute are wrapped: the heart's
 * state in {@code updateCreakingState}, and {@code serverTick}'s, which lets the creaking go once the heart isn't active.
 * The creaking itself has no clock: it lives as long as its heart keeps it.
 */
@Mixin(CreakingHeartBlockEntity.class)
public abstract class CreakingHeartBlockEntityMixin {

    @WrapOperation(
            method = {"updateCreakingState", "serverTick"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/core/BlockPos;)Ljava/lang/Object;"
            ),
            require = 2
    )
    private static Object playersVersus$activeInCaves(EnvironmentAttributeSystem attributes, EnvironmentAttribute<?> attribute, BlockPos pos,
                                                      Operation<Object> original, @Local(argsOnly = true) Level level) {
        return CreakingHearts.activeInCaves(original.call(attributes, attribute, pos), attribute, level, pos);
    }
}
