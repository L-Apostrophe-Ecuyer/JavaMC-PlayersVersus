package frootloops.versus.mixin.mobs.hostile;

import frootloops.versus.mod.mobs.melee.MeleeHolder;
import frootloops.versus.mod.mobs.melee.MeleeState;
import frootloops.versus.mod.mobs.melee.MobMelee;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every mob carries its melee swing ({@link MeleeState}); brain mobs count theirs down after their AI each tick. */
@Mixin(Mob.class)
public abstract class MobMeleeMixin implements MeleeHolder {

    @Unique
    @Nullable
    private MeleeState playersVersus$melee;

    @Override
    public MeleeState playersVersus$melee() {
        if (this.playersVersus$melee == null) this.playersVersus$melee = new MeleeState();
        return this.playersVersus$melee;
    }

    @Override
    @Nullable
    public MeleeState playersVersus$meleeIfAny() {
        return this.playersVersus$melee;
    }

    @Inject(method = "serverAiStep", at = @At("TAIL"))
    private void playersVersus$tickBrainSwing(CallbackInfo info) {
        MobMelee.tick((Mob) (Object) this);
    }
}
