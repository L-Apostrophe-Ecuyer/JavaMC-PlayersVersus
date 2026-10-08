package frootloops.versus.mixin.environment.blocks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import frootloops.versus.mod.environment.blocks.CreakingHearts;
import net.minecraft.core.BlockPos;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CreakingHeartBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The creaking heart block's own reads of vanilla's {@code creaking_active} attribute, made true in a cave as the block
 * entity's are ({@link CreakingHeartBlockEntityMixin}): the state a heart takes when placed or when its logs come back,
 * and its idle sound.
 */
@Mixin(CreakingHeartBlock.class)
public abstract class CreakingHeartBlockMixin {

    @WrapOperation(
            method = "updateState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/core/BlockPos;)Ljava/lang/Object;"
            )
    )
    private static Object playersVersus$activeInCaves(EnvironmentAttributeSystem attributes, EnvironmentAttribute<?> attribute, BlockPos pos,
                                                      Operation<Object> original, @Local(argsOnly = true) Level level) {
        return CreakingHearts.activeInCaves(original.call(attributes, attribute, pos), attribute, level, pos);
    }

    @WrapOperation(
            method = "animateTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/attribute/EnvironmentAttributeSystem;getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/core/BlockPos;)Ljava/lang/Object;"
            )
    )
    private Object playersVersus$idleInCaves(EnvironmentAttributeSystem attributes, EnvironmentAttribute<?> attribute, BlockPos pos,
                                             Operation<Object> original, @Local(argsOnly = true) Level level) {
        return CreakingHearts.activeInCaves(original.call(attributes, attribute, pos), attribute, level, pos);
    }
}
