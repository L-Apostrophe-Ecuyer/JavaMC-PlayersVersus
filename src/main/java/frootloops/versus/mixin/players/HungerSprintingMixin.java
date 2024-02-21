package frootloops.versus.mixin.players;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.ClientPlayerEntity;
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
    private float immediateFeedback(float tickDelayAfterUpdate) {
        if(!this.isSwimming() && this.age - this.getLastAttackedTime() < 32) return 128.0f; // No sprinting when damaged
        else return -128.0f; // Otherwise, can always sprint
    }
}
