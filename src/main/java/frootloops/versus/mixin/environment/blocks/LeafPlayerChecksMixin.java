package frootloops.versus.mixin.environment.blocks;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import frootloops.versus.mod.environment.blocks.leaves.LeafDebug;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Logs (LeafDebug) the server's answers to a player's moves that leaves can upset: pulling the player back from where
 * their client said they went, and finding nothing holding up a player who says they're on the ground. Changes nothing.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class LeafPlayerChecksMixin {

    @Shadow public ServerPlayer player;

    @WrapOperation(method = "handlePlayerPositionChange", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V"))
    private void playersVersus$logPullBack(ServerGamePacketListenerImpl self, double x, double y, double z, float yRot, float xRot, Operation<Void> original,
                                          @Local(argsOnly = true, ordinal = 0) double claimedX, @Local(argsOnly = true, ordinal = 1) double claimedY,
                                          @Local(argsOnly = true, ordinal = 2) double claimedZ) {
        if (LeafDebug.on(this.player)) LeafDebug.pulledBack(this.player, new Vec3(claimedX, claimedY, claimedZ), new Vec3(x, y, z));
        original.call(self, x, y, z, yRot, xRot);
    }

    @Inject(method = "forceSendPlayerSupportBlocks", at = @At("HEAD"))
    private void playersVersus$logStandingOnAir(CallbackInfo ci) {
        if (LeafDebug.on(this.player)) LeafDebug.standingOnAir(this.player);
    }
}
