package frootloops.versus.mixin.client.items.inventory;

import frootloops.versus.mod.Combat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
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
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
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
@Mixin(value = MinecraftClient.class)
public abstract class MinecraftClientMixin extends ReentrantThreadExecutor<Runnable> implements WindowEventHandler {
    @Shadow public ClientPlayerEntity player;
    @Shadow public ClientWorld world;
    @Shadow protected int attackCooldown;
    @Shadow private int itemUseCooldown;
    @Shadow @Nullable public HitResult crosshairTarget;
    @Shadow @Nullable public ClientPlayerInteractionManager interactionManager;
    @Shadow @Nullable public final GameRenderer gameRenderer;
    @Shadow public final GameOptions options;

    @Shadow private void addBlockEntityNbt(ItemStack stack, BlockEntity blockEntity, DynamicRegistryManager registryManager) {
        NbtCompound nbtCompound = blockEntity.createComponentlessNbtWithIdentifyingData(registryManager);
        blockEntity.removeFromCopiedStackNbt(nbtCompound);
        BlockItem.setBlockEntityData(stack, blockEntity.getType(), nbtCompound);
        stack.applyComponentsFrom(blockEntity.createComponentMap());
    }

    public MinecraftClientMixin(String string, @Nullable GameRenderer gameRenderer, GameOptions options) { super(string);
        this.gameRenderer = gameRenderer;
        this.options = options;
    }

    /**
     * Shields are activated when looking at a mob or player, regardless of mainhand stack.
     * @param ci
     */
    @Inject(method = "doItemUse",at = @At("HEAD"), cancellable = true)
    private void doItemUse(CallbackInfo ci) {
        if (this.interactionManager.isBreakingBlock()) return;
        if(itemUseCooldown == 4) return;
        itemUseCooldown = 4;

        if (this.player.isRiding()) return;
        if(this.shouldPrioritizeOffhand()){
            if(!this.tryUsingItem(Hand.OFF_HAND)) this.tryUsingItem(Hand.MAIN_HAND);
            ci.cancel();
        }
    }

    private boolean shouldPrioritizeOffhand(){
        ItemStack offhandStack = player.getOffHandStack();
        ItemStack mainhandStack = player.getMainHandStack();
        if(offhandStack.isEmpty() || mainhandStack.isEmpty()) return false;
        if(offhandStack.getItem() instanceof ShieldItem){
            if(player.isSneaking()) {
                return true;
            }
            else if (crosshairTarget.getType() == HitResult.Type.ENTITY) {
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
                return (Combat.isLookingTowards(player,player.getAttacker().getPos()));
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
                return (Combat.isLookingTowards(player,player.getAttacker().getPos()));
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


    /**
     * Shields are activated when looking at a mob or player, regardless of mainhand stack.
     * @param hand
     * @return whether the item was used or not.
     */
    private boolean tryUsingItem(Hand hand) {
        ItemStack itemStack = this.player.getStackInHand(hand);
        if (!itemStack.isItemEnabled(world.getEnabledFeatures())) return false;
        ActionResult actionResult = null;

        if (this.crosshairTarget != null) {
            switch (this.crosshairTarget.getType()) {
                case ENTITY:
                    EntityHitResult entityHitResult = (EntityHitResult)this.crosshairTarget;
                    Entity entity = entityHitResult.getEntity();
                    if (!world.getWorldBorder().contains(entity.getBlockPos())) {
                        return false;
                    }

                    actionResult = this.interactionManager.interactEntityAtLocation(this.player, entity, entityHitResult, hand);
                    if (!actionResult.isAccepted()) {
                        actionResult = this.interactionManager.interactEntity(this.player, entity, hand);
                    }

                    if (actionResult.isAccepted()) {
                        if (actionResult.shouldSwingHand()) {
                            this.player.swingHand(hand);
                        }
                        return true;
                    }
                    break;
                case BLOCK:
                    BlockHitResult blockHitResult = (BlockHitResult)this.crosshairTarget;
                    int i = itemStack.getCount();

                    if(itemStack.getComponents().contains(DataComponentTypes.FOOD) && !this.player.isSneaking() && this.player.getHungerManager().isNotFull())
                        actionResult = this.interactionManager.interactItem(this.player, hand);

                    if(actionResult == null || !actionResult.isAccepted())
                        actionResult = this.interactionManager.interactBlock(this.player, hand, blockHitResult);

                    if (actionResult.isAccepted()) {
                        if (actionResult.shouldSwingHand()) {
                            this.player.swingHand(hand);
                            if (!itemStack.isEmpty() && (itemStack.getCount() != i || this.interactionManager.hasCreativeInventory())) {
                                this.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                            }
                        }
                        return true;
                    }

                    if (actionResult == ActionResult.FAIL) {
                        return false;
                    }
            }
        }

        if (!itemStack.isEmpty()) {
            actionResult = this.interactionManager.interactItem(this.player, hand);
            if (actionResult.isAccepted()) {
                if (actionResult.shouldSwingHand()) {
                    this.player.swingHand(hand);
                }

                this.gameRenderer.firstPersonRenderer.resetEquipProgress(hand);
                return true;
            }
        }

        return false;
    }

}
