package frootloops.versus.mixin.client.players;

import frootloops.versus.VersusMod;
import frootloops.versus.mod.Combat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(value = MinecraftClient.class, priority = 999)
public abstract class MinecraftClientMixin extends ReentrantThreadExecutor<Runnable> implements WindowEventHandler {
    @Shadow public ClientPlayerEntity player;
    @Shadow public ClientWorld world;
    @Shadow protected int attackCooldown;
    @Shadow private int itemUseCooldown;
    @Shadow @Nullable public HitResult crosshairTarget;
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;
    @Shadow @Nullable public final GameRenderer gameRenderer;
    @Shadow public final GameOptions options;

    public MinecraftClientMixin(String string, @Nullable GameRenderer gameRenderer, GameOptions options) { super(string);
        this.gameRenderer = gameRenderer;
        this.options = options;
    }


    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void doItemUse(CallbackInfo info) {
        this.itemUseCooldown = 4;
        if (!this.interactionManager.isBreakingBlock() && !this.player.isRiding()) {

            if (this.crosshairTarget == null) {
                VersusMod.MOD_LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");
            }

            ActionResult actionResult = null;
            Hand[] hands = this.shouldPrioritizeOffhand() ? new Hand[] {Hand.OFF_HAND, Hand.MAIN_HAND} :  new Hand[] {Hand.MAIN_HAND, Hand.OFF_HAND};
            for (Hand hand : hands) {
                ItemStack itemStack = this.player.getStackInHand(hand);
                if (!itemStack.isItemEnabled(this.world.getEnabledFeatures())) continue;
                if (this.crosshairTarget != null) {
                    switch (this.crosshairTarget.getType()) {
                        case ENTITY:
                            EntityHitResult entityHitResult = (EntityHitResult)this.crosshairTarget;
                            Entity entity = entityHitResult.getEntity();
                            if (!this.world.getWorldBorder().contains(entity.getBlockPos())) {
                                info.cancel();
                                return;
                            }

                            actionResult = this.interactionManager.interactEntityAtLocation(this.player, entity, entityHitResult, hand);
                            if (!actionResult.isAccepted()) actionResult = this.interactionManager.interactEntity(this.player, entity, hand);
                            if (actionResult instanceof ActionResult.Success success) {
                                if (success.swingSource() == ActionResult.SwingSource.CLIENT) {
                                    this.player.swingHand(hand);
                                }
                                info.cancel();
                                return;
                            }
                            break;
                        case BLOCK:
                            BlockHitResult blockHitResult = (BlockHitResult)this.crosshairTarget;
                            int i = itemStack.getCount();
                            actionResult = this.interactionManager.interactBlock(this.player, hand, blockHitResult);
                            if (actionResult instanceof ActionResult.Success success) {
                                if (success.swingSource() == ActionResult.SwingSource.CLIENT) {
                                    this.player.swingHand(hand);
                                    if (!itemStack.isEmpty() && (itemStack.getCount() != i || this.player.isInCreativeMode())) {
                                        this.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                                    }
                                }
                                info.cancel();
                                return;
                            }
                            if (actionResult instanceof ActionResult.Fail) {
                                info.cancel();
                                return;
                            }
                    }
                }

                if (!itemStack.isEmpty() && this.interactionManager.interactItem(this.player, hand) instanceof ActionResult.Success success3) {
                    if (success3.swingSource() == ActionResult.SwingSource.CLIENT) {
                        this.player.swingHand(hand);
                    }

                    this.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                    info.cancel();
                    return;
                }

            }
        }
        info.cancel();
    }

    private boolean shouldPrioritizeOffhand(){
        ItemStack offhandStack = player.getOffHandStack();
        ItemStack mainhandStack = player.getMainHandStack();
        if(offhandStack.isEmpty() || mainhandStack.isEmpty() || player.getItemCooldownManager().isCoolingDown(offhandStack)) return false;
        if(offhandStack.getUseAction() == UseAction.BLOCK){
            if (crosshairTarget.getType() == HitResult.Type.ENTITY) {
                if(player.isUsingItem()) {
                    return player.getActiveItem() == offhandStack;
                } else {
                    Entity target = ((EntityHitResult) this.crosshairTarget).getEntity();
                    return (target instanceof HostileEntity || target instanceof PlayerEntity || target == player.getAttacker());
                }
            }
            else if (mainhandStack.getUseAction() == UseAction.BLOCK) {
                return true;
            }
            else if (player.getAttacker() != null && player.getAttacker().isAlive()) {
                return (Combat.isLookingTowards(player,player.getAttacker().getEntityPos()));
            }
        }
        else if(mainhandStack.getUseAction() == UseAction.BLOCK){
            if(player.isSneaking()) {
                return !(mainhandStack.getItem() instanceof ShieldItem);
            }
            else if (crosshairTarget.getType() == HitResult.Type.ENTITY) {
                if(player.isUsingItem()) {
                    return player.getActiveItem() == offhandStack;
                } else {
                    Entity target = ((EntityHitResult) this.crosshairTarget).getEntity();
                    return !(target instanceof HostileEntity || target instanceof PlayerEntity || target == player.getAttacker());
                }
            }
            else if (player.getAttacker() != null && player.getAttacker().isAlive()) {
                return (Combat.isLookingTowards(player,player.getAttacker().getEntityPos()));
            }
            else if(offhandStack.getUseAction() == UseAction.EAT || offhandStack.getUseAction() == UseAction.DRINK) {
                if(offhandStack.getComponents().contains(DataComponentTypes.FOOD)) {
                    if(offhandStack.getItem().getComponents().get(DataComponentTypes.FOOD).canAlwaysEat()) return true;
                    return player.getHungerManager().isNotFull();
                }
                else return true;
            }
        }
        return false;
    }
}
