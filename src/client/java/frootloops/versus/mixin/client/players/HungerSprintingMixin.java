package frootloops.versus.mixin.client.players;

import com.mojang.authlib.GameProfile;
import frootloops.versus.VersusSettings;
import frootloops.versus.mod.Combat;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ClientPlayerEntity.class)
public abstract class HungerSprintingMixin extends PlayerEntity {

    public HungerSprintingMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @ModifyConstant(method = "canSprint", constant = @Constant(floatValue = 6.0f))
    private float foodRequiedToSprint(float foodLevel) {
        if(!VersusSettings.DO_FOOD_OVERHAUL) return 6.0f;
        return Combat.canPlayerSprint(this.hungerManager, this.hasStatusEffect(StatusEffects.HUNGER)) ? -1.0f : 128.0f;
    }
}
