package frootloops.versus.mixin.environment.blocks;

import com.llamalad7.mixinextras.sugar.Local;
import frootloops.versus.mod.environment.blocks.trees.TreeFelling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** A falling tree's logs never take the last half heart from a player or a pet (TreeFelling.spare). */
@Mixin(LivingEntity.class)
public abstract class FelledTreeDamageMixin {

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true)
    private float playersVersus$spareFromFallingTrees(float amount, @Local(argsOnly = true) DamageSource source) {
        return TreeFelling.spare((LivingEntity) (Object) this, source, amount);
    }
}
