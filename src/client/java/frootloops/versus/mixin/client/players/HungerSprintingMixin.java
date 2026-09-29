package frootloops.versus.mixin.client.players;

import com.mojang.authlib.GameProfile;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.Combat;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LocalPlayer.class)
public abstract class HungerSprintingMixin extends Player {
    public HungerSprintingMixin(Level world, GameProfile profile) {
        super(world, profile);
    }

    // 26.3 checks the food for sprinting in Player#hasEnoughFoodToDoExhaustiveManoeuvres (flying, or
    // FoodData#hasEnoughFood, which holds the old 6.0); overriding it for the local player keeps this client-side,
    // as the constant in LocalPlayer#hasEnoughFoodToSprint was.
    @Override
    protected boolean hasEnoughFoodToDoExhaustiveManoeuvres() {
        if(!VersusSettings.Combat.DO_FOOD_OVERHAUL) return super.hasEnoughFoodToDoExhaustiveManoeuvres();
        return this.getAbilities().mayfly || Combat.canPlayerSprint(this.foodData, this.hasEffect(MobEffects.HUNGER));
    }
}
