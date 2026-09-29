package frootloops.versus.mixin.client.players;

import com.mojang.authlib.GameProfile;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.Combat;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LocalPlayer.class)
public abstract class HungerSprintingMixin extends Player {
    public HungerSprintingMixin(Level world, GameProfile profile) {
        super(world, profile);
    }

    @ModifyConstant(method = "hasEnoughFoodToSprint()Z", constant = @Constant(floatValue = 6.0f))
    private float foodRequiedToSprint(float foodLevel) {
        if(!VersusSettings.Combat.DO_FOOD_OVERHAUL) return 6.0f;
        return Combat.canPlayerSprint(this.foodData, this.hasEffect(MobEffects.HUNGER)) ? -1.0f : 128.0f;
    }
}
