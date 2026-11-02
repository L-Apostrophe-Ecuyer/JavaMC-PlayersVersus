package frootloops.versus.mixin.mobs.passive;

import frootloops.versus.mod.mobs.hostile.overworld.PaleWolfEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wolves hunt pale wolves as they hunt skeletons (at the same priority), and pale wolves fear them as skeletons do
 * (PaleWolfEntity). Only living wolves: zombie and pale wolves (Wolf too) don't.
 */
@Mixin(Wolf.class)
public abstract class WolfHuntsPaleWolvesMixin extends TamableAnimal {

    protected WolfHuntsPaleWolvesMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void playersVersus$huntPaleWolves(CallbackInfo info) {
        if (this.getType() == EntityTypes.WOLF) {
            this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, PaleWolfEntity.class, false));
        }
    }
}
